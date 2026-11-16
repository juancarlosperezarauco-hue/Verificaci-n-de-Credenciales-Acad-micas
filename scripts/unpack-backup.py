"""Extract an authenticated backup only into a new, private rehearsal directory."""
import json
import hashlib
import sys
import tarfile
from pathlib import Path, PurePosixPath

archive, target = Path(sys.argv[1]), Path(sys.argv[2]).resolve()
with tarfile.open(archive) as bundle:
    for member in bundle.getmembers():
        name = PurePosixPath(member.name)
        if (name.is_absolute() or ".." in name.parts or ":" in member.name
                or not (member.isfile() or member.isdir())):
            raise ValueError("Unsafe backup member")
    bundle.extractall(target, filter="data")
manifest = json.loads((target / "manifest.json").read_text(encoding="utf-8-sig"))
expected = {item["path"]: item["sha256"].lower() for item in manifest["files"]}
actual = {p.relative_to(target).as_posix() for p in target.rglob("*")
          if p.is_file() and p.name != "manifest.json"}
if actual != set(expected):
    raise ValueError("Backup file set differs from its manifest")
for name, digest in expected.items():
    with (target / name).open("rb") as source:
        if hashlib.file_digest(source, "sha256").hexdigest() != digest:
            raise ValueError("Backup integrity check failed")
print(f"Verified {len(expected)} files")
