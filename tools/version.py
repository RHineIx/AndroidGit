#!/usr/bin/env python3
"""Read, validate, and bump AndroidGit's Gradle version fields."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

GRADLE_FILE = Path(__file__).resolve().parents[1] / "app" / "build.gradle.kts"
VERSION_CODE_RE = re.compile(r"(?m)^(\s*versionCode\s*=\s*)(\d+)(\s*)$")
VERSION_NAME_RE = re.compile(r'(?m)^(\s*versionName\s*=\s*"\s*)([^"\n]+?)(\s*"\s*)$')
SEMVER_RE = re.compile(r"^(?P<major>0|[1-9]\d*)\.(?P<minor>0|[1-9]\d*)\.(?P<patch>0|[1-9]\d*)(?:-(?P<suffix>[0-9A-Za-z.-]+))?$")


class VersionError(ValueError):
    pass


def read_version() -> tuple[int, str, str]:
    text = GRADLE_FILE.read_text(encoding="utf-8")
    code_match = VERSION_CODE_RE.search(text)
    name_match = VERSION_NAME_RE.search(text)
    if not code_match or not name_match:
        raise VersionError("Could not find versionCode and versionName in app/build.gradle.kts")
    version_code = int(code_match.group(2))
    version_name = name_match.group(2).strip()
    validate_values(version_code, version_name)
    return version_code, version_name, text


def validate_values(version_code: int, version_name: str) -> None:
    if version_code <= 0:
        raise VersionError("versionCode must be a positive integer")
    if not SEMVER_RE.fullmatch(version_name):
        raise VersionError(f"versionName '{version_name}' must use MAJOR.MINOR.PATCH with an optional suffix")


def parse_semver(version_name: str) -> tuple[int, int, int, str]:
    match = SEMVER_RE.fullmatch(version_name)
    if not match:
        raise VersionError(f"Invalid semantic version: {version_name}")
    return int(match.group("major")), int(match.group("minor")), int(match.group("patch")), match.group("suffix") or ""


def canonical_code(major: int, minor: int, patch: int, current_code: int) -> int:
    return max(current_code + 1, major * 10000 + minor * 100 + patch)


def bump_version(version_name: str, version_code: int, bump_type: str, count: int) -> tuple[int, str]:
    major, minor, patch, suffix = parse_semver(version_name)
    for _ in range(count):
        if bump_type == "major":
            major, minor, patch = major + 1, 0, 0
        elif bump_type == "minor":
            minor, patch = minor + 1, 0
        elif bump_type == "patch":
            patch += 1
        else:
            raise VersionError(f"Unsupported bump type: {bump_type}")
    next_name = f"{major}.{minor}.{patch}" + (f"-{suffix}" if suffix else "")
    return canonical_code(major, minor, patch, version_code), next_name


def write_version(version_code: int, version_name: str, original_text: str) -> None:
    updated = VERSION_CODE_RE.sub(lambda match: f"{match.group(1)}{version_code}{match.group(3)}", original_text, count=1)
    updated = VERSION_NAME_RE.sub(lambda match: f'{match.group(1)}{version_name}{match.group(3)}', updated, count=1)
    if updated == original_text:
        raise VersionError("No version change was written")
    GRADLE_FILE.write_text(updated, encoding="utf-8")


def emit(version_code: int, version_name: str, output_file: str | None) -> None:
    major, minor, patch, suffix = parse_semver(version_name)
    values = {
        "VERSION_CODE": str(version_code),
        "VERSION_NAME": version_name,
        "VERSION_BASE": f"{major}.{minor}.{patch}",
        "VERSION_SUFFIX": suffix,
        "TAG_NAME": f"v{version_name}",
    }
    lines = [f"{key}={value}" for key, value in values.items()]
    print("\n".join(lines))
    if output_file:
        with Path(output_file).open("a", encoding="utf-8") as handle:
            handle.write("\n".join(lines) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    print_parser = subparsers.add_parser("print")
    print_parser.add_argument("--github-output")
    validate_parser = subparsers.add_parser("validate")
    validate_parser.add_argument("--version-code", type=int)
    validate_parser.add_argument("--version-name")
    bump_parser = subparsers.add_parser("bump")
    bump_parser.add_argument("--type", choices=("patch", "minor", "major"), required=True)
    bump_parser.add_argument("--count", type=int, default=1)
    bump_parser.add_argument("--version")
    bump_parser.add_argument("--version-code", type=int)
    bump_parser.add_argument("--github-output")

    args = parser.parse_args()
    current_code, current_name, original_text = read_version()
    if args.command == "print":
        emit(current_code, current_name, args.github_output)
        return
    if args.command == "validate":
        validate_values(args.version_code if args.version_code is not None else current_code, args.version_name or current_name)
        print(f"Validated versionCode={current_code} versionName={current_name}")
        return
    if args.count < 1 or args.count > 20:
        raise VersionError("bump count must be between 1 and 20")
    if args.version:
        major, minor, patch, _ = parse_semver(args.version)
        if args.version_code is not None and args.version_code <= current_code:
            raise VersionError("versionCode override must be greater than the current versionCode")
        next_code = args.version_code or canonical_code(major, minor, patch, current_code)
        next_name = args.version
        validate_values(next_code, next_name)
    else:
        next_code, next_name = bump_version(current_name, current_code, args.type, args.count)
    write_version(next_code, next_name, original_text)
    emit(next_code, next_name, args.github_output)


if __name__ == "__main__":
    try:
        main()
    except (OSError, VersionError) as error:
        raise SystemExit(f"version.py: {error}")
