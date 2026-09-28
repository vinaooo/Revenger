"""Makes tools/coverage and tools/mutation importable (`import shell_coverage`,
`import coverage_summary`, `import mutation_testing`)."""
import os
import sys

_TOOLS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
for _package in ("coverage", "mutation"):
    sys.path.insert(0, os.path.join(_TOOLS, _package))
