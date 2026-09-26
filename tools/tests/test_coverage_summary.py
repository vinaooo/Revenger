"""Tests for tools/coverage/coverage_summary.py with small hand-written reports."""
import json

import coverage_summary

KOVER = """<?xml version="1.0" ?>
<report name="debug">
  <package name="a">
    <counter type="LINE" missed="999" covered="1"/>
  </package>
  <counter type="INSTRUCTION" missed="10" covered="90"/>
  <counter type="BRANCH" missed="4" covered="6"/>
  <counter type="LINE" missed="25" covered="75"/>
</report>
"""

COBERTURA = """<?xml version="1.0" ?>
<coverage lines-valid="20" lines-covered="19" branches-valid="10" branches-covered="8" line-rate="0.95">
</coverage>
"""


def _files(tmp_path, python=COBERTURA):
    kotlin = tmp_path / "kover.xml"
    kotlin.write_text(KOVER)
    py = tmp_path / "python.xml"
    py.write_text(python)
    shell = tmp_path / "shell.json"
    shell.write_text(json.dumps({"lines_valid": 10, "lines_covered": 8, "files": {}}))
    return kotlin, py, shell


def test_read_kover_uses_the_report_level_counters(tmp_path):
    kotlin, _, _ = _files(tmp_path)

    assert coverage_summary.read_kover(kotlin) == {
        "lines": {"covered": 75, "valid": 100},
        "branches": {"covered": 6, "valid": 10},
    }


def test_read_cobertura_lines_and_branches(tmp_path):
    _, python, _ = _files(tmp_path)

    assert coverage_summary.read_cobertura(python) == {
        "lines": {"covered": 19, "valid": 20},
        "branches": {"covered": 8, "valid": 10},
    }


def test_read_cobertura_without_branch_data(tmp_path):
    _, python, _ = _files(tmp_path, python='<coverage lines-valid="4" lines-covered="2" branches-valid="0"/>')

    assert coverage_summary.read_cobertura(python) == {"lines": {"covered": 2, "valid": 4}}


def test_combine_adds_lines_everywhere_and_branches_where_measured():
    total = coverage_summary.combine({
        "k": {"lines": {"covered": 75, "valid": 100}, "branches": {"covered": 6, "valid": 10}},
        "p": {"lines": {"covered": 19, "valid": 20}, "branches": {"covered": 8, "valid": 10}},
        "s": {"lines": {"covered": 8, "valid": 10}},
        "missing": None,
    })

    assert total == {"lines": {"covered": 102, "valid": 130}, "branches": {"covered": 14, "valid": 20}}


def test_main_writes_the_combined_table(tmp_path, capsys):
    kotlin, python, shell = _files(tmp_path)
    output = tmp_path / "out" / "summary.md"

    code = coverage_summary.main([
        "--kotlin", str(kotlin), "--python", str(python), "--shell", str(shell), "--output", str(output),
    ])

    assert code == 0
    printed = capsys.readouterr().out
    assert output.read_text() == "# Coverage summary\n\n" + printed
    assert "| Kotlin (app, unit tests) | 75.0% (75/100) | 60.0% (6/10) |" in printed
    assert "| Python (icons/scripts) | 95.0% (19/20) | 80.0% (8/10) |" in printed
    assert "| Shell (*.sh) | 80.0% (8/10) | – |" in printed
    assert "| **All code** | **78.5% (102/130)** | 70.0% (14/20) |" in printed


def test_missing_inputs_are_shown_as_not_measured(tmp_path, capsys):
    coverage_summary.main(["--python", str(tmp_path / "nope.xml"), "--output", str(tmp_path / "s.md")])

    printed = capsys.readouterr().out
    assert "| Python (icons/scripts) | not measured | not measured |" in printed
    assert "| **All code** | **–** | – |" in printed
