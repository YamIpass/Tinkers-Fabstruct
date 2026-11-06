"""Generate access widener directives for the vanilla members the ported code still cannot reach.

Reads javac errors of the form "X 在 Y 中是 private 访问控制" from a compile log, resolves the
mapped class from the Loom cache, and asks javap for the exact descriptor so the emitted
directives are valid.
"""
import re
import subprocess
import sys
import zipfile
from pathlib import Path

LOG = Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/p12.log")
AW = Path("src/main/resources/tconstruct.accesswidener")

loom = Path.home() / ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft"
jars = sorted(loom.rglob("minecraft-merged-*-v2.jar"), key=lambda p: p.stat().st_mtime, reverse=True)
if not jars:
    sys.exit("no mapped minecraft jar")
JAR = jars[0]
print("jar:", JAR.name)

# simple name -> internal path
entries = zipfile.ZipFile(JAR).namelist()
simple = {}
for n in entries:
    if n.startswith("net/minecraft/") and n.endswith(".class") and "$" not in n:
        simple.setdefault(n.rsplit("/", 1)[-1][:-6], []).append(n)

text = LOG.read_text(encoding="utf-8", errors="replace")
pattern = re.compile(r"错误: (.*?) 在 (\S+) 中是 (private|protected) 访问控制")
seen = set()
for raw_member, cls, access in pattern.findall(text):
    key = (cls, raw_member)
    if key in seen:
        continue
    seen.add(key)
    candidates = simple.get(cls)
    if not candidates:
        print("?? unknown class", cls)
        continue
    if len(candidates) > 1:
        print("?? ambiguous", cls, candidates)
        continue
    internal = candidates[0]
    binary = internal[:-6].replace("/", ".")
    member = raw_member
    is_method = "(" in member
    name = member.split("(")[0]
    javap = subprocess.run(
        ["javap", "-p", "-classpath", str(JAR), binary], capture_output=True, text=True
    )
    if javap.returncode != 0:
        print("?? javap failed", cls, javap.stderr.strip()[:120])
        continue
    desc = None
    for line in javap.stdout.splitlines():
        stripped = line.strip().rstrip(";")
        if is_method:
            if f" {name}(" not in stripped or "static" in stripped.split(" ")[0]:
                continue
            params = member[member.index("(") + 1:member.rindex(")")]
            types = [] if not params else params.split(",")
            sig_part = stripped[stripped.index("("):]
            if sig_part.replace(" ", "").split(")")[0] == "(" + ",".join(types):
                desc = descriptor_for(binary, name, javap, types, JAR)
                break
        else:
            if stripped.endswith(" " + name) or stripped.endswith("\t" + name) or re.search(r"\s" + re.escape(name) + "$", stripped):
                ftype = stripped[: -len(name)].strip().split(" ")[-1]
                desc = type_desc(ftype)
                break
    if desc is None:
        print("?? unresolved", cls, member)
        continue
    print(f"accessible {'method' if is_method else 'field'} {internal[:-6]} {name} {desc};")
