#!/usr/bin/env python3
"""Reads the JUnit reports Maestro writes, for run-suite.sh.

  junit.py failed-files <report.xml>
      Prints the flow file of each failed test case, one per line.

  junit.py summary <run dir>
      Prints "[Passed|Failed|Flaky] <flow> (<seconds>s)" per test case. Flaky
      means failed, then passed on the retry.

  junit.py history <run dir> <results.jsonl>
      Appends one JSON line per test case of the run (flow, result, retry
      result, duration, branch, sha) for the flake report.
"""
import json
import os
import sys
import xml.etree.ElementTree as ET


def cases(path):
    if not os.path.exists(path):
        return []
    return ET.parse(path).getroot().iter("testcase")


def failed(case):
    return case.get("status", "SUCCESS") != "SUCCESS" or case.find("failure") is not None


def failed_files(report):
    for case in cases(report):
        if failed(case):
            print(case.get("file"))


def results(run_dir):
    for kind in ("journey", "flows"):
        retried = {c.get("file"): not failed(c)
                   for c in cases(os.path.join(run_dir, f"{kind}-retry.xml"))}
        for case in cases(os.path.join(run_dir, f"{kind}.xml")):
            passed = not failed(case)
            yield case, passed, None if passed else retried.get(case.get("file"), False)


def summary(run_dir):
    for case, passed, retry_passed in results(run_dir):
        label = "Passed" if passed else ("Flaky" if retry_passed else "Failed")
        print(f"[{label}] {case.get('name')} ({round(float(case.get('time') or 0))}s)")


def history(run_dir, out):
    meta = json.load(open(os.path.join(run_dir, "meta.json")))
    lines = []
    for case, passed, retry_passed in results(run_dir):
        lines.append({
            "flow": case.get("name"),
            "file": case.get("file"),
            "result": "passed" if passed else "failed",
            "retry": None if passed else ("passed" if retry_passed else "failed"),
            "duration": float(case.get("time") or 0),
            "branch": meta.get("branch"),
            "sha": meta.get("sha"),
            "finished": meta.get("finished"),
        })
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "a") as f:
        for line in lines:
            f.write(json.dumps(line, ensure_ascii=False) + "\n")


if __name__ == "__main__":
    command, *args = sys.argv[1:]
    {"failed-files": failed_files, "summary": summary, "history": history}[command](*args)
