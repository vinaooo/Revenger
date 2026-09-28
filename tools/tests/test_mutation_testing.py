"""Tests for tools/mutation/mutation_testing.py. Gradle is replaced by fakes; the git parts run
against throwaway repositories in tmp_path."""
import json
import os
import shutil
import subprocess
import threading
import time

import pytest

import mutation_testing as mt

needs_git = pytest.mark.skipif(shutil.which("git") is None, reason="git not installed")


def _operators(line, previous="", following=""):
    return [m.operator for m in mt.mutants_for_line("A.kt", 1, line, previous, following)]


# --- mutant generation ------------------------------------------------------------------------


def test_every_operator_on_a_line_becomes_its_own_mutant():
    ops = _operators("        if (a == b && c > 0 || d != e) return true")

    assert ops == ["== -> !=", "!= -> ==", "> -> <=", "&& -> ||", "|| -> &&", "true -> false"]


def test_comparisons_and_off_by_one_are_mutated():
    assert _operators("x <= y") == ["<= -> >"]
    assert _operators("x >= y") == [">= -> <"]
    assert _operators("x < y") == ["< -> >="]
    assert _operators("val n = size - 1") == ["- 1 -> + 1"]
    assert _operators("val n = size + 1") == ["+ 1 -> - 1"]
    assert _operators("val n = size + 10") == []
    assert _operators("val n = size + 1.5f") == []


def test_identity_equality_is_left_alone():
    assert _operators("if (a === b) x") == []
    assert _operators("if (a !== b) x") == []


def test_string_contents_and_trailing_comments_are_not_mutated():
    assert _operators('val s = "a == b && true" + c') == []
    assert _operators("val c = 'x' // flag == true") == []
    assert _operators('val s = "\\"quoted == \\"" == other') == ["== -> !="]


@pytest.mark.parametrize("line", [
    "",
    "import a.b.c",
    "package a.b",
    "// if (a == b)",
    "     * a == b in KDoc",
    "@Suppress(\"x\") val y = a == b",
    "        Log.d(TAG, \"x\" + (a == b))",
    "        MenuLogger.debug(a == b)",
    "        log(\"done\")",
    "        logDebug(a == b)",
])
def test_comments_imports_annotations_and_logging_are_skipped(line):
    assert mt.mutants_for_line("A.kt", 1, line) == []


def test_mutant_holds_the_whole_replacement_line():
    (mutant,) = mt.mutants_for_line("A.kt", 7, "    if (a == b) go()")

    assert mutant.file == "A.kt"
    assert mutant.line == 7
    assert mutant.original == "    if (a == b) go()"
    assert mutant.mutated == "    if (a != b) go()"


def test_a_standalone_call_statement_is_replaced_with_unit():
    (mutant,) = mt.mutants_for_line("A.kt", 3, "        listener.onDone(value)")

    assert mutant.operator == "remove call"
    assert mutant.mutated == "        Unit"


@pytest.mark.parametrize("line, previous, following", [
    ("return compute()", "", ""),
    ("val x = compute()", "", ""),
    ("items.forEach { use(it) }", "", ""),
    (".replace(id, fragment)", "", ""),
    ("?.let(consume)", "", ""),
    ("Builder()", "", ".build()"),
    ("Builder()", "", "?.build()"),
    ("compute(a,", "", ""),
    ("compute()", "val x =", ""),
    ("compute()", "call(", ""),
    ("compute()", "first &&", ""),
    ("compute()", "a ?:", ""),
    ("when (x) -> go()", "", ""),
])
def test_lines_that_are_not_whole_call_statements_keep_their_call(line, previous, following):
    assert "remove call" not in _operators(line, previous, following)


def test_generate_mutants_skips_block_comments_and_honours_the_line_filter():
    text = "\n".join([
        "/*",
        " a == b",
        "*/",
        "fun f() {",
        "    if (a == b) go()",
        "    if (c != d) stop()",
        "}",
    ])

    everything = mt.generate_mutants("A.kt", text)
    only_six = mt.generate_mutants("A.kt", text, {6})

    assert [(m.line, m.operator) for m in everything] == [(5, "== -> !="), (6, "!= -> ==")]
    assert [(m.line, m.operator) for m in only_six] == [(6, "!= -> ==")]


def test_generate_mutants_passes_the_nearest_non_blank_neighbours():
    text = "val x =\n\n    compute()\n\n    .done()\nnext()\n"

    assert mt.generate_mutants("A.kt", text) == [
        mt.Mutant("A.kt", 6, "remove call", "next()", "Unit"),
    ]


def test_apply_mutant_replaces_only_its_line():
    text = "a\n  if (x == y) go()\nc\n"
    (mutant,) = [m for m in mt.generate_mutants("A.kt", text) if m.operator == "== -> !="]

    assert mt.apply_mutant(text, mutant) == "a\n  if (x != y) go()\nc\n"


def test_apply_mutant_refuses_a_line_that_changed():
    mutant = mt.Mutant("A.kt", 1, "== -> !=", "a == b", "a != b")

    with pytest.raises(ValueError, match="A.kt:1"):
        mt.apply_mutant("a == c\n", mutant)


def test_mutant_key_ignores_indentation_but_not_position_or_operator():
    a = mt.Mutant("A.kt", 3, "== -> !=", "  a == b", "  a != b")
    b = mt.Mutant("A.kt", 3, "== -> !=", "a == b", "a != b")
    c = mt.Mutant("A.kt", 4, "== -> !=", "a == b", "a != b")

    assert a.key == b.key
    assert a.key != c.key
    assert a.key.startswith("A.kt:3:== -> !=:")


def test_the_same_operator_twice_on_a_line_gives_mutants_with_different_keys():
    first, second = mt.mutants_for_line("A.kt", 5, "if (w > 0 && h > 0) go()")[:2]

    assert (first.operator, second.operator) == ("> -> <=", "> -> <=")
    assert first.mutated == "if (w <= 0 && h > 0) go()"
    assert second.mutated == "if (w > 0 && h <= 0) go()"
    assert (first.occurrence, second.occurrence) == (0, 1)
    assert first.key != second.key


def test_matches_in_separate_code_spans_are_counted_together():
    mutants = mt.mutants_for_line("A.kt", 1, 'f(a == b, "x", c == d)')

    assert [m.occurrence for m in mutants if m.operator == "== -> !="] == [0, 1]


def test_resuming_skips_only_the_recorded_one_of_two_same_line_mutants(tmp_path):
    first, second = mt.mutants_for_line("A.kt", 5, "if (w > 0 && h > 0) go()")[:2]
    path = tmp_path / "mutants.jsonl"
    path.write_text(json.dumps(mt.result_record(first, {"status": mt.KILLED})) + "\n")

    done = mt.load_done(str(path))

    assert first.key in done
    assert second.key not in done


# --- changed lines ----------------------------------------------------------------------------


def test_parse_changed_lines_reads_new_side_hunks_per_file():
    diff = "\n".join([
        "diff --git a/x/A.kt b/x/A.kt",
        "--- a/x/A.kt",
        "+++ b/x/A.kt",
        "@@ -4,0 +5,3 @@ fun f()",
        "+one",
        "@@ -10 +12 @@",
        "@@ -20,2 +21,0 @@",
        "diff --git a/x/B.kt b/x/B.kt",
        "--- a/x/B.kt",
        "+++ /dev/null",
        "@@ -1,3 +0,0 @@",
        "diff --git a/x/C.kt b/x/C.kt",
        "+++ b/x/C.kt",
        "@@ -1 +1,0 @@",
    ])

    assert mt.parse_changed_lines(diff) == {"x/A.kt": {5, 6, 7, 12}}


# --- test selection ---------------------------------------------------------------------------


def _write(root, rel, text):
    path = os.path.join(root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


MAIN = "app/src/main/java/org/sample/"


@pytest.fixture
def sample_tree(tmp_path):
    root = str(tmp_path)
    _write(root, MAIN + "Alpha.kt", "class Alpha {\n    fun ok(a: Int) = a == 1\n}\n")
    _write(root, MAIN + "Beta.kt", "internal class Beta(private val alpha: Alpha) {\n    fun run() {\n"
                                   "        alpha.ok(2)\n    }\n}\n")
    _write(root, MAIN + "Gamma.kt", "object Gamma {\n    const val ON = true\n}\n")
    _write(root, "tests/Alpha_test.kt", "class Alpha_test {\n    val a = Alpha()\n}\n")
    _write(root, "tests/Beta_test.kt", "class BetaTest {\n    val b = Beta(fake)\n}\n"
                                       "private class FakeHelper\n")
    _write(root, "tests/Other_test.kt", "class Other_test\n")
    _write(root, MAIN + "notes.txt", "Alpha")
    return root


def test_declared_names_reads_top_level_types_and_keeps_the_file_stem():
    text = "data class One(val x: Int)\nsealed interface Two\nenum class Three\nfun interface Four\n" \
           "    class Nested\nobject Five\n"

    assert mt.declared_names(text, "File") == {"One", "Two", "Three", "Four", "Five", "File"}


def test_source_index_loads_only_kotlin_sources(sample_tree):
    index = mt.SourceIndex.load(sample_tree)

    assert sorted(index.main) == [MAIN + "Alpha.kt", MAIN + "Beta.kt", MAIN + "Gamma.kt"]
    assert sorted(index.tests) == ["tests/Alpha_test.kt", "tests/Beta_test.kt", "tests/Other_test.kt"]


def test_direct_dependents_and_dependent_tests(sample_tree):
    index = mt.SourceIndex.load(sample_tree)

    assert index.direct_tests(MAIN + "Alpha.kt") == ["tests/Alpha_test.kt"]
    assert index.dependents(MAIN + "Alpha.kt") == [MAIN + "Beta.kt"]
    assert index.dependent_tests(MAIN + "Alpha.kt") == ["tests/Alpha_test.kt", "tests/Beta_test.kt"]
    assert index.direct_tests(MAIN + "Gamma.kt") == []
    assert index.dependents(MAIN + "Gamma.kt") == []


def test_test_classes_use_declared_test_class_names_not_helpers(sample_tree):
    index = mt.SourceIndex.load(sample_tree)

    assert index.test_classes(["tests/Alpha_test.kt", "tests/Beta_test.kt"]) == ["Alpha_test", "BetaTest"]


def test_test_classes_fall_back_to_the_file_stem(tmp_path):
    index = mt.SourceIndex(tests={"tests/Odd_test.kt": "fun helper() = 1\n"})

    assert index.test_classes(["tests/Odd_test.kt"]) == ["Odd_test"]


def test_plan_stages_goes_narrow_to_wide_and_drops_repeats(sample_tree):
    index = mt.SourceIndex.load(sample_tree)

    def plan(name):
        return mt.plan_stages(mt.Mutant(MAIN + name, 1, "op", "x", "y"), index)

    assert plan("Alpha.kt") == [("direct", ["Alpha_test"]), ("dependents", ["Alpha_test", "BetaTest"]),
                                ("full", [])]
    assert plan("Beta.kt") == [("direct", ["BetaTest"]), ("full", [])]
    assert plan("Gamma.kt") == [("full", [])]


# --- Gradle runs ------------------------------------------------------------------------------


@pytest.mark.parametrize("code, output, expected", [
    (0, "", mt.SURVIVED),
    (1, "e: file:///x/A.kt:3:5 Unresolved reference", mt.COMPILE_ERROR),
    (1, "Execution failed for task ':app:compileDebugKotlin'.", mt.COMPILE_ERROR),
    (1, "No tests found for given includes: [*.X_test]", mt.NO_TESTS),
    (1, "Test Alpha_test.x: Timeout has been exceeded", mt.TIMEOUT),
    (1, "Execution failed for task ':app:testDebugUnitTest'. 1 test failed", mt.KILLED),
])
def test_classify(code, output, expected):
    assert mt.classify(code, output) == expected


def test_gradle_command_filters_by_class_or_runs_everything():
    base = ["./gradlew", "testDebugUnitTest", "-PskipAssetStaging", "-q", "--offline",
            "--init-script", "t.gradle"]

    assert mt.gradle_command([], "t.gradle") == base
    assert mt.gradle_command(["A_test", "BTest"], "t.gradle") == base + ["--tests", "*.A_test",
                                                                         "--tests", "*.BTest"]


def test_init_script_sets_the_test_task_timeout():
    script = mt.INIT_SCRIPT.format(seconds=90)

    assert "tasks.withType(Test)" in script
    assert "java.time.Duration.ofSeconds(90)" in script


def _fake_gradlew(directory, body):
    path = os.path.join(directory, "gradlew")
    with open(path, "w", encoding="utf-8") as f:
        f.write("#!/bin/sh\n" + body + "\n")
    os.chmod(path, 0o755)


@pytest.mark.skipif(shutil.which("sh") is None, reason="sh not installed")
def test_run_gradle_classifies_the_process_result(tmp_path):
    _fake_gradlew(str(tmp_path), 'echo "$@" > args.txt\necho "e: broken" >&2\nexit 1')

    assert mt.run_gradle(str(tmp_path), ["A_test"], "t.gradle", 30) == mt.COMPILE_ERROR
    assert (tmp_path / "args.txt").read_text().split()[-2:] == ["--tests", "*.A_test"]


@pytest.mark.skipif(shutil.which("sh") is None, reason="sh not installed")
def test_run_gradle_reports_a_hung_run_as_timeout(tmp_path):
    _fake_gradlew(str(tmp_path), "exec sleep 5")

    assert mt.run_gradle(str(tmp_path), [], "t.gradle", 0.2) == mt.TIMEOUT


# --- scheduling -------------------------------------------------------------------------------


class _Slots:
    def __init__(self):
        self.entered = 0

    def __enter__(self):
        self.entered += 1

    def __exit__(self, *exc):
        return False


STAGES = [("direct", ["A_test"]), ("dependents", ["A_test", "B_test"]), ("full", [])]


def _evaluate(outcomes, stages=STAGES):
    calls, slots = [], _Slots()

    def run(classes):
        calls.append(classes)
        return outcomes[len(calls) - 1]

    result = mt.evaluate(mt.Mutant("A.kt", 1, "op", "x", "y"), stages, run, slots)
    return result, calls, slots.entered


def test_evaluate_stops_at_the_first_stage_that_kills():
    result, calls, full = _evaluate([mt.KILLED])

    assert (result["status"], result["stage"], result["timeout"]) == (mt.KILLED, "direct", False)
    assert calls == [["A_test"]]
    assert full == 0


def test_evaluate_widens_until_killed_and_only_the_full_stage_takes_a_slot():
    result, calls, full = _evaluate([mt.SURVIVED, mt.SURVIVED, mt.KILLED])

    assert (result["status"], result["stage"]) == (mt.KILLED, "full")
    assert calls == [["A_test"], ["A_test", "B_test"], []]
    assert full == 1


def test_evaluate_reports_survived_only_after_the_full_suite():
    result, calls, _ = _evaluate([mt.SURVIVED, mt.NO_TESTS, mt.SURVIVED])

    assert (result["status"], result["stage"]) == (mt.SURVIVED, "full")
    assert len(calls) == 3
    assert isinstance(result["secs"], float)


def test_evaluate_times_all_stages_together(monkeypatch):
    clock = iter([100.0, 107.5])
    monkeypatch.setattr(mt.time, "monotonic", lambda: next(clock))

    result, _, _ = _evaluate([mt.SURVIVED, mt.SURVIVED, mt.KILLED])

    assert result["secs"] == 7.5


def test_evaluate_counts_a_timeout_as_killed_and_keeps_compile_errors_apart():
    timeout, _, _ = _evaluate([mt.TIMEOUT])
    broken, _, _ = _evaluate([mt.COMPILE_ERROR])

    assert (timeout["status"], timeout["timeout"]) == (mt.KILLED, True)
    assert (broken["status"], broken["stage"]) == (mt.COMPILE_ERROR, "direct")


def _worktrees(tmp_path, count, text):
    trees = []
    for i in range(count):
        tree = str(tmp_path / f"w{i}")
        _write(tree, "A.kt", text)
        trees.append(tree)
    return trees


def test_run_mutants_applies_each_mutant_during_its_run_and_restores_the_file(tmp_path):
    text = "if (a == b) go()\nif (c != d) stop()\n"
    trees = _worktrees(tmp_path, 2, text)
    mutants = mt.generate_mutants("A.kt", text)
    index = mt.SourceIndex(main={"A.kt": text})
    seen, results = [], []

    def run_in(tree, classes):
        with open(os.path.join(tree, "A.kt"), encoding="utf-8") as f:
            seen.append(f.read())
        return mt.KILLED

    errors = mt.run_mutants(mutants, index, trees, run_in, 1, lambda m, r: results.append((m, r)))

    assert errors == []
    assert sorted(seen) == sorted(mt.apply_mutant(text, m) for m in mutants)
    assert sorted(m.line for m, _ in results) == [1, 2]
    assert all(r["status"] == mt.KILLED for _, r in results)
    for tree in trees:
        assert open(os.path.join(tree, "A.kt"), encoding="utf-8").read() == text


def test_run_mutants_collects_errors_and_leaves_the_file_untouched(tmp_path):
    (tree,) = _worktrees(tmp_path, 1, "if (x == y) go()\n")
    stale = mt.Mutant("A.kt", 1, "== -> !=", "if (a == b) go()", "if (a != b) go()")
    results = []

    errors = mt.run_mutants([stale], mt.SourceIndex(main={"A.kt": ""}), [tree],
                            lambda t, c: mt.KILLED, 1, lambda m, r: results.append(m))

    assert [(m, type(e)) for m, e in errors] == [(stale, ValueError)]
    assert results == []
    assert open(os.path.join(tree, "A.kt"), encoding="utf-8").read() == "if (x == y) go()\n"


def test_run_mutants_bounds_concurrent_full_suite_runs(tmp_path):
    text = "\n".join(f"if (a{i} == b) go()" for i in range(6)) + "\n"
    trees = _worktrees(tmp_path, 3, text)
    running, peak, lock = [0], [0], threading.Lock()

    def run_in(tree, classes):
        with lock:
            running[0] += 1
            peak[0] = max(peak[0], running[0])
        time.sleep(0.05)
        with lock:
            running[0] -= 1
        return mt.SURVIVED

    results = []
    mt.run_mutants(mt.generate_mutants("A.kt", text), mt.SourceIndex(main={"A.kt": text}), trees,
                   run_in, 1, lambda m, r: results.append(r))

    assert len(results) == 6
    assert peak[0] == 1


# --- results ----------------------------------------------------------------------------------


def _record(file, line, status, code="x"):
    return {"key": f"{file}:{line}", "file": file, "line": line, "operator": "== -> !=", "code": code,
            "status": status, "stage": "direct", "timeout": False, "secs": 1.0}


def test_result_record_and_load_done_round_trip(tmp_path):
    mutant = mt.Mutant("A.kt", 2, "== -> !=", "  a == b", "  a != b")
    record = mt.result_record(mutant, {"status": mt.KILLED, "stage": "direct", "timeout": False, "secs": 2})
    path = tmp_path / "mutants.jsonl"
    path.write_text(json.dumps(record) + "\n\n")

    assert record["code"] == "a == b"
    assert record["key"] == mutant.key
    assert mt.load_done(str(path)) == {mutant.key}
    assert mt.load_done(str(tmp_path / "missing.jsonl")) == set()


def test_summarize_scores_files_worst_first_and_lists_survivors():
    records = [
        _record("A.kt", 1, mt.KILLED), _record("A.kt", 2, mt.SURVIVED, "a == b"),
        _record("B.kt", 1, mt.KILLED), _record("B.kt", 2, mt.COMPILE_ERROR),
        _record("C.kt", 1, mt.COMPILE_ERROR),
    ]

    summary = mt.summarize(records)

    assert "Mutants: 5. Killed: 2. Survived: 1. Compile errors (not counted): 2." in summary
    assert "Score: 66.7%." in summary
    table = [line for line in summary.splitlines() if line.startswith("| ") and ".kt" in line]
    assert table == ["| A.kt | 1 | 1 | 50.0% |", "| B.kt | 1 | 0 | 100.0% |", "| C.kt | 0 | 0 | n/a |"]
    assert "- `A.kt:2` == -> !=: `a == b`" in summary


def test_summarize_without_survivors_or_records():
    assert "None." in mt.summarize([_record("A.kt", 1, mt.KILLED)])
    assert "Score: n/a." in mt.summarize([])


# --- git and worktrees ------------------------------------------------------------------------


@pytest.fixture
def repo(tmp_path, monkeypatch):
    for name in ("AUTHOR", "COMMITTER"):
        monkeypatch.setenv(f"GIT_{name}_NAME", "Test")
        monkeypatch.setenv(f"GIT_{name}_EMAIL", "test@example.invalid")
    monkeypatch.setenv("GIT_CONFIG_GLOBAL", os.devnull)
    monkeypatch.setenv("GIT_CONFIG_NOSYSTEM", "1")
    root = str(tmp_path / "repo")
    os.makedirs(root)
    mt.git(root, "init", "-q", "-b", "main")
    _write(root, ".gitignore", "/local.properties\napp/src/main/res/raw/*\napp/src/main/assets/rom/*\n"
                               "/build\n")
    _write(root, MAIN + "Alpha.kt", "class Alpha {\n    fun ok(a: Int) = a == 1\n}\n")
    _write(root, "tests/Alpha_test.kt", "class Alpha_test {\n    val a = Alpha()\n}\n")
    mt.git(root, "add", "-A")
    mt.git(root, "commit", "-q", "-m", "base")
    _write(root, "app/src/main/res/raw/sound.wav", "wav")
    _write(root, "app/src/main/assets/rom/game.bin", "rom")
    _write(root, "local.properties", "sdk.dir=/nowhere\n")
    return root


@needs_git
def test_uncommitted_sources_lists_changes_under_sources_and_tests_only(repo):
    assert mt.uncommitted_sources(repo) == []

    _write(repo, MAIN + "Alpha.kt", "class Alpha\n")
    _write(repo, "tests/New_test.kt", "class New_test\n")
    _write(repo, "README.md", "x")

    assert sorted(mt.uncommitted_sources(repo)) == [MAIN + "Alpha.kt", "tests/New_test.kt"]


@needs_git
def test_changed_lines_diffs_the_branch_against_its_merge_base(repo):
    mt.git(repo, "switch", "-q", "-c", "feature")
    _write(repo, MAIN + "Alpha.kt", "class Alpha {\n    fun ok(a: Int) = a != 1\n    fun no() = false\n}\n")
    _write(repo, MAIN + "notes.txt", "x")
    _write(repo, "tests/Alpha_test.kt", "class Alpha_test\n")
    mt.git(repo, "add", "-A")
    mt.git(repo, "commit", "-q", "-m", "change")

    assert mt.changed_lines(repo, "main") == {MAIN + "Alpha.kt": {2, 3}}


@needs_git
def test_build_inputs_lists_ignored_build_files_but_not_the_rom(repo):
    assert mt.build_inputs(repo) == ["app/src/main/res/raw/sound.wav", "local.properties"]


@needs_git
def test_prepare_worktrees_creates_then_resets_and_remove_cleans_up(repo, tmp_path):
    workdir = str(tmp_path / "work")
    commit = mt.git(repo, "rev-parse", "HEAD").strip()

    trees = mt.prepare_worktrees(repo, workdir, 2, commit)

    assert trees == [os.path.join(workdir, "w1"), os.path.join(workdir, "w2")]
    for tree in trees:
        assert open(os.path.join(tree, "app/src/main/res/raw/sound.wav")).read() == "wav"
        assert open(os.path.join(tree, "local.properties")).read() == "sdk.dir=/nowhere\n"
        assert not os.path.exists(os.path.join(tree, "app/src/main/assets/rom/game.bin"))

    _write(trees[0], MAIN + "Alpha.kt", "broken")
    _write(trees[0], "tests/Leftover_test.kt", "class Leftover_test\n")
    assert mt.prepare_worktrees(repo, workdir, 1, commit) == trees[:1]
    assert "a == 1" in open(os.path.join(trees[0], MAIN + "Alpha.kt")).read()
    assert not os.path.exists(os.path.join(trees[0], "tests/Leftover_test.kt"))

    mt.remove_worktrees(repo, trees)

    assert not any(os.path.exists(tree) for tree in trees)
    assert mt.git(repo, "worktree", "list").count("\n") == 1


# --- CLI --------------------------------------------------------------------------------------


@pytest.mark.parametrize("argv", [[], ["--all", "--changed"], ["--all", "A.kt"]])
def test_parse_args_needs_exactly_one_mode(argv):
    with pytest.raises(SystemExit):
        mt.parse_args(argv)


def test_parse_args_defaults():
    args = mt.parse_args(["--changed"])

    assert (args.base, args.jobs, args.full_jobs, args.timeout) == ("origin/develop", 4, 3, 600)
    assert args.report_dir == "build/reports/mutation"


@needs_git
def test_repo_root_is_the_enclosing_checkout():
    root = mt.repo_root()

    assert os.path.isfile(os.path.join(root, "tools", "mutation", "mutation_testing.py"))


def _cli(repo, tmp_path, monkeypatch, *argv, runner=None):
    monkeypatch.setattr(mt, "repo_root", lambda: repo)
    return mt.main(["--workdir", str(tmp_path / "work"), *argv],
                   runner=runner or (lambda *a: pytest.fail("runner must not be called")))


@needs_git
def test_main_list_prints_mutants_without_running(repo, tmp_path, monkeypatch, capsys):
    assert _cli(repo, tmp_path, monkeypatch, "--all", "--list") == 0

    out = capsys.readouterr().out
    assert f"{MAIN}Alpha.kt:2 == -> !=: fun ok(a: Int) = a == 1" in out
    assert "1 mutants" in out


@needs_git
def test_main_rejects_a_file_outside_the_sources(repo, tmp_path, monkeypatch):
    with pytest.raises(SystemExit, match="Not a Kotlin file"):
        _cli(repo, tmp_path, monkeypatch, os.path.join(repo, "tests/Alpha_test.kt"))


@needs_git
def test_main_refuses_uncommitted_sources(repo, tmp_path, monkeypatch, capsys):
    _write(repo, MAIN + "Alpha.kt", "class Alpha\n")

    assert _cli(repo, tmp_path, monkeypatch, "--all") == 2
    assert MAIN + "Alpha.kt" in capsys.readouterr().err


@needs_git
def test_main_runs_records_resumes_and_starts_fresh(repo, tmp_path, monkeypatch, capsys):
    calls = []

    def runner(tree, classes, init_script, timeout):
        calls.append((os.path.basename(tree), classes, timeout))
        assert open(init_script).read() == mt.INIT_SCRIPT.format(seconds=90)
        return mt.SURVIVED if classes else mt.KILLED

    target = os.path.join(repo, MAIN + "Alpha.kt")
    assert _cli(repo, tmp_path, monkeypatch, target, "--timeout", "120", runner=runner) == 0

    report = os.path.join(repo, "build/reports/mutation")
    (record,) = [json.loads(line) for line in open(os.path.join(report, "mutants.jsonl"))]
    assert (record["status"], record["stage"]) == (mt.KILLED, "full")
    assert calls == [("w1", ["Alpha_test"], 120), ("w1", [], 120)]
    assert "Killed: 1. Survived: 0." in open(os.path.join(report, "summary.md")).read()
    assert not os.path.exists(str(tmp_path / "work" / "w1"))

    calls.clear()
    assert _cli(repo, tmp_path, monkeypatch, target, runner=runner) == 0
    assert calls == []
    assert "1 mutants, 1 already done, 0 to run" in capsys.readouterr().out

    assert _cli(repo, tmp_path, monkeypatch, target, "--fresh", "--timeout", "120",
                "--keep-worktrees", runner=runner) == 0
    assert len(calls) == 2
    assert len(open(os.path.join(report, "mutants.jsonl")).readlines()) == 1
    assert os.path.isdir(str(tmp_path / "work" / "w1"))


@needs_git
def test_main_reports_mutants_that_errored(repo, tmp_path, monkeypatch, capsys):
    def runner(*args):
        raise RuntimeError("gradle exploded")

    assert _cli(repo, tmp_path, monkeypatch, "--all", runner=runner) == 0

    assert "gradle exploded" in capsys.readouterr().err


@needs_git
def test_main_changed_mode_mutates_only_branch_lines(repo, tmp_path, monkeypatch, capsys):
    mt.git(repo, "switch", "-q", "-c", "feature")
    _write(repo, MAIN + "Alpha.kt", "class Alpha {\n    fun ok(a: Int) = a == 1\n    fun no() = true\n}\n")
    mt.git(repo, "commit", "-q", "-am", "change")

    assert _cli(repo, tmp_path, monkeypatch, "--changed", "--base", "main", "--list") == 0

    out = capsys.readouterr().out
    assert "Alpha.kt:3 true -> false" in out
    assert "Alpha.kt:2" not in out


def test_script_runs_as_a_program():
    script = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                          "mutation", "mutation_testing.py")

    result = subprocess.run(["python3", script, "--help"], capture_output=True, text=True)

    assert result.returncode == 0
    assert "--changed" in result.stdout


@needs_git
def test_build_inputs_without_local_properties(repo):
    os.remove(os.path.join(repo, "local.properties"))

    assert mt.build_inputs(repo) == ["app/src/main/res/raw/sound.wav"]


@needs_git
def test_main_changed_mode_skips_a_changed_file_missing_from_the_working_tree(repo, tmp_path, monkeypatch,
                                                                              capsys):
    mt.git(repo, "switch", "-q", "-c", "feature")
    _write(repo, MAIN + "Alpha.kt", "class Alpha {\n    fun no() = true\n}\n")
    mt.git(repo, "commit", "-q", "-am", "change")
    os.remove(os.path.join(repo, MAIN + "Alpha.kt"))

    assert _cli(repo, tmp_path, monkeypatch, "--changed", "--base", "main", "--list") == 0

    assert "0 mutants" in capsys.readouterr().out
