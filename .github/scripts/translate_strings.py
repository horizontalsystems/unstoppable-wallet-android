#!/usr/bin/env python3
"""
Auto-translate new Android strings.xml entries on PRs.
Detects new/changed English keys and translates to all supported languages.
"""

import os
import re
import sys
import json
import glob
import argparse
import anthropic

# Languages from LocaleType enum in core/src/main/java/io/horizontalsystems/core/helpers/LocaleHelper.kt
LANGUAGES = {
    "de": "German",
    "es": "Spanish",
    "pt-BR": "Brazilian Portuguese",
    "fa": "Persian (Farsi)",
    "fr": "French",
    "ko": "Korean",
    "ru": "Russian",
    "tr": "Turkish",
    "zh": "Chinese (Simplified)",
}

SNAPSHOT_FILE = "translation_snapshot.json"

# Matches <string name="key" [attrs]>value</string>, including multi-line values
ELEMENT_PATTERN = re.compile(
    r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>',
    re.DOTALL,
)


def lang_to_dir(lang: str) -> str:
    """Convert lang tag to Android resource directory suffix (pt-BR → pt-rBR)."""
    if "-" in lang:
        base, region = lang.split("-", 1)
        return f"{base}-r{region}"
    return lang


def is_translatable(attrs: str) -> bool:
    return 'translatable="false"' not in attrs


def has_cdata(value: str) -> bool:
    return "<![CDATA[" in value


def escape_xml_value(value: str) -> str:
    """Escape characters that must be escaped in Android strings.xml values."""
    # & first — convert &apos; to \' (Android rejects the XML entity), then
    # escape any bare & that isn't part of a remaining named entity
    value = value.replace('&apos;', "\\'")
    value = re.sub(r'&(?!(amp|lt|gt|quot);)', '&amp;', value)
    value = value.replace('<', '&lt;').replace('>', '&gt;')
    value = re.sub(r'(?<!\\)"', r'\\"', value)
    value = re.sub(r"(?<!\\)'", r"\\'", value)
    return value


def parse_strings(content: str) -> dict[str, str]:
    """Return all translatable, non-CDATA strings from XML content."""
    result = {}
    for m in ELEMENT_PATTERN.finditer(content):
        key, attrs, value = m.group(1), m.group(2), m.group(3)
        if is_translatable(attrs) and not has_cdata(value):
            result[key] = value.strip()
    return result


def find_source_files() -> list[str]:
    """Find all module default strings.xml files (excludes build/ dirs)."""
    return sorted(
        f for f in glob.glob("**/src/main/res/values/strings.xml", recursive=True)
        if "/build/" not in f
    )


def load_snapshot() -> dict:
    try:
        with open(SNAPSHOT_FILE, encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return {}


def save_snapshot(snapshot: dict) -> None:
    with open(SNAPSHOT_FILE, "w", encoding="utf-8") as f:
        json.dump(snapshot, f, indent=2, ensure_ascii=False, sort_keys=True)


def translate_new_strings(new_strings: dict[str, str]) -> dict[str, dict[str, str]]:
    """Call Claude API. Returns {lang_code: {key: translated_value}}."""
    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])

    strings_block = "\n".join(
        f'<string name="{k}">{v}</string>' for k, v in new_strings.items()
    )
    lang_list = "\n".join(f"- {code}: {name}" for code, name in LANGUAGES.items())

    prompt = f"""You are a professional technical translator for a cryptocurrency wallet Android app. Translate the following Android strings.xml entries from English into all target languages listed below. Use industry-standard blockchain and crypto terminology (wallet, token, blockchain, swap, gas fee, private key, DeFi, NFT, dApp).

Target languages:
{lang_list}

English strings:
{strings_block}

Rules:
- Keep name attributes exactly as-is (never translate the keys)
- Preserve all Android format placeholders: %s, %d, %1$s, %2$s, %3$s, etc.
- Preserve literal \\n newline sequences as \\n
- Preserve XML entities &amp; &lt; &gt; — do NOT use &apos; or &quot;, use \' and \" instead (Android requirement)
- Return ONLY valid JSON with this exact structure:
{{
  "de": {{"KeyName": "translated value", ...}},
  "es": {{...}},
  ...
}}
No markdown fences, no explanation. Only the JSON object.
"""

    message = client.messages.create(
        model="claude-haiku-4-5-20251001",
        max_tokens=8192,
        messages=[{"role": "user", "content": prompt}],
    )

    text = re.sub(r"```(?:json)?\s*|\s*```", "", message.content[0].text).strip()
    return json.loads(text)


def apply_translations(lang_file: str, new_translations: dict[str, str], deleted_keys: set[str]) -> None:
    """Update a translation file in-place: replace existing entries, append new ones, remove deleted."""
    try:
        with open(lang_file, encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        content = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n</resources>\n'

    # Remove deleted keys
    for key in deleted_keys:
        content = re.sub(
            rf'[ \t]*<string\s+name="{re.escape(key)}"[^>]*>.*?</string>[ \t]*\n?',
            "",
            content,
            flags=re.DOTALL,
        )

    # Apply new/updated translations
    for key, value in new_translations.items():
        value = escape_xml_value(value)
        existing_pat = re.compile(
            rf'<string\s+name="{re.escape(key)}"[^>]*>.*?</string>',
            re.DOTALL,
        )
        new_entry = f'<string name="{key}">{value}</string>'
        if existing_pat.search(content):
            content = existing_pat.sub(new_entry, content)
        else:
            content = content.replace("</resources>", f"    {new_entry}\n</resources>")

    os.makedirs(os.path.dirname(lang_file), exist_ok=True)
    with open(lang_file, "w", encoding="utf-8") as f:
        f.write(content)


def process_module(source_file: str, snapshot: dict) -> bool:
    """Process one module's strings.xml. Returns True if any files were updated."""
    module_snapshot = snapshot.get(source_file, {})

    try:
        with open(source_file, encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        return False

    current = parse_strings(content)
    new_strings = {k: v for k, v in current.items() if module_snapshot.get(k) != v}
    deleted_keys = set(module_snapshot.keys()) - set(current.keys())

    if not new_strings and not deleted_keys:
        return False

    print(f"\n  {source_file}:")
    if deleted_keys:
        print(f"    {len(deleted_keys)} deleted: {', '.join(sorted(deleted_keys))}")
    if new_strings:
        print(f"    {len(new_strings)} new/changed — translating...")

    translations_by_lang: dict[str, dict[str, str]] = {}
    if new_strings:
        translations_by_lang = translate_new_strings(new_strings)

    res_dir = os.path.dirname(os.path.dirname(source_file))  # .../src/main/res
    for lang in LANGUAGES:
        lang_file = os.path.join(res_dir, f"values-{lang_to_dir(lang)}", "strings.xml")
        apply_translations(lang_file, translations_by_lang.get(lang, {}), deleted_keys)
        print(f"    ✓ {lang_file}")

    snapshot[source_file] = current
    return True


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--init",
        action="store_true",
        help="Bootstrap snapshot from current EN files without translating",
    )
    args = parser.parse_args()

    source_files = find_source_files()
    if not source_files:
        print("No strings.xml source files found.")
        sys.exit(0)

    snapshot = load_snapshot()

    if args.init:
        for source_file in source_files:
            try:
                with open(source_file, encoding="utf-8") as f:
                    snapshot[source_file] = parse_strings(f.read())
                print(f"  ✓ {source_file} ({len(snapshot[source_file])} keys)")
            except FileNotFoundError:
                pass
        save_snapshot(snapshot)
        print("Snapshot initialized. No translation performed.")
        sys.exit(0)

    print("Checking for new/changed English strings...")
    any_changes = False
    for source_file in source_files:
        if process_module(source_file, snapshot):
            any_changes = True

    if not any_changes:
        print("No new, changed, or deleted strings found. Nothing to do.")
        sys.exit(0)

    save_snapshot(snapshot)
    print("\nDone.")


if __name__ == "__main__":
    main()
