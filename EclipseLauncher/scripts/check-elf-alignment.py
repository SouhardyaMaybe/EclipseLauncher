#!/usr/bin/env python3
"""16 KB gate for native libraries and Android archives.

Three rules are checked, and they are not equally strict:

  * **Hard:** every 64-bit ``PT_LOAD`` segment must have ``p_align >= 0x4000``
    and satisfy ``(p_vaddr - p_offset) % p_align == 0``. This is the rule a
    16 KB kernel enforces; a failure here means the library cannot load.
  * **Hard:** the page-rounded RELRO range must not cover writable memory that
    lies outside the RELRO range. That is the documented crash: the linker
    finds writable data inside the region it just made read-only.
  * **Warning:** a RELRO range whose end is not 16 KB aligned. On Android this
    makes ``mprotect`` span the following page; if that page is unmapped the
    hardening is skipped with a linker warning, and if it holds writable data
    the process crashes — which the hard rule above already catches precisely.
    Google's own ``androidx.graphics.path`` ships libraries in this state, so
    the warning is reported but not treated as a release blocker.

The fourth rule concerns packaging, not the ELF: an *uncompressed* ``.so``
inside a zip must start at a 16 KB-aligned offset (``zipalign -P 16``). With
``extractNativeLibs=true`` — this app's configuration, see docs/04 §9.1 —
libraries are compressed and extracted at install time, so the offset rule does
not apply. Compressed entries are accepted by default; ``--require-stored``
restores the strict zip rule for artifacts that are mapped in place.

Usage:
  check-elf-alignment.py path/to/lib.so [more.so ...]
  check-elf-alignment.py --dir app/src/main/jniLibs
  check-elf-alignment.py --zip app/build/outputs/apk/release/app-release.apk
  check-elf-alignment.py --require-stored --zip some-uncompressed.aar

Exit: 0 pass, 1 failure, 2 usage.
"""
from __future__ import annotations

import argparse
import pathlib
import struct
import sys
import zipfile

PT_LOAD = 1
PT_GNU_RELRO = 0x6474E552
PF_W = 0x2
PAGE_16K = 0x4000

ELF_MAGIC = b"\x7fELF"
ELFCLASS32 = 1
ELFCLASS64 = 2


def program_headers(data: bytes) -> list[dict[str, int]]:
    """Return every program header as a dict.

    Handles both ELF classes: an APK can still carry an armeabi-v7a library,
    and refusing to parse it would hide a real structural problem.
    """
    if len(data) < 52 or data[:4] != ELF_MAGIC:
        raise ValueError("not an ELF file")
    elf_class = data[4]
    endian = "<" if data[5] == 1 else ">"

    if elf_class == ELFCLASS64:
        e_phoff = struct.unpack_from(endian + "Q", data, 0x20)[0]
        e_phentsize = struct.unpack_from(endian + "H", data, 0x36)[0]
        e_phnum = struct.unpack_from(endian + "H", data, 0x38)[0]
        minimum = 56
    elif elf_class == ELFCLASS32:
        e_phoff = struct.unpack_from(endian + "I", data, 0x1C)[0]
        e_phentsize = struct.unpack_from(endian + "H", data, 0x2A)[0]
        e_phnum = struct.unpack_from(endian + "H", data, 0x2C)[0]
        minimum = 32
    else:
        raise ValueError(f"unknown ELF class {elf_class}")

    headers = []
    for index in range(e_phnum):
        offset = e_phoff + index * e_phentsize
        if offset + minimum > len(data):
            raise ValueError("truncated program header table")
        header = {"type": struct.unpack_from(endian + "I", data, offset)[0]}
        if elf_class == ELFCLASS64:
            header["flags"] = struct.unpack_from(endian + "I", data, offset + 4)[0]
            header["offset"] = struct.unpack_from(endian + "Q", data, offset + 8)[0]
            header["vaddr"] = struct.unpack_from(endian + "Q", data, offset + 16)[0]
            header["memsz"] = struct.unpack_from(endian + "Q", data, offset + 40)[0]
            header["align"] = struct.unpack_from(endian + "Q", data, offset + 48)[0]
        else:
            header["offset"] = struct.unpack_from(endian + "I", data, offset + 4)[0]
            header["vaddr"] = struct.unpack_from(endian + "I", data, offset + 8)[0]
            header["memsz"] = struct.unpack_from(endian + "I", data, offset + 20)[0]
            header["flags"] = struct.unpack_from(endian + "I", data, offset + 24)[0]
            header["align"] = struct.unpack_from(endian + "I", data, offset + 28)[0]
        headers.append(header)
    return headers


def check_elf(name: str, data: bytes) -> tuple[list[str], list[str]]:
    """Return (failures, warnings) for one ELF object."""
    failures: list[str] = []
    warnings: list[str] = []

    try:
        headers = program_headers(data)
    except Exception as exc:  # noqa: BLE001 - report, do not traceback
        return [f"{name}: {exc}"], []

    sixty_four = len(data) > 4 and data[4] == ELFCLASS64
    loads = [header for header in headers if header["type"] == PT_LOAD]
    if not loads:
        failures.append(f"{name}: no PT_LOAD segments")

    if sixty_four:
        for header in loads:
            align = header["align"]
            if align < PAGE_16K:
                failures.append(f"{name}: PT_LOAD p_align=0x{align:x} (< 0x4000)")
                continue
            if (header["vaddr"] - header["offset"]) % align != 0:
                failures.append(
                    f"{name}: PT_LOAD vaddr 0x{header['vaddr']:x} and offset "
                    f"0x{header['offset']:x} are incongruent modulo 0x{align:x}"
                )

    relro = [header for header in headers if header["type"] == PT_GNU_RELRO]
    writable = [header for header in loads if header["flags"] & PF_W]

    for segment in relro:
        start = segment["vaddr"]
        end = start + segment["memsz"]
        protected_start = start & ~(PAGE_16K - 1)
        protected_end = (end + PAGE_16K - 1) & ~(PAGE_16K - 1)

        if end % PAGE_16K != 0:
            warnings.append(
                f"{name}: GNU_RELRO ends at 0x{end:x}, not 16 KB aligned "
                f"(mprotect spans 0x{protected_end:x})"
            )

        # The crash the platform documentation describes: page rounding makes
        # writable memory read-only. RELRO's own range is writable before the
        # protection is applied, so only the excess counts.
        for header in writable:
            memory_start = header["vaddr"]
            memory_end = memory_start + header["memsz"]
            guarded = min(memory_end, protected_end) - max(memory_start, protected_start)
            guarded = max(guarded, 0)
            inside_relro = min(memory_end, end) - max(memory_start, start)
            inside_relro = max(inside_relro, 0)
            if guarded > inside_relro:
                failures.append(
                    f"{name}: RELRO rounding 0x{protected_start:x}-0x{protected_end:x} "
                    f"covers writable memory 0x{memory_start:x}-0x{memory_end:x}"
                )

    return failures, warnings


def check_zip(path: pathlib.Path, require_stored: bool) -> tuple[list[str], list[str], int]:
    failures: list[str] = []
    warnings: list[str] = []
    checked = 0

    try:
        with zipfile.ZipFile(path) as archive:
            entries = [
                (info, archive.read(info))
                for info in archive.infolist()
                if info.filename.endswith(".so")
            ]
    except Exception as exc:  # noqa: BLE001
        return [f"{path}: {exc}"], [], 0

    if not entries:
        return [f"{path}: no .so entries"], [], 0

    for info, content in entries:
        checked += 1
        name = f"{path}!{info.filename}"
        found, warned = check_elf(name, content)
        failures.extend(found)
        warnings.extend(warned)

        stored = info.compress_type == zipfile.ZIP_STORED
        if not stored and require_stored:
            failures.append(f"{name}: is compressed but --require-stored was given")
        if stored:
            # Data offset is the local header plus its variable-length parts.
            data_offset = info.header_offset + 30 + len(info.filename.encode()) + len(info.extra)
            if data_offset % PAGE_16K != 0:
                failures.append(
                    f"{name}: stored data offset 0x{data_offset:x} is not 16 KB aligned"
                )

    return failures, warnings, checked


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="*", type=pathlib.Path)
    parser.add_argument("--dir", type=pathlib.Path)
    parser.add_argument("--zip", dest="zips", nargs="*", default=[], type=pathlib.Path)
    parser.add_argument(
        "--require-stored",
        action="store_true",
        help="fail when a .so is compressed (use for artifacts that are mapped in place)",
    )
    parser.add_argument("--quiet-warnings", action="store_true")
    args = parser.parse_args()

    targets = list(args.paths)
    if args.dir:
        targets.extend(sorted(args.dir.rglob("*.so")))
    if not targets and not args.zips:
        parser.error("give at least one path, --dir, or --zip")

    failures: list[str] = []
    warnings: list[str] = []
    elf_count = 0

    for path in targets:
        if not path.is_file():
            failures.append(f"{path}: not a file")
            continue
        data = path.read_bytes()
        if data[:4] == ELF_MAGIC:
            elf_count += 1
            found, warned = check_elf(str(path), data)
        elif zipfile.is_zipfile(path):
            found, warned, count = check_zip(path, args.require_stored)
            elf_count += count
        else:
            failures.append(f"{path}: not an ELF or zip")
            continue
        failures.extend(found)
        warnings.extend(warned)

    for path in args.zips:
        found, warned, count = check_zip(path, args.require_stored)
        failures.extend(found)
        warnings.extend(warned)
        elf_count += count

    if warnings and not args.quiet_warnings:
        for warning in warnings:
            print("WARN " + warning, file=sys.stderr)

    for failure in failures:
        print("FAIL " + failure, file=sys.stderr)

    if failures:
        print(f"16 KB gate: {len(failures)} failure(s), {len(warnings)} warning(s)", file=sys.stderr)
        return 1

    print(
        f"16 KB gate: passed ({elf_count} ELF object(s), {len(args.zips)} archive(s), "
        f"{len(warnings)} warning(s))"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
