#!/usr/bin/env python3
"""Fail when a translatable string is missing from any supported locale.

Compares every module's src/main/res/values/strings.xml with the locale
files for the languages declared in the LocaleType enum. Reports each
missing or stale key and each placeholder mismatch as a GitHub error
annotation and exits non-zero when anything is wrong.
"""

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

LOCALE_ENUM = "walletkit/src/main/java/io/horizontalsystems/walletkit/helpers/LocaleHelper.kt"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf]|%\.\d+f|%%")


def supported_locale_dirs() -> list[str]:
    """Locale resource suffixes (de, pt-rBR, ...) for every LocaleType tag except English."""
    source = open(LOCALE_ENUM, encoding="utf-8").read()
    enum = re.search(r"enum class LocaleType\b.*?\{(.*?)\}", source, re.DOTALL)
    if not enum:
        sys.exit(f"LocaleType enum not found in {LOCALE_ENUM}")
    tags = re.findall(r'\w+\("([a-zA-Z-]+)"\)', enum.group(1))
    dirs = []
    for tag in tags:
        if tag == "en":
            continue
        base, _, region = tag.partition("-")
        dirs.append(f"{base}-r{region}" if region else base)
    if not dirs:
        sys.exit(f"No locale tags parsed from {LOCALE_ENUM}")
    return dirs


def entries(path: str) -> dict[str, list[str]]:
    """Translatable <string> and <plurals> entries keyed by name, value is the placeholder list for comparison."""
    result = {}
    for element in ET.parse(path).getroot():
        if element.tag not in ("string", "plurals"):
            continue
        if element.get("translatable") == "false":
            continue
        if element.tag == "plurals":
            # every quantity item carries the same placeholders, and locales
            # declare different quantity sets, so compare the distinct set
            found = set()
            for item in element:
                found.update(placeholders("".join(item.itertext())))
            result[element.get("name")] = sorted(found)
        else:
            result[element.get("name")] = placeholders("".join(element.itertext()))
    return result


def placeholders(text: str) -> list[str]:
    """Format specifiers in the text, exactly as written, so an index or type change is reported."""
    return sorted(PLACEHOLDER.findall(text))


def main() -> int:
    """Compare every module's English strings with each supported locale and report problems."""
    locales = supported_locale_dirs()
    sources = sorted(
        f for f in glob.glob("**/src/main/res/values/strings.xml", recursive=True)
        if "/build/" not in f
    )
    if not sources:
        print("No strings.xml source files found.")
        return 1

    problems = 0

    def error(path: str, message: str) -> None:
        """Print a GitHub error annotation for the file and count it."""
        nonlocal problems
        problems += 1
        print(f"::error file={path}::{message}")

    for source in sources:
        res_dir = os.path.dirname(os.path.dirname(source))
        english = entries(source)

        present = {
            d[len("values-"):]
            for d in os.listdir(res_dir)
            if d.startswith("values-") and os.path.isfile(os.path.join(res_dir, d, "strings.xml"))
        }
        for locale in sorted(present - set(locales)):
            error(f"{res_dir}/values-{locale}/strings.xml", f"Locale '{locale}' is not in LocaleType; remove it or add it to the enum")

        for locale in locales:
            path = f"{res_dir}/values-{locale}/strings.xml"
            if not os.path.isfile(path):
                error(path, f"Locale file is missing; {len(english)} strings need translation")
                continue
            translated = entries(path)
            for key in sorted(english.keys() - translated.keys()):
                error(path, f"Missing translation for {key}")
            for key in sorted(translated.keys() - english.keys()):
                error(path, f"Stale key {key} no longer exists in {source}")
            for key in sorted(english.keys() & translated.keys()):
                if english[key] != translated[key]:
                    error(path, f"Placeholders in {key} differ from English: expected {english[key]}, found {translated[key]}")

        print(f"{source}: {len(english)} strings checked against {len(locales)} locales")

    if problems:
        print(f"\n{problems} translation problem(s) found.")
        return 1
    print("All strings are translated.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
