"""Mutation testing for the Kotlin sources: breaks the code one small change at a time and checks
that some unit test fails.

Modes:
  --all             every line of every file in app/src/main/java (the baseline, hours)
  --changed         only the lines this branch changed since its merge base with --base
                    (default origin/develop); a few minutes per PR
  FILE [FILE ...]   every line of the given files

How it works:
- Mutants are textual: == / != swapped, comparisons flipped, && / || swapped, true / false
  swapped, + 1 / - 1 swapped, and a single-line call statement replaced with Unit. Comments,
  string contents, imports and logging lines are never mutated.
- The runs happen in git worktrees of HEAD under --workdir, never in your checkout, so the
  checkout must have no uncommitted changes in app/src or tests/. Gitignored inputs the build
  needs (generated icons, raw sounds, local.properties) are copied in; the ROM is not.
- Each mutant goes through up to three stages and stops at the first one that kills it:
    direct      the test classes whose file names the mutated file's classes
    dependents  also the tests that name any main-source file that names those classes
    full        the whole unit-test suite (at most --full-jobs at once, they are memory-heavy)
  A mutant is only reported as survived after the full suite passes with it, so a narrow test
  selection costs time, never a wrong result.
- A timeout (for example a mutant that makes a loop endless) counts as killed. The test task
  gets a Gradle timeout through an init script, so only that worktree's test JVM is stopped.
- Results are appended to <report-dir>/mutants.jsonl as they finish; a rerun skips mutants
  already recorded there (use --fresh to start over). <report-dir>/summary.md lists the score
  per file and every survivor.
"""
import argparse
import hashlib
import json
import os
import queue
import re
import shutil
import subprocess
import sys
import threading
import time
from dataclasses import dataclass, field

MAIN_SRC = "app/src/main/java"
TEST_SRC = "tests"
ROM_DIR = "app/src/main/assets/rom/"
DEFAULT_WORKDIR = os.path.join(os.path.expanduser("~"), ".cache", "revenger-mutation")

KILLED = "killed"
SURVIVED = "survived"
COMPILE_ERROR = "compile_error"
TIMEOUT = "timeout"
NO_TESTS = "no_tests"
STAGES = ("direct", "dependents", "full")

# ---------------------------------------------------------------------------------------------
# Mutant generation (pure)
# ---------------------------------------------------------------------------------------------

_SKIP_LINE = re.compile(
    r"^\s*(//|\*|/\*|import\s|package\s|@|log\w*\()|\bLog\.[vdiwe]\(|MenuLogger\.|\bTAG\b"
)
_STRING = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'')
_OPERATORS = (
    (re.compile(r"(?<![=!<>])==(?!=)"), "!="),
    (re.compile(r"(?<![=!])!=(?!=)"), "=="),
    (re.compile(r" <= "), " > "),
    (re.compile(r" >= "), " < "),
    (re.compile(r" < "), " >= "),
    (re.compile(r" > "), " <= "),
    (re.compile(r"&&"), "||"),
    (re.compile(r"\|\|"), "&&"),
    (re.compile(r"\btrue\b"), "false"),
    (re.compile(r"\bfalse\b"), "true"),
    (re.compile(r" \+ 1\b(?!\.)"), " - 1"),
    (re.compile(r" - 1\b(?!\.)"), " + 1"),
)
_CALL_STATEMENT = re.compile(r"^\s*[\w.?!()\[\]]*\w\(.*\)\s*$")
_NOT_A_CALL = re.compile(
    r"^\s*(return|val|var|if|else|when|for|while|do|throw|try|catch|finally|fun|class|object|"
    r"interface|init|require|check|super|this)\b|[{}=]|->"
)


@dataclass(frozen=True)
class Mutant:
    """One changed line. `line` is 1-based; `mutated` is the full replacement line text."""

    file: str
    line: int
    operator: str
    original: str
    mutated: str

    @property
    def key(self):
        """Stable across runs as long as the line itself is unchanged."""
        digest = hashlib.sha1(self.original.strip().encode("utf-8")).hexdigest()[:12]
        return f"{self.file}:{self.line}:{self.operator}:{digest}"


def _code_spans(line):
    """(start, end) spans of `line` that are code: outside string literals and before `//`."""
    spans, pos = [], 0
    for literal in _STRING.finditer(line):
        spans.append((pos, literal.start()))
        pos = literal.end()
    spans.append((pos, len(line)))
    code = []
    for start, end in spans:
        comment = line.find("//", start, end)
        if comment != -1:
            code.append((start, comment))
            break
        code.append((start, end))
    return code


_CONTINUES = ("(", ",", "=", ".", "&&", "||", "+", "?:")


def _is_call_statement(code, previous_code, next_code):
    """True when `code` is a whole call statement on its own line: balanced parentheses, not a
    link of a call chain (neither starting one nor continuing one), and not the tail of the
    previous line's expression."""
    stripped = code.strip()
    if not _CALL_STATEMENT.match(code) or _NOT_A_CALL.search(code):
        return False
    if stripped.startswith((".", "?.")) or stripped.count("(") != stripped.count(")"):
        return False
    if next_code.strip().startswith((".", "?.")):
        return False
    return not previous_code.rstrip().endswith(_CONTINUES)


def _code_only(line):
    return "".join(line[s:e] for s, e in _code_spans(line))


def mutants_for_line(file, number, line, previous="", following=""):
    """All mutants of one source line (without its newline). `previous` and `following` are the
    nearest non-blank lines around it, used to tell a call statement from part of a multi-line
    expression."""
    if not line.strip() or _SKIP_LINE.search(line):
        return []
    found = []
    spans = _code_spans(line)
    for pattern, replacement in _OPERATORS:
        for start, end in spans:
            for match in pattern.finditer(line, start, end):
                mutated = line[: match.start()] + replacement + line[match.end():]
                operator = f"{match.group(0).strip()} -> {replacement.strip()}"
                found.append(Mutant(file, number, operator, line, mutated))
    if _is_call_statement(_code_only(line), _code_only(previous), _code_only(following)):
        indent = line[: len(line) - len(line.lstrip())]
        found.append(Mutant(file, number, "remove call", line, indent + "Unit"))
    return found


def generate_mutants(file, text, lines=None):
    """Mutants for `text` (the content of `file`), limited to the 1-based `lines` when given."""
    result = []
    in_block_comment = False
    lines_of_text = text.split("\n")
    previous = ""
    for number, line in enumerate(lines_of_text, start=1):
        stripped = line.strip()
        before, previous = previous, line if stripped else previous
        if in_block_comment:
            in_block_comment = "*/" not in stripped
            continue
        if stripped.startswith("/*") and "*/" not in stripped:
            in_block_comment = True
            continue
        if lines is not None and number not in lines:
            continue
        following = next((later for later in lines_of_text[number:] if later.strip()), "")
        result.extend(mutants_for_line(file, number, line, before, following))
    return result


def apply_mutant(text, mutant):
    """`text` with the mutant's line replaced. Raises ValueError if the line no longer matches."""
    lines = text.split("\n")
    if lines[mutant.line - 1] != mutant.original:
        raise ValueError(f"{mutant.file}:{mutant.line} changed since the mutant was made")
    lines[mutant.line - 1] = mutant.mutated
    return "\n".join(lines)


# ---------------------------------------------------------------------------------------------
# Changed lines (pure parsing)
# ---------------------------------------------------------------------------------------------

_HUNK = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@")


def parse_changed_lines(diff_text):
    """{file: set of new-side line numbers} from `git diff -U0` output."""
    changed, current = {}, None
    for line in diff_text.splitlines():
        if line.startswith("+++ "):
            path = line[4:]
            current = path[2:] if path.startswith("b/") else None
            continue
        match = _HUNK.match(line)
        if match and current:
            start, count = int(match.group(1)), int(match.group(2) or "1")
            changed.setdefault(current, set()).update(range(start, start + count))
    return {path: lines for path, lines in changed.items() if lines}


# ---------------------------------------------------------------------------------------------
# Test selection (pure, over file contents)
# ---------------------------------------------------------------------------------------------

_DECLARATION = re.compile(
    r"^(?:(?:public|internal|private|open|abstract|sealed|data|enum|annotation|inline|value|fun)\s+)*"
    r"(?:class|object|interface)\s+([A-Z]\w*)",
    re.M,
)


def declared_names(text, fallback):
    """Top-level class/object/interface names in a Kotlin file, plus `fallback` (its file stem)."""
    names = set(_DECLARATION.findall(text))
    names.add(fallback)
    return names


def _mentions(text, names):
    return any(re.search(rf"\b{re.escape(name)}\b", text) for name in names)


@dataclass
class SourceIndex:
    """Contents of every main and test source, keyed by repo-relative path."""

    main: dict = field(default_factory=dict)
    tests: dict = field(default_factory=dict)

    @classmethod
    def load(cls, root):
        index = cls()
        for base, target in ((MAIN_SRC, index.main), (TEST_SRC, index.tests)):
            for directory, _, files in os.walk(os.path.join(root, base)):
                for name in files:
                    if name.endswith(".kt"):
                        path = os.path.join(directory, name)
                        with open(path, encoding="utf-8") as f:
                            target[os.path.relpath(path, root)] = f.read()
        return index

    def names_of(self, path):
        stem = os.path.splitext(os.path.basename(path))[0]
        return declared_names(self.main.get(path, ""), stem)

    def test_classes(self, test_paths):
        """Test class filters for Gradle --tests, one per declared test class."""
        classes = set()
        for path in test_paths:
            stem = os.path.splitext(os.path.basename(path))[0]
            declared = {n for n in _DECLARATION.findall(self.tests[path]) if n.endswith(("_test", "Test"))}
            classes.update(declared or {stem})
        return sorted(classes)

    def direct_tests(self, path):
        names = self.names_of(path)
        return sorted(t for t, text in self.tests.items() if _mentions(text, names))

    def dependents(self, path):
        names = self.names_of(path)
        return sorted(p for p, text in self.main.items() if p != path and _mentions(text, names))

    def dependent_tests(self, path):
        tests = set(self.direct_tests(path))
        for user in self.dependents(path):
            tests.update(self.direct_tests(user))
        return sorted(tests)


# ---------------------------------------------------------------------------------------------
# Gradle runs
# ---------------------------------------------------------------------------------------------

_COMPILE_ERROR = re.compile(r"^e: |Compilation error|compileDebug\w*Kotlin' FAILED|Execution failed for task ':app:compile", re.M)
_NO_TESTS = re.compile(r"No tests found for given includes")
_TEST_TIMEOUT = re.compile(r"Timeout has been exceeded|timed out", re.I)


def classify(returncode, output):
    """Outcome of one Gradle test run with a mutant applied."""
    if returncode == 0:
        return SURVIVED
    if _COMPILE_ERROR.search(output):
        return COMPILE_ERROR
    if _NO_TESTS.search(output):
        return NO_TESTS
    if _TEST_TIMEOUT.search(output):
        return TIMEOUT
    return KILLED


INIT_SCRIPT = """// Written by tools/mutation/mutation_testing.py: stops a hung test JVM in this build only.
allprojects {{
    tasks.withType(Test).configureEach {{
        timeout = java.time.Duration.ofSeconds({seconds})
    }}
}}
"""


def gradle_command(test_classes, init_script):
    """Gradle arguments for one run; an empty `test_classes` runs the whole suite."""
    command = ["./gradlew", "testDebugUnitTest", "-PskipAssetStaging", "-q", "--offline",
               "--init-script", init_script]
    for name in test_classes:
        command += ["--tests", f"*.{name}"]
    return command


def run_gradle(worktree, test_classes, init_script, timeout):
    """Runs the tests in `worktree`; returns an outcome from classify()."""
    try:
        process = subprocess.run(gradle_command(test_classes, init_script), cwd=worktree,
                                 capture_output=True, text=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        return TIMEOUT
    return classify(process.returncode, process.stdout + process.stderr)


# ---------------------------------------------------------------------------------------------
# Scheduling (the Gradle runner is injected so this can be tested without Gradle)
# ---------------------------------------------------------------------------------------------


def plan_stages(mutant, index):
    """[(stage, test classes)] to try in order. A stage with the same tests as the one before it
    is dropped; the full suite (empty list) always comes last."""
    stages, previous = [], None
    for stage, tests in (("direct", index.direct_tests(mutant.file)),
                         ("dependents", index.dependent_tests(mutant.file))):
        classes = index.test_classes(tests)
        if classes and classes != previous:
            stages.append((stage, classes))
            previous = classes
    stages.append(("full", []))
    return stages


def evaluate(mutant, stages, run, full_slots):
    """Runs the stages until one kills the mutant. `run(test_classes)` returns an outcome;
    `full_slots` is a semaphore that bounds concurrent full-suite runs. `secs` is the time of all
    stages together, including any wait for a full-suite slot."""
    start = time.monotonic()
    for stage, classes in stages:
        if stage == "full":
            with full_slots:
                outcome = run(classes)
        else:
            outcome = run(classes)
        if outcome == NO_TESTS:
            continue
        if outcome != SURVIVED:
            status = KILLED if outcome == TIMEOUT else outcome
            return {"status": status, "stage": stage, "timeout": outcome == TIMEOUT,
                    "secs": round(time.monotonic() - start, 1)}
    return {"status": SURVIVED, "stage": "full", "timeout": False, "secs": round(time.monotonic() - start, 1)}


def run_mutants(mutants, index, worktrees, run_in, full_jobs, on_result):
    """Evaluates every mutant, one worker thread per worktree.

    `run_in(worktree, test_classes)` runs Gradle in a worktree; `on_result(mutant, result)` is
    called (under a lock) as each mutant finishes. The mutated file is always restored.
    """
    work = queue.Queue()
    for mutant in mutants:
        work.put(mutant)
    full_slots = threading.Semaphore(max(1, full_jobs))
    lock = threading.Lock()
    errors = []

    def worker(worktree):
        while True:
            try:
                mutant = work.get_nowait()
            except queue.Empty:
                return
            path = os.path.join(worktree, mutant.file)
            with open(path, encoding="utf-8") as f:
                original = f.read()
            try:
                mutated = apply_mutant(original, mutant)
                with open(path, "w", encoding="utf-8") as f:
                    f.write(mutated)
                result = evaluate(mutant, plan_stages(mutant, index),
                                  lambda classes, wt=worktree: run_in(wt, classes), full_slots)
            except Exception as error:  # reported at the end; the other workers go on
                with lock:
                    errors.append((mutant, error))
                continue
            finally:
                with open(path, "w", encoding="utf-8") as f:
                    f.write(original)
            with lock:
                on_result(mutant, result)

    threads = [threading.Thread(target=worker, args=(wt,), daemon=True) for wt in worktrees]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()
    return errors


# ---------------------------------------------------------------------------------------------
# Results
# ---------------------------------------------------------------------------------------------


def load_done(results_path):
    """Keys of mutants already recorded in the jsonl file."""
    if not os.path.exists(results_path):
        return set()
    done = set()
    with open(results_path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                done.add(json.loads(line)["key"])
    return done


def result_record(mutant, result):
    return {"key": mutant.key, "file": mutant.file, "line": mutant.line, "operator": mutant.operator,
            "code": mutant.original.strip(), **result}


def summarize(records):
    """Markdown summary: score per file and the list of survivors."""
    per_file = {}
    for record in records:
        counts = per_file.setdefault(record["file"], {KILLED: 0, SURVIVED: 0, COMPILE_ERROR: 0})
        counts[record["status"]] = counts.get(record["status"], 0) + 1
    killed = sum(c[KILLED] for c in per_file.values())
    survived = sum(c[SURVIVED] for c in per_file.values())
    lines = ["# Mutation testing", "",
             f"Mutants: {len(records)}. Killed: {killed}. Survived: {survived}. "
             f"Compile errors (not counted): {sum(c[COMPILE_ERROR] for c in per_file.values())}.",
             f"Score: {_score(killed, survived)}.", "",
             "| File | Killed | Survived | Score |", "|---|---:|---:|---:|"]
    for path in sorted(per_file, key=lambda p: (-per_file[p][SURVIVED], p)):
        c = per_file[path]
        lines.append(f"| {path} | {c[KILLED]} | {c[SURVIVED]} | {_score(c[KILLED], c[SURVIVED])} |")
    survivors = sorted((r for r in records if r["status"] == SURVIVED), key=lambda r: (r["file"], r["line"]))
    lines += ["", "## Survivors", ""]
    if not survivors:
        lines.append("None.")
    for record in survivors:
        lines.append(f"- `{record['file']}:{record['line']}` {record['operator']}: `{record['code']}`")
    return "\n".join(lines) + "\n"


def _score(killed, survived):
    total = killed + survived
    return f"{100 * killed / total:.1f}%" if total else "n/a"


# ---------------------------------------------------------------------------------------------
# Git and worktrees
# ---------------------------------------------------------------------------------------------


def git(root, *args):
    return subprocess.run(["git", *args], cwd=root, capture_output=True, text=True, check=True).stdout


def uncommitted_sources(root):
    """Paths under app/src or tests/ that differ from HEAD (modified, staged or untracked)."""
    out = git(root, "status", "--porcelain", "--", "app/src", TEST_SRC)
    return [line[3:] for line in out.splitlines() if line.strip()]


def changed_lines(root, base):
    """{main-source file: changed line numbers} between the merge base with `base` and HEAD."""
    merge_base = git(root, "merge-base", "HEAD", base).strip()
    diff = git(root, "diff", "-U0", merge_base, "HEAD", "--", MAIN_SRC)
    return {p: lines for p, lines in parse_changed_lines(diff).items() if p.endswith(".kt")}


def build_inputs(root):
    """Gitignored files the build needs in a fresh worktree: generated resources and
    local.properties. The ROM stays out (-PskipAssetStaging doesn't need it)."""
    out = git(root, "ls-files", "--others", "--ignored", "--exclude-standard", "--", "app/src/main")
    paths = [p for p in out.splitlines() if p and not p.startswith(ROM_DIR)]
    if os.path.exists(os.path.join(root, "local.properties")):
        paths.append("local.properties")
    return sorted(paths)


def prepare_worktrees(root, workdir, count, commit):
    """`count` worktrees of `commit` under `workdir`, reused (and reset) when they exist."""
    inputs = build_inputs(root)
    worktrees = []
    for i in range(1, count + 1):
        path = os.path.join(workdir, f"w{i}")
        if os.path.exists(os.path.join(path, ".git")):
            git(path, "checkout", "-q", "--force", "--detach", commit)
            git(path, "clean", "-q", "-fd", "--", "app/src", TEST_SRC)
        else:
            os.makedirs(workdir, exist_ok=True)
            git(root, "worktree", "add", "-q", "--detach", path, commit)
        for rel in inputs:
            target = os.path.join(path, rel)
            os.makedirs(os.path.dirname(target) or path, exist_ok=True)
            shutil.copy2(os.path.join(root, rel), target)
        worktrees.append(path)
    return worktrees


def remove_worktrees(root, worktrees):
    for path in worktrees:
        subprocess.run(["git", "worktree", "remove", "--force", path], cwd=root, capture_output=True)
    subprocess.run(["git", "worktree", "prune"], cwd=root, capture_output=True)


# ---------------------------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------------------------


def repo_root():
    here = os.path.dirname(os.path.abspath(__file__))
    return git(here, "rev-parse", "--show-toplevel").strip()


def select_mutants(root, index, args):
    """Mutants for the chosen mode."""
    if args.changed:
        targets = changed_lines(root, args.base)
    elif args.all:
        targets = {p: None for p in index.main}
    else:
        targets = {}
        for path in args.files:
            rel = os.path.relpath(os.path.abspath(path), root)
            if rel not in index.main:
                raise SystemExit(f"Not a Kotlin file under {MAIN_SRC}: {path}")
            targets[rel] = None
    mutants = []
    for path in sorted(targets):
        if path in index.main:
            mutants.extend(generate_mutants(path, index.main[path], targets[path]))
    return mutants


def parse_args(argv):
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--all", action="store_true", help=f"mutate every file in {MAIN_SRC}")
    parser.add_argument("--changed", action="store_true", help="mutate only the lines this branch changed")
    parser.add_argument("files", nargs="*", default=[], help="Kotlin files to mutate")
    parser.add_argument("--base", default="origin/develop", help="branch --changed compares against")
    parser.add_argument("--jobs", type=int, default=4, help="parallel worktrees (default 4)")
    parser.add_argument("--full-jobs", type=int, default=3, help="parallel full-suite runs (default 3)")
    parser.add_argument("--timeout", type=int, default=600, help="seconds per test run (default 600)")
    parser.add_argument("--workdir", default=DEFAULT_WORKDIR, help="where the worktrees live")
    parser.add_argument("--report-dir", default="build/reports/mutation", help="jsonl + summary.md")
    parser.add_argument("--fresh", action="store_true", help="forget earlier results in report-dir")
    parser.add_argument("--keep-worktrees", action="store_true", help="don't remove the worktrees at the end")
    parser.add_argument("--list", action="store_true", help="only print the mutants, run nothing")
    args = parser.parse_args(argv)
    if sum((args.all, args.changed, bool(args.files))) != 1:
        parser.error("choose exactly one of --all, --changed or FILE")
    return args


def main(argv=None, runner=run_gradle):
    args = parse_args(sys.argv[1:] if argv is None else argv)
    root = repo_root()
    index = SourceIndex.load(root)
    mutants = select_mutants(root, index, args)
    if args.list:
        for mutant in mutants:
            print(f"{mutant.file}:{mutant.line} {mutant.operator}: {mutant.original.strip()}")
        print(f"{len(mutants)} mutants")
        return 0

    dirty = uncommitted_sources(root)
    if dirty:
        print("Commit or stash these first; the runs use HEAD, not your working tree:", file=sys.stderr)
        for path in dirty:
            print(f"  {path}", file=sys.stderr)
        return 2

    report_dir = os.path.join(root, args.report_dir)
    os.makedirs(report_dir, exist_ok=True)
    results_path = os.path.join(report_dir, "mutants.jsonl")
    if args.fresh and os.path.exists(results_path):
        os.remove(results_path)
    done = load_done(results_path)
    todo = [m for m in mutants if m.key not in done]
    print(f"{len(mutants)} mutants, {len(mutants) - len(todo)} already done, {len(todo)} to run")

    init_script = os.path.join(report_dir, "timeout.init.gradle")
    with open(init_script, "w", encoding="utf-8") as f:
        f.write(INIT_SCRIPT.format(seconds=max(60, args.timeout - 30)))

    worktrees = []
    try:
        if todo:
            commit = git(root, "rev-parse", "HEAD").strip()
            worktrees = prepare_worktrees(root, args.workdir, min(args.jobs, len(todo)), commit)
            finished = [0]

            def record(mutant, result):
                finished[0] += 1
                with open(results_path, "a", encoding="utf-8") as f:
                    f.write(json.dumps(result_record(mutant, result)) + "\n")
                print(f"[{finished[0]}/{len(todo)}] {result['status']:<13} {result['stage']:<10} "
                      f"{mutant.file}:{mutant.line} {mutant.operator}", flush=True)

            errors = run_mutants(todo, index, worktrees,
                                 lambda wt, classes: runner(wt, classes, init_script, args.timeout),
                                 args.full_jobs, record)
            for mutant, error in errors:
                print(f"error: {mutant.file}:{mutant.line} {mutant.operator}: {error}", file=sys.stderr)
    finally:
        if worktrees and not args.keep_worktrees:
            remove_worktrees(root, worktrees)

    keys = {m.key for m in mutants}
    records = []
    if os.path.exists(results_path):
        with open(results_path, encoding="utf-8") as f:
            records = [r for r in map(json.loads, filter(str.strip, f)) if r["key"] in keys]
    summary = summarize(records)
    with open(os.path.join(report_dir, "summary.md"), "w", encoding="utf-8") as f:
        f.write(summary)
    print(summary.split("\n\n| File")[0])
    print(f"Report: {os.path.join(report_dir, 'summary.md')}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
