"""Fail CI if README is accidentally replaced by workflow/config content."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
readme = (root / "README.md").read_text(encoding="utf-8")
required = (
    "# Motion Photo Studio",
    "## Fitur",
    "## Batasan penting",
    "## Menjalankan dari sumber",
    "## English",
    "assets/motion-mark-small.png",
)
if not readme.startswith(required[0] + "\n"):
    raise SystemExit("README must start with its project title, not workflow YAML")
if any(text not in readme for text in required):
    raise SystemExit("README is missing required project documentation")
if readme.startswith("name: Android") or "jobs:\n  android:" in readme:
    raise SystemExit("Workflow YAML was written into README")
for path in ("assets/motion-mark-small.png", "docs/PRD.md", "docs/DESIGN.md", "SECURITY.md"):
    if not (root / path).is_file():
        raise SystemExit(f"README references a missing file: {path}")
print("README structure and local links OK")
