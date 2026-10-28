#!/usr/bin/env python3
"""Track the F-Droid release of the newest version tag.

A release goes through F-Droid in stages that can take days: the tag must be
picked up into fdroiddata, built in one of the build cycles, signed offline
and finally deployed to the index. This script finds the stage the newest
tag is in, compares it with the stage recorded on the previous run, and
reports a Telegram message when the stage changed. The stage record is a
small JSON document the workflow carries between runs as an artifact.

Environment:
  GITHUB_REPOSITORY  owner/name of this repository
  GITHUB_TOKEN       optional, raises the GitHub API rate limit
  PREVIOUS_STATE     JSON state from the previous run, may be empty
  GITHUB_OUTPUT      where step outputs are written; stdout when unset

Outputs: state (JSON for the next run), changed (true/false), message.
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

PACKAGE = "io.horizontalsystems.bankwallet"
REPOSITORY = os.environ.get("GITHUB_REPOSITORY", "horizontalsystems/unstoppable-wallet-android")
METADATA_URL = f"https://gitlab.com/fdroid/fdroiddata/-/raw/master/metadata/{PACKAGE}.yml"
METADATA_PAGE = f"https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/{PACKAGE}.yml"
INDEX_URL = f"https://f-droid.org/api/v1/packages/{PACKAGE}"
PACKAGE_PAGE = f"https://f-droid.org/packages/{PACKAGE}/"
MONITOR_URL = "https://monitor.f-droid.org/builds"
TAG_PATTERN = re.compile(r"^\d+\.\d+(\.\d+)?$")

# A stage that lasts longer than this gets a single reminder, because F-Droid
# has skipped tags before and nothing else reports that.
STALE_AFTER = timedelta(days=7)


def fetch(url, token=None):
    headers = {"User-Agent": "unstoppable-wallet-fdroid-monitor"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8", errors="replace")


def fetch_optional(url):
    try:
        return fetch(url)
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def latest_tag(token):
    names = []
    page = 1
    while True:
        tags = json.loads(fetch(f"https://api.github.com/repos/{REPOSITORY}/tags?per_page=100&page={page}", token))
        names += [tag["name"] for tag in tags if TAG_PATTERN.match(tag["name"])]
        if len(tags) < 100:
            break
        page += 1
    if not names:
        sys.exit("No version tags found")
    return max(names, key=lambda name: tuple(int(part) for part in name.split(".")))


def version_code(tag):
    gradle = fetch(f"https://raw.githubusercontent.com/{REPOSITORY}/{tag}/app/build.gradle.kts")
    match = re.search(r"^\s*versionCode\s*=\s*(\d+)", gradle, re.MULTILINE)
    if not match:
        sys.exit(f"versionCode not found in app/build.gradle.kts at tag {tag}")
    return int(match.group(1))


def in_metadata(code):
    return re.search(rf"^\s*versionCode:\s*{code}\s*$", fetch(METADATA_URL), re.MULTILINE) is not None


def build_result(code):
    """None while there is no finished build log, else 'built' or 'failed'."""
    log = fetch_optional(f"{MONITOR_URL}/log/{PACKAGE}/{code}")
    if log is None:
        return None
    # A failed log can still say "Successfully built": Gradle succeeded and a
    # later step, such as the reproducibility check, rejected the APK
    if "build failed" in log or "Could not build" in log:
        return "failed"
    if "build succeeded" in log or f"Successfully built {PACKAGE}:{code}" in log:
        return "built"
    return None


def published(code):
    index = json.loads(fetch(INDEX_URL))
    return any(package.get("versionCode") == code for package in index.get("packages", []))


def current_stage(code):
    if published(code):
        return "published"
    result = build_result(code)
    if result:
        return result
    if in_metadata(code):
        return "queued"
    return "waiting"


def describe(stage, tag, code):
    log_url = f"{MONITOR_URL}/log/{PACKAGE}/{code}"
    version = f"{tag} ({code})"
    return {
        "waiting": f"⏳ F-Droid: {version} tagged, not yet picked up into fdroiddata\n{METADATA_PAGE}",
        "queued": f"📦 F-Droid: {version} is in fdroiddata, waiting for a build cycle\n{MONITOR_URL}",
        "built": f"✅ F-Droid: {version} built, waiting for signing and publishing\n{log_url}",
        "failed": f"❌ F-Droid: {version} build failed\n{log_url}",
        "published": f"🚀 F-Droid: {version} published\n{PACKAGE_PAGE}",
    }[stage]


def load_state(raw):
    try:
        state = json.loads(raw) if raw else {}
    except json.JSONDecodeError:
        state = {}
    return state if isinstance(state, dict) else {}


def write_outputs(outputs):
    path = os.environ.get("GITHUB_OUTPUT")
    stream = open(path, "a", encoding="utf-8") if path else sys.stdout
    with stream:
        for key, value in outputs.items():
            stream.write(f"{key}<<FDROID_MONITOR_EOF\n{value}\nFDROID_MONITOR_EOF\n")


def main():
    token = os.environ.get("GITHUB_TOKEN")
    now = datetime.now(timezone.utc)

    tag = latest_tag(token)
    code = version_code(tag)
    stage = current_stage(code)

    previous = load_state(os.environ.get("PREVIOUS_STATE", ""))
    same_version = previous.get("version") == tag
    same_stage = same_version and previous.get("stage") == stage

    if same_stage:
        state = previous
    else:
        state = {"version": tag, "stage": stage, "since": now.isoformat(timespec="seconds")}

    message = ""
    if not same_stage:
        message = describe(stage, tag, code)
    elif stage != "published" and not state.get("reminded"):
        try:
            since = datetime.fromisoformat(state["since"])
        except (KeyError, ValueError):
            since = now
        if now - since > STALE_AFTER:
            days = (now - since).days
            state = dict(state, reminded=True)
            message = f"⚠️ F-Droid: {tag} ({code}) has been in stage '{stage}' for {days} days\n" + describe(stage, tag, code).split("\n", 1)[1]

    print(f"tag {tag}, versionCode {code}, stage {stage}, changed {bool(message)}", file=sys.stderr)
    write_outputs({
        "state": json.dumps(state),
        "changed": "true" if message else "false",
        "message": message,
    })


if __name__ == "__main__":
    main()
