#!/usr/bin/env python3
"""Fail when a Maestro flow selects a test tag that TestTags does not declare.

Collects every `id:` selector in the shared flows (maestro/appkb/flows, where
maestro/scripts/fetch-flows.sh puts them, or the directories given as
arguments) and matches it against the names in
TestTags.kt: each `const val` is one name, each `fun` a pattern whose
string-template parts match anything. Selectors inside a
`runFlow` that runs only on iOS (`when: platform: iOS`) select iOS names
and are skipped. A conditional step (`when: visible`, `optional: true`) with
an undeclared name is skipped by Maestro and the flow still passes, so a typo
or a renamed tag would otherwise go unnoticed.
A name a function builds is checked up to its variable part only:
balance_item_BTC_amout passes, because balanceItem() takes any coin code.
Reports each undeclared name as a GitHub error annotation and exits non-zero.
"""

import glob
import os
import re
import sys

TEST_TAGS = "walletkit/src/main/java/io/horizontalsystems/walletkit/uiv3/TestTags.kt"
DEFAULT_FLOWS = ["maestro/appkb/flows"]
CONST = re.compile(r'^\s*const val \w+ = "([^"]*)"', re.MULTILINE)
FUNCTION = re.compile(r'^\s*fun \w+\([^)]*\) = "([^"]*)"', re.MULTILINE)
# $name or ${expression} in a Kotlin string template
KOTLIN_TEMPLATE = re.compile(r"\$\{[^}]*\}|\$\w+")
SELECTOR = re.compile(r"(?<![\w-])id:\s*(.+)$")
RUN_FLOW = re.compile(r"^(\s*)- runFlow:")
PLATFORM = re.compile(r"^\s*platform:\s*(\w+)")
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
    """Yield (line number, id value) for every `id:` selector a flow runs on Android."""
    # (indent of the "- runFlow:" line, platform its when names, or None)
    run_flows: list[list] = []
    for number, line in enumerate(open(path, encoding="utf-8"), start=1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        indent = len(line) - len(line.lstrip())
        while run_flows and indent <= run_flows[-1][0]:
            run_flows.pop()
        if match := RUN_FLOW.match(line):
            run_flows.append([len(match.group(1)), None])
            continue
        if run_flows and (match := PLATFORM.match(line)):
            run_flows[-1][1] = match.group(1)
            continue
        if any(platform not in (None, "Android") for _, platform in run_flows):
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
    flows_dirs = sys.argv[1:] or DEFAULT_FLOWS
    patterns = declared_patterns()
    flows = []
    for flows_dir in flows_dirs:
        found = sorted(glob.glob(os.path.join(flows_dir, "**/*.yaml"), recursive=True))
        if not found:
            print(f"No Maestro flows found in {flows_dir}; run maestro/scripts/fetch-flows.sh")
            return 1
        flows += found

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
