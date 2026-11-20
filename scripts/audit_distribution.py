#!/usr/bin/env python3
"""Audit OpenRadio's actual release dependency graph and Android network policy."""
from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"

PROPRIETARY_GROUPS = (
    "com.google.android.gms",
    "com.google.firebase",
    "com.google.android.play",
    "com.google.android.ump",
    "com.facebook.android",
    "com.appsflyer",
    "com.adjust.sdk",
    "com.onesignal",
)
REQUIRED_HTTPS_DOMAINS = frozenset((
    "ipapi.co", "ipwho.is", "api.iheart.com",
    "raw.githubusercontent.com", "api.somafm.com",
    "itunes.apple.com", "fmstream.org",
    "de1.api.radio-browser.info", "fi1.api.radio-browser.info",
    "at1.api.radio-browser.info", "www.google.com", "wsrv.nl",
))
API_SOURCES = (
    "app/src/main/java/com/example/data/MultiSourceRadioApi.kt",
    "app/src/main/java/com/example/data/RadioBrowserApi.kt",
)


def check_dependency_graph(graph: str) -> list[str]:
    """Look for forbidden dependency coordinates in Gradle's runtimeClasspath tree."""
    errors = []
    coordinate = re.compile(r"\b([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([A-Za-z0-9_.+-]+)")
    for line_number, line in enumerate(graph.splitlines(), start=1):
        for match in coordinate.finditer(line):
            group = match.group(1)
            if any(group == blocked or group.startswith(blocked + ".")
                   for blocked in PROPRIETARY_GROUPS):
                errors.append(f"Dependency at line {line_number}: {match.group(0)}")
    return errors


def check_project(root: Path) -> list[str]:
    errors = []
    manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
    application = manifest.find("application")
    if application is None:
        return ["AndroidManifest.xml lacks an application element"]
    if application.attrib.get(ANDROID + "networkSecurityConfig") != "@xml/network_security_config":
        errors.append("Missing audited Network Security Config in application manifest")
    if ANDROID + "usesCleartextTraffic" in application.attrib:
        errors.append("Remove redundant usesCleartextTraffic flag; use Network Security Config")

    config = ET.parse(root / "app/src/main/res/xml/network_security_config.xml").getroot()
    base = config.find("base-config")
    if base is None or base.attrib.get("cleartextTrafficPermitted") != "true":
        errors.append("Third-party HTTP radio stream compatibility setting changed; needs review")
    protected = set()
    for domain_config in config.findall("domain-config"):
        if domain_config.attrib.get("cleartextTrafficPermitted") == "false":
            protected.update((domain.text or "").strip() for domain in domain_config.findall("domain"))
    missing = sorted(REQUIRED_HTTPS_DOMAINS - protected)
    if missing:
        errors.append("Missing TLS-only API hosts: " + ", ".join(missing))

    for relative_path in API_SOURCES:
        source = (root / relative_path).read_text(encoding="utf-8")
        if re.search(r'\.baseUrl\(\s*"http://', source):
            errors.append(f"Unencrypted Retrofit endpoint in {relative_path}")
        if not re.search(r"\.followSslRedirects\(false\)", source):
            errors.append(f"API client must block HTTPS-to-HTTP redirects: {relative_path}")

    geo = (root / "app/src/main/java/com/example/ui/RadioViewModel.kt").read_text(encoding="utf-8")
    if "detectAutoIpLocation(triggerFetch = false)" in geo:
        errors.append("IP geolocation must not automatically run at startup")
    logos = (root / "app/src/main/java/com/example/util/LogoImageLoader.kt").read_text(encoding="utf-8")
    if 'candidates.add("http://"' in logos or ".followSslRedirects(true)" in logos:
        errors.append("Logo loader may downgrade HTTPS images")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dependencies", type=Path, required=True)
    args = parser.parse_args()
    try:
        errors = check_project(ROOT)
        errors += check_dependency_graph(args.dependencies.read_text(encoding="utf-8"))
    except (OSError, ET.ParseError) as exc:
        print(f"AUDIT ERROR: {exc}", file=sys.stderr)
        return 2
    for item in errors:
        print("FAIL: " + item, file=sys.stderr)
    if errors:
        return 1
    print("PASS: No known proprietary/tracking runtime SDK groups detected.")
    print("PASS: Application API and image fallback hosts are TLS-only.")
    print("EXCEPTION: Dynamic third-party HTTP radio streams remain supported.")
    print("Manual dependency licence, attribution and provenance review is still required.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
