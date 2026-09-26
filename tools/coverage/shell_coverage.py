"""Line coverage for bash scripts, from bash's own xtrace. No kcov/bashcov needed.

How it works:
- trace_env(trace_dir) returns environment variables for running a script under test. Bash reads
  $BASH_ENV before a non-interactive script, and the file it points to turns on xtrace. Every
  executed command then logs "+<start dir>|<script>|<line>| <command>" to a trace file in trace_dir.
- report(scripts, trace_dir) matches those trace lines against each script's executable lines.

"Executable" is a heuristic: every line except blanks, comments, the shebang, and lines holding
only a keyword that xtrace never prints (then, else, fi, do, done, esac, braces, ;;). That is
exact for straight-line scripts like pick_icon.sh. Here-docs and multi-line strings would count
their inner lines as missed; none of the repo's scripts use them.
"""
import argparse
import json
import os
import re
import sys

_NON_EXECUTABLE = re.compile(r"^(then|else|fi|do|done|esac|\{|\}|;;|\(|\))$")
_FUNCTION_HEADER = re.compile(r"^(function\s+)?[A-Za-z_][A-Za-z0-9_]*\s*\(\)\s*\{?$")
_TRACE_LINE = re.compile(r"^\++([^|]*)\|([^|]+)\|(\d+)\| ")

# PS4 is expanded before each traced command; bash repeats its first character per nesting level.
# BASH_SOURCE is relative to the directory the script started in, so that directory is recorded
# once at startup (a later `cd` in the script doesn't change it).
_PS4 = "+${__cov_start_dir}|${BASH_SOURCE[0]}|${LINENO}| "


def executable_lines(path):
    """1-based line numbers xtrace can report for `path`."""
    lines = set()
    with open(path, encoding="utf-8") as f:
        for number, raw in enumerate(f, start=1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if _NON_EXECUTABLE.match(line) or _FUNCTION_HEADER.match(line):
                continue
            lines.add(number)
    return lines


def trace_env(trace_dir):
    """Environment additions that make any bash script started with them log its executed lines
    into trace_dir (one file per process)."""
    os.makedirs(trace_dir, exist_ok=True)
    bash_env = os.path.join(trace_dir, "bash_env.sh")
    with open(bash_env, "w", encoding="utf-8") as f:
        f.write(
            "# Written by tools/coverage/shell_coverage.py: turns on xtrace for coverage.\n"
            f'exec {{__cov_fd}}>>"{trace_dir}/trace.$$"\n'
            "BASH_XTRACEFD=$__cov_fd\n"
            "__cov_start_dir=$PWD\n"
            f"PS4='{_PS4}'\n"
            "set -x\n"
        )
    return {"BASH_ENV": bash_env}


def executed_lines(trace_dir):
    """{absolute script path: set of executed line numbers} from every trace file in trace_dir."""
    executed = {}
    if not os.path.isdir(trace_dir):
        return executed
    for name in sorted(os.listdir(trace_dir)):
        if not name.startswith("trace."):
            continue
        with open(os.path.join(trace_dir, name), encoding="utf-8", errors="replace") as f:
            for line in f:
                match = _TRACE_LINE.match(line)
                if not match:
                    continue  # continuation of a multi-line command
                cwd, source, number = match.groups()
                path = os.path.realpath(os.path.join(cwd, source))
                executed.setdefault(path, set()).add(int(number))
    return executed


def report(scripts, trace_dir):
    """Per-script and total line coverage, as a JSON-ready dict."""
    executed = executed_lines(trace_dir)
    files = {}
    total_valid = total_covered = 0
    for script in scripts:
        valid = executable_lines(script)
        hit = executed.get(os.path.realpath(script), set()) & valid
        files[script] = {
            "lines_valid": len(valid),
            "lines_covered": len(hit),
            "missed_lines": sorted(valid - hit),
        }
        total_valid += len(valid)
        total_covered += len(hit)
    return {"lines_valid": total_valid, "lines_covered": total_covered, "files": files}


def main(argv=None):
    parser = argparse.ArgumentParser(description="Shell line coverage from xtrace files.")
    parser.add_argument("--trace-dir", required=True)
    parser.add_argument("--output", required=True, help="JSON report path")
    parser.add_argument("scripts", nargs="+")
    args = parser.parse_args(argv)

    result = report(args.scripts, args.trace_dir)
    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    for script, data in result["files"].items():
        missed = ",".join(map(str, data["missed_lines"])) or "-"
        sys.stdout.write(f"{script}: {data['lines_covered']}/{data['lines_valid']} lines (missed: {missed})\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
