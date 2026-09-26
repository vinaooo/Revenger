"""Combines Kotlin, Python and shell coverage into one summary (`./gradlew coverageAll`).

Inputs, all optional (a missing one is shown as "not measured"):
- --kotlin: Kover XML (JaCoCo format), from koverXmlReportDebug
- --python: coverage.py XML (Cobertura format), from pytest --cov --cov-report=xml
- --shell:  JSON from tools/coverage/shell_coverage.py

Writes a Markdown table to --output and prints it. The "All code" row adds up lines across the
three languages; branches are only summed where a tool measures them (Kotlin, Python).
"""
import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET


def _counts(covered, valid):
    return {"covered": int(covered), "valid": int(valid)}


def read_kover(path):
    """{"lines": ..., "branches": ...} from the report-level counters of a Kover XML report."""
    root = ET.parse(path).getroot()
    result = {}
    for counter in root.findall("counter"):  # direct children only: the whole-report totals
        kind = counter.get("type")
        if kind in ("LINE", "BRANCH"):
            missed, covered = int(counter.get("missed")), int(counter.get("covered"))
            result["lines" if kind == "LINE" else "branches"] = _counts(covered, covered + missed)
    return result


def read_cobertura(path):
    root = ET.parse(path).getroot()
    result = {"lines": _counts(root.get("lines-covered"), root.get("lines-valid"))}
    if int(root.get("branches-valid", "0")) > 0:
        result["branches"] = _counts(root.get("branches-covered"), root.get("branches-valid"))
    return result


def read_shell(path):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    return {"lines": _counts(data["lines_covered"], data["lines_valid"])}


def _percent(counts):
    if not counts or counts["valid"] == 0:
        return "–"
    return f"{100.0 * counts['covered'] / counts['valid']:.1f}% ({counts['covered']}/{counts['valid']})"


def combine(languages):
    """Adds up lines (and branches, where measured) over the measured languages."""
    total = {}
    for key in ("lines", "branches"):
        parts = [data[key] for data in languages.values() if data and key in data]
        if parts:
            total[key] = _counts(sum(p["covered"] for p in parts), sum(p["valid"] for p in parts))
    return total


def render(languages):
    rows = ["| Code | Lines | Branches |", "|---|---|---|"]
    for name, data in languages.items():
        if data is None:
            rows.append(f"| {name} | not measured | not measured |")
        else:
            rows.append(f"| {name} | {_percent(data.get('lines'))} | {_percent(data.get('branches'))} |")
    total = combine(languages)
    rows.append(f"| **All code** | **{_percent(total.get('lines'))}** | {_percent(total.get('branches'))} |")
    return "\n".join(rows) + "\n"


def _load(reader, path):
    if path and os.path.exists(path):
        return reader(path)
    return None


def main(argv=None):
    parser = argparse.ArgumentParser(description="Combined Kotlin/Python/shell coverage summary.")
    parser.add_argument("--kotlin")
    parser.add_argument("--python")
    parser.add_argument("--shell")
    parser.add_argument("--output", required=True)
    args = parser.parse_args(argv)

    languages = {
        "Kotlin (app, unit tests)": _load(read_kover, args.kotlin),
        "Python (icons/scripts)": _load(read_cobertura, args.python),
        "Shell (*.sh)": _load(read_shell, args.shell),
    }
    table = render(languages)
    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        f.write("# Coverage summary\n\n" + table)
    sys.stdout.write(table)
    return 0


if __name__ == "__main__":
    sys.exit(main())
