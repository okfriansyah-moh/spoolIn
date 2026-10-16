"""Check the shared policy module's declared Kotlin dependency direction."""

from pathlib import Path
import re

root = Path(__file__).resolve().parents[3]
source = root / "libraries/backend-support/src/main/kotlin"
files = sorted(source.rglob("*.kt"))
assert files, "Shared policy source selection is empty"
for path in files:
    imported = re.findall(r"^import\s+(com\.spoolin\.[\w.]+)", path.read_text(), re.MULTILINE)
    assert all(name.startswith("com.spoolin.support.") for name in imported), path
print(f"Checked dependency direction in {len(files)} shared Kotlin source files")
