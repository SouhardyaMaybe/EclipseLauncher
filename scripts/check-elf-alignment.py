#!/usr/bin/env python3
"""16 KB gate for native libraries and Android archives.

Checks, without needing the Android SDK:
  * every PT_LOAD segment in an ELF .so has p_align >= 0x4000;
  * every PT_GNU_RELRO range ends on a 16 KB boundary;
  * every uncompressed .so inside an APK/AAR starts at a 16 KB-aligned
    file offset (the zipalign -P 16 contract).

Usage:
  check-elf-alignment.py path/to/lib.so [more.so ...]
  check-elf-alignment.py --dir app/src/main/jniLibs
  check-elf-alignment.py --zip app/build/outputs/apk/release/app-release.apk
"""
from __future__ import annotations

import argparse
import pathlib
import struct
import sys
import zipfile

PT_LOAD = 1
PT_GNU_RELRO = 0x6474E552
PAGE_16K = 0x4000


def elf_load_alignments(path: pathlib.Path) -> list[tuple[int, int, int, int]]:
    """Return (p_align, p_vaddr, p_memsz, p_type) for every program header."""
    data = path.read_bytes()
    if len(data) < 64 or data[:4] != b"\x7fELF":
        raise ValueError("not an ELF file")
    if data[4] != 2:
        raise ValueError("only 64-bit ELF is supported")
    endian = "<" if data[5] == 1 else ">"
    e_phoff = struct.unpack_from(endian + "Q", data, 0x20)[0]
    e_phentsize = struct.unpack_from(endian + "H", data, 0x36)[0]
    e_phnum = struct.unpack_from(endian + "H", data, 0x38)[0]
    headers = []
    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        if off + 56 > len(data):
            raise ValueError("truncated program header table")
        p_type = struct.unpack_from(endian + "I", data, off)[0]
        p_vaddr = struct.unpack_from(endian + "Q", data, off + 16)[0]
        p_memsz = struct.unpack_from(endian + "Q", data, off + 40)[0]
        p_align = struct.unpack_from(endian + "Q", data, off + 48)[0]
        headers.append((p_align, p_vaddr, p_memsz, p_type))
    return headers


def check_elf(path: pathlib.Path) -> list[str]:
    errors = []
    try:
        headers = elf_load_alignments(path)
    except Exception as exc:  # noqa: BLE001 - report, do not traceback
        return [f"{path}: {exc}"]

    loads = [h for h in headers if h[3] == PT_LOAD]
    if not loads:
        errors.append(f"{path}: no PT_LOAD segments")
    for align, _, _, _ in loads:
        if align < PAGE_16K:
            errors.append(f"{path}: PT_LOAD p_align=0x{align:x} (< 0x4000)")
    for align, vaddr, memsz, p_type in headers:
        if p_type == PT_GNU_RELRO and (vaddr + memsz) % PAGE_16K != 0:
            errors.append(
                f"{path}: PT_GNU_RELRO ends at 0x{vaddr + memsz:x} "
                f"(not 16 KB aligned; p_align=0x{align:x})"
            )
    return errors


def zip_so_offsets(path: pathlib.Path) -> list[tuple[str, int, int]]:
    """Return (name, data_offset, compress_type) for every .so in a zip."""
    out = []
    with zipfile.ZipFile(path) as zf:
        for info in zf.infolist():
            if not info.filename.endswith(".so"):
                continue
            with open(path, "rb") as fh:
                fh.seek(info.header_offset)
                header = fh.read(30)
            if len(header) != 30 or header[:4] != b"PK\x03\x04":
                raise ValueError(f"{path}: bad local header for {info.filename}")
            name_len = struct.unpack_from("<H", header, 26)[0]
            extra_len = struct.unpack_from("<H", header, 28)[0]
            data_offset = info.header_offset + 30 + name_len + extra_len
            out.append((info.filename, data_offset, info.compress_type))
    return out


def check_zip(path: pathlib.Path) -> list[str]:
    errors = []
    try:
        entries = zip_so_offsets(path)
    except Exception as exc:  # noqa: BLE001
        return [f"{path}: {exc}"]
    if not entries:
        errors.append(f"{path}: no .so entries")
    for name, offset, compress_type in entries:
        if compress_type != zipfile.ZIP_STORED:
            errors.append(f"{path}: {name} is compressed; Android requires stored .so")
        if offset % PAGE_16K != 0:
            errors.append(
                f"{path}: {name} data offset 0x{offset:x} is not 16 KB aligned"
            )
    return errors


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("paths", nargs="*", type=pathlib.Path)
    ap.add_argument("--dir", type=pathlib.Path)
    ap.add_argument("--zip", dest="zips", nargs="*", default=[], type=pathlib.Path)
    args = ap.parse_args()

    targets = list(args.paths)
    if args.dir:
        targets.extend(sorted(args.dir.rglob("*.so")))
    if not targets and not args.zips:
        ap.error("give at least one path, --dir, or --zip")

    errors: list[str] = []
    for path in targets:
        if not path.is_file():
            errors.append(f"{path}: not a file")
            continue
        if path.suffix == ".so" or path.read_bytes()[:4] == b"\x7fELF":
            errors.extend(check_elf(path))
        elif zipfile.is_zipfile(path):
            errors.extend(check_zip(path))
        else:
            errors.append(f"{path}: not an ELF or zip")

    for path in args.zips:
        errors.extend(check_zip(path))

    for err in errors:
        print("FAIL " + err, file=sys.stderr)
    if errors:
        print(f"16 KB gate: {len(errors)} failure(s)", file=sys.stderr)
        return 1
    print(f"16 KB gate: passed ({len(targets)} ELF target(s), {len(args.zips)} archive(s))")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
