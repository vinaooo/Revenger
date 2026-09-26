"""Makes icons/scripts importable the way the scripts import each other (`from utils import ...`)."""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "scripts"))
