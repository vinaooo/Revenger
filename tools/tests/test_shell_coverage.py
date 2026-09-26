"""Tests for tools/coverage/shell_coverage.py: executable-line detection, and real traced bash runs
of small scripts written to tmp_path."""
import json
import os
import shutil
import subprocess

import pytest

import shell_coverage

BASH = shutil.which("bash")
needs_bash = pytest.mark.skipif(BASH is None, reason="bash not installed")

SCRIPT = """#!/bin/bash
# a comment

echo start
if [ "$1" = "yes" ]
then
    echo taken
else
    echo not-taken
fi
greet() {
    echo "hi $1"
}
greet you
"""
# line numbers:     4 echo start, 5 if, 7 echo taken, 9 echo not-taken, 12 echo hi, 14 greet you


def _write(tmp_path, text=SCRIPT, name="script.sh"):
    path = tmp_path / name
    path.write_text(text)
    path.chmod(0o755)
    return path


def _run(script, trace_dir, *args, cwd=None):
    env = dict(os.environ)
    env.update(shell_coverage.trace_env(str(trace_dir)))
    return subprocess.run([BASH, str(script), *args], env=env, cwd=cwd, capture_output=True, text=True, timeout=30)


def test_executable_lines_skip_blanks_comments_keywords_and_function_headers(tmp_path):
    assert shell_coverage.executable_lines(_write(tmp_path)) == {4, 5, 7, 9, 12, 14}


@needs_bash
def test_report_counts_the_lines_a_run_executed(tmp_path):
    script = _write(tmp_path)
    traces = tmp_path / "traces"

    result = _run(script, traces, "yes")

    assert result.stdout.split("\n")[:2] == ["start", "taken"]  # tracing doesn't touch stdout
    data = shell_coverage.report([str(script)], str(traces))
    assert data["files"][str(script)] == {"lines_valid": 6, "lines_covered": 5, "missed_lines": [9]}
    assert (data["lines_valid"], data["lines_covered"]) == (6, 5)


@needs_bash
def test_runs_add_up_across_processes(tmp_path):
    script = _write(tmp_path)
    traces = tmp_path / "traces"

    _run(script, traces, "yes")
    _run(script, traces, "no")

    assert shell_coverage.report([str(script)], str(traces))["files"][str(script)]["missed_lines"] == []


@needs_bash
def test_relative_invocation_and_cd_inside_the_script_map_to_the_same_file(tmp_path):
    script = _write(tmp_path, "#!/bin/bash\ncd /\necho after-cd\n", name="moves.sh")
    traces = tmp_path / "traces"

    _run("moves.sh", traces, cwd=tmp_path)

    data = shell_coverage.report([str(script)], str(traces))
    assert data["files"][str(script)]["missed_lines"] == []


def test_report_without_traces_covers_nothing(tmp_path):
    script = _write(tmp_path)

    data = shell_coverage.report([str(script)], str(tmp_path / "missing"))

    assert data["lines_covered"] == 0
    assert data["files"][str(script)]["missed_lines"] == [4, 5, 7, 9, 12, 14]


def test_executed_lines_ignores_other_files_and_continuation_lines(tmp_path):
    traces = tmp_path / "traces"
    traces.mkdir()
    (traces / "trace.1").write_text(
        f"+{tmp_path}|a.sh|3| echo one\n"
        "second line of a multi-line command\n"
        f"++{tmp_path}|a.sh|5| nested\n"
    )
    (traces / "bash_env.sh").write_text(f"+{tmp_path}|a.sh|9| not a trace file\n")

    assert shell_coverage.executed_lines(str(traces)) == {os.path.realpath(tmp_path / "a.sh"): {3, 5}}


@needs_bash
def test_main_writes_json_and_prints_a_line_per_script(tmp_path, capsys):
    script = _write(tmp_path)
    traces = tmp_path / "traces"
    _run(script, traces, "no")
    output = tmp_path / "out" / "shell.json"

    assert shell_coverage.main(["--trace-dir", str(traces), "--output", str(output), str(script)]) == 0

    assert json.loads(output.read_text())["lines_covered"] == 5
    assert capsys.readouterr().out == f"{script}: 5/6 lines (missed: 7)\n"
