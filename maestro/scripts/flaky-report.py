#!/usr/bin/env python3
"""Prints the flake rate per flow over the last 14 days of runs.

A flow that failed and then passed on the retry counts as flaky. One that
flaked 3 times, or in more than 10% of its runs, is marked as a quarantine
candidate: tag it `quarantine` and open an issue labelled e2e-flaky (see
maestro/README.md). Journey steps are never quarantined; fix them first.

Usage: flaky-report.py <results.jsonl> [days]
"""
import json
import sys
from collections import defaultdict
from datetime import datetime, timedelta, timezone

path = sys.argv[1]
days = int(sys.argv[2]) if len(sys.argv) > 2 else 14
since = datetime.now(timezone.utc) - timedelta(days=days)

stats = defaultdict(lambda: {"runs": 0, "flaky": 0, "failed": 0})
try:
    lines = open(path).read().splitlines()
except FileNotFoundError:
    lines = []
for line in lines:
    row = json.loads(line)
    finished = datetime.fromisoformat(row["finished"].replace("Z", "+00:00"))
    if finished < since:
        continue
    s = stats[row["flow"]]
    s["runs"] += 1
    if row["result"] == "failed":
        s["flaky" if row["retry"] == "passed" else "failed"] += 1

flaky = {flow: s for flow, s in stats.items() if s["flaky"]}
if not flaky:
    print(f"No flaky tests in the last {days} days ({len(stats)} flows ran).")
    sys.exit(0)

print(f"Flaky tests, last {days} days:")
for flow, s in sorted(flaky.items(), key=lambda item: -item[1]["flaky"]):
    rate = s["flaky"] / s["runs"]
    candidate = s["flaky"] >= 3 or rate > 0.10
    mark = "  → quarantine candidate" if candidate else ""
    print(f"• {flow}: {s['flaky']}/{s['runs']} runs ({rate:.0%}){mark}")
