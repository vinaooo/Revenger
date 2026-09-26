"""Makes tools/coverage importable (`import shell_coverage`, `import coverage_summary`)."""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "coverage"))
