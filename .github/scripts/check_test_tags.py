#!/usr/bin/env python3
"""Fail when a Maestro flow selects a test tag that TestTags does not declare.

Collects every `id:` selector in maestro/ and matches it against the names
in TestTags.kt: each `const val` is one name, each `fun` a pattern whose
string-template parts match anything. A conditional step (`when: visible`,
`optional: true`) with an undeclared name is skipped by Maestro and the flow
still passes, so a typo or a renamed tag would otherwise go unnoticed.
A name a function builds is checked up to its variable part only:
balance_item_BTC_amout passes, because balanceItem() takes any coin code.
Reports each undeclared name as a GitHub error annotation and exits non-zero.
"""

import glob
import re
import sys

TEST_TAGS = "walletkit/src/main/java/io/horizontalsystems/walletkit/uiv3/TestTags.kt"
FLOWS = "maestro/**/*.yaml"
CONST = re.compile(r'^\s*const val \w+ = "([^"]*)"', re.MULTILINE)
FUNCTION = re.compile(r'^\s*fun \w+\([^)]*\) = "([^"]*)"', re.MULTILINE)
# $name or ${expression} in a Kotlin string template
KOTLIN_TEMPLATE = re.compile(r"\$\{[^}]*\}|\$\w+")
SELECTOR = re.compile(r"(?<![\w-])id:\s*(.+)$")
# ${expression} in a flow, or a regex wildcard: Maestro matches id as a regex
FLOW_VARIABLE = re.compile(r"\$\{.*?\}|\.[*+]")
SAMPLE = "x1"


def declared_patterns() -> list[re.Pattern]:
    """One full-match pattern per TestTags name; template parts of a function match anything."""
    source = open(TEST_TAGS, encoding="utf-8").read()
    patterns = [re.compile(re.escape(name)) for name in CONST.findall(source)]
    for template in FUNCTION.findall(source):
        parts = KOTLIN_TEMPLATE.split(template)
        patterns.append(re.compile(".+".join(re.escape(part) for part in parts)))
    if not patterns:
        sys.exit(f"No test tags parsed from {TEST_TAGS}")
    return patterns


def selectors(path: str):
    """Yield (line number, id value) for every `id:` selector in a flow file."""
    for number, line in enumerate(open(path, encoding="utf-8"), start=1):
        if line.lstrip().startswith("#"):
            continue
        match = SELECTOR.search(line)
        if not match:
            continue
        value = re.sub(r"\s+#.*$", "", match.group(1)).strip().rstrip(",}").strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        yield number, value


def main() -> int:
    """Check every flow's id selectors against TestTags and report undeclared names."""
    patterns = declared_patterns()
    flows = sorted(glob.glob(FLOWS, recursive=True))
    if not flows:
        print("No Maestro flows found.")
        return 1

    problems = 0
    checked = 0
    for path in flows:
        for number, value in selectors(path):
            # a variable part stands for whatever a declared function would produce
            sample = FLOW_VARIABLE.sub(SAMPLE, value)
            if sample.replace(SAMPLE, "") == "":
                continue
            checked += 1
            if not any(p.fullmatch(sample) for p in patterns):
                problems += 1
                print(f"::error file={path},line={number}::Test tag '{value}' is not declared in {TEST_TAGS}")

    print(f"{checked} id selectors in {len(flows)} flows checked against {len(patterns)} test tags")
    if problems:
        print(f"\n{problems} undeclared test tag(s) found.")
        return 1
    print("All test tags are declared.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
