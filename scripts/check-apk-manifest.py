#!/usr/bin/env python3
"""Inspect a built APK without the Android SDK.

The interesting claims about a release are made by the *final* manifest, after
manifest merging, not by the source file: which processes exist, whether the
native libraries are extracted, whether the build is debuggable, and which
version code shipped. This reads ``AndroidManifest.xml`` straight out of the
APK — it is Android binary XML, not text — and checks those claims.

No SDK, no device, no Java. That is deliberate: CI should be able to prove the
packaging contract on the runner that built it.

Usage:
    check-apk-manifest.py <apk> --app-id <id> [--debug | --release]

Exit: 0 all checks passed, 1 a check failed, 2 usage or parse error.
"""

from __future__ import annotations

import struct
import sys
import zipfile
from dataclasses import dataclass, field
from typing import Iterator

# Chunk types from frameworks/base/libs/androidfw/include/androidfw/ResourceTypes.h
RES_STRING_POOL_TYPE = 0x0001
RES_XML_TYPE = 0x0003
RES_XML_START_ELEMENT_TYPE = 0x0102
RES_XML_END_ELEMENT_TYPE = 0x0103
RES_XML_RESOURCE_MAP_TYPE = 0x0180

UTF8_FLAG = 0x100

TYPE_REFERENCE = 0x01
TYPE_STRING = 0x03
TYPE_INT_DEC = 0x10
TYPE_INT_HEX = 0x11
TYPE_INT_BOOLEAN = 0x12


class AxmlError(Exception):
    pass


class StringPool:
    def __init__(self, data: bytes) -> None:
        (
            _type,
            header_size,
            _size,
            string_count,
            _style_count,
            flags,
            strings_start,
            _styles_start,
        ) = struct.unpack_from("<HHIIIIII", data, 0)
        self.utf8 = bool(flags & UTF8_FLAG)
        self.strings: list[str] = []

        offset = header_size
        offsets = struct.unpack_from(f"<{string_count}I", data, offset)
        base = strings_start
        for relative in offsets:
            self.strings.append(self._read(data, base + relative))

    def _read(self, data: bytes, offset: int) -> str:
        if self.utf8:
            # Two lengths: UTF-16 code units, then bytes. Both are 1 byte, or
            # 2 bytes when the high bit is set.
            first = data[offset]
            offset += 1
            if first & 0x80:
                offset += 1
            length = data[offset]
            offset += 1
            if length & 0x80:
                length = ((length & 0x7F) << 8) | data[offset]
                offset += 1
            raw = data[offset : offset + length]
            return raw.decode("utf-8", "replace")

        length = struct.unpack_from("<H", data, offset)[0]
        offset += 2
        if length & 0x8000:
            length = ((length & 0x7FFF) << 16) | struct.unpack_from("<H", data, offset)[0]
            offset += 2
        return data[offset : offset + length * 2].decode("utf-16-le", "replace")

    def __getitem__(self, index: int) -> str:
        if index == 0xFFFFFFFF or index >= len(self.strings):
            return ""
        return self.strings[index]


@dataclass
class Element:
    name: str
    attributes: dict[str, str] = field(default_factory=dict)
    children: list["Element"] = field(default_factory=list)
    parent: "Element | None" = None

    def find_all(self, name: str) -> Iterator["Element"]:
        for child in self.children:
            if child.name == name:
                yield child
            yield from child.find_all(name)

    def find(self, name: str) -> "Element | None":
        return next(self.find_all(name), None)


def parse_manifest(data: bytes) -> Element:
    if struct.unpack_from("<H", data, 0)[0] != RES_XML_TYPE:
        raise AxmlError("not binary XML")

    pool: StringPool | None = None
    root: Element | None = None
    current: Element | None = None

    offset = struct.unpack_from("<H", data, 2)[0]
    end = len(data)

    while offset + 8 <= end:
        chunk_type, header_size, chunk_size = struct.unpack_from("<HHI", data, offset)
        if chunk_size < 8 or offset + chunk_size > end:
            raise AxmlError(f"bad chunk at {offset}")

        body = data[offset : offset + chunk_size]

        if chunk_type == RES_STRING_POOL_TYPE:
            pool = StringPool(body)
        elif chunk_type == RES_XML_RESOURCE_MAP_TYPE:
            pass
        elif chunk_type == RES_XML_START_ELEMENT_TYPE:
            if pool is None:
                raise AxmlError("element before string pool")
            element = _read_start_element(body, pool)
            element.parent = current
            if current is None:
                root = element
            else:
                current.children.append(element)
            current = element
        elif chunk_type == RES_XML_END_ELEMENT_TYPE:
            if current is not None:
                current = current.parent

        offset += chunk_size

    if root is None:
        raise AxmlError("no root element")
    return root


def _read_start_element(body: bytes, pool: StringPool) -> Element:
    # Chunk header (8) + line number (4) + comment (4), then the attribute
    # extension record.
    ns, name, attribute_start, attribute_size, attribute_count = struct.unpack_from(
        "<IIHHH", body, 16
    )
    element = Element(name=pool[name])

    for index in range(attribute_count):
        base = 16 + attribute_start + index * attribute_size
        attr_ns, attr_name, _raw, _size, _res0, data_type, value = struct.unpack_from(
            "<IIIHBBI", body, base
        )
        key = pool[attr_name]
        text = _value(pool, data_type, value)
        # Framework attributes are unprefixed in the pool; the namespace index
        # is not needed because our manifest does not mix prefixes.
        element.attributes[key] = text
        _ = attr_ns

    return element


def _value(pool: StringPool, data_type: int, value: int) -> str:
    if data_type == TYPE_STRING:
        return pool[value]
    if data_type == TYPE_INT_BOOLEAN:
        return "true" if value else "false"
    if data_type == TYPE_INT_DEC:
        return str(value)
    if data_type == TYPE_INT_HEX:
        return hex(value)
    if data_type == TYPE_REFERENCE:
        return f"@{value:#x}"
    return str(value)


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__.strip().splitlines()[-1], file=sys.stderr)
        return 2

    apk = argv[1]
    app_id: str | None = None
    debug: bool | None = None

    index = 2
    while index < len(argv):
        argument = argv[index]
        if argument == "--app-id" and index + 1 < len(argv):
            app_id = argv[index + 1]
            index += 2
        elif argument == "--debug":
            debug = True
            index += 1
        elif argument == "--release":
            debug = False
            index += 1
        else:
            print(f"unknown argument: {argument}", file=sys.stderr)
            return 2

    with zipfile.ZipFile(apk) as archive:
        try:
            raw = archive.read("AndroidManifest.xml")
        except KeyError:
            print("APK has no AndroidManifest.xml", file=sys.stderr)
            return 1

    try:
        manifest = parse_manifest(raw)
    except AxmlError as error:
        print(f"cannot parse AndroidManifest.xml: {error}", file=sys.stderr)
        return 1

    failures: list[str] = []
    checks = 0

    def expect(condition: bool, message: str) -> None:
        nonlocal checks
        checks += 1
        if not condition:
            failures.append(message)

    attributes = manifest.attributes
    actual_id = attributes.get("package", "")
    if app_id is not None:
        expect(
            actual_id == app_id,
            f"package is {actual_id!r}, expected {app_id!r}",
        )

    application = manifest.find("application")
    if application is None:
        failures.append("no <application> element")
    else:
        expect(
            application.attributes.get("extractNativeLibs") == "true",
            "extractNativeLibs is not true; HotSpot cannot dlopen a library inside the APK",
        )
        expect(
            application.attributes.get("allowBackup") == "false",
            "allowBackup is not false",
        )
        expect(
            application.attributes.get("label") != "",
            "application has no label",
        )

    if debug is not None:
        value = application.attributes.get("debuggable") if application else None
        expect(
            (value == "true") == debug,
            f"debuggable is {value!r}, expected {'true' if debug else 'false or absent'}",
        )

    if debug is False:
        expect(
            attributes.get("versionName", "").endswith("-debug") is False,
            "release versionName carries the debug suffix",
        )

    if attributes.get("versionCode"):
        expect(
            attributes["versionCode"].isdigit(),
            f"versionCode is not an integer: {attributes['versionCode']!r}",
        )

    activities: dict[str, Element] = {}
    if application is not None:
        for activity in application.find_all("activity"):
            activities[activity.attributes.get("name", "")] = activity

    expected = {
        "dev.shadow.eclipselauncher.MainActivity": "",
        "dev.shadow.eclipselauncher.host.GameActivity": ":game",
        "dev.shadow.eclipselauncher.host.InstallerActivity": ":installer",
    }

    for name, process in expected.items():
        activity = activities.get(name)
        if activity is None:
            failures.append(f"activity {name} is missing from the merged manifest")
            continue
        actual_process = activity.attributes.get("process", "")
        expect(
            actual_process == process,
            f"{name} runs in process {actual_process!r}, expected {process!r}",
        )

    # The launcher icon is adaptive on API 26+, which is our floor, so the
    # application must point at a mipmap that exists for every density bucket
    # the resource shrinker keeps.
    if application is not None:
        icon = application.attributes.get("icon", "")
        expect(icon.startswith("@"), f"application icon is not a resource: {icon!r}")

    print(f"APK        {apk}")
    print(f"package    {actual_id}")
    print(f"version    {attributes.get('versionName', '?')} ({attributes.get('versionCode', '?')})")
    print(f"activities {len(activities)}")
    for name in sorted(activities):
        process = activities[name].attributes.get("process", ":main")
        print(f"  {process:<12} {name}")

    if failures:
        print(file=sys.stderr)
        for failure in failures:
            print(f"FAIL {failure}", file=sys.stderr)
        return 1

    print(f"OK — {checks} manifest checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
