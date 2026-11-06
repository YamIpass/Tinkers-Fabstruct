"""Audits model/texture references so a silent missing texture cannot slip through."""
from __future__ import annotations

import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets"


_JAR_NAMES: set[str] = set()


def _load_jars() -> set[str]:
    """asset paths provided by the bundled jars (Porting Lib ships the forge: model parents)"""
    import zipfile
    names: set[str] = set()
    for jar in sorted((ROOT / "libs").glob("*.jar")):
        try:
            with zipfile.ZipFile(jar) as zf:
                names.update(zf.namelist())
        except zipfile.BadZipFile:
            continue
    return names


def split(rid: str) -> tuple[str, str]:
    # unqualified resource ids default to minecraft, exactly like ResourceLocation
    if ":" in rid:
        ns, _, path = rid.partition(":")
        return ns, path
    return "minecraft", rid


def model_path(rid: str) -> pathlib.Path:
    ns, path = split(rid)
    return ASSETS / ns / "models" / f"{path}.json"


def model_exists(rid: str) -> bool:
    ns, path = split(rid)
    if model_path(rid).exists():
        return True
    return f"assets/{ns}/models/{path}.json" in _JAR_NAMES


def texture_path(rid: str) -> pathlib.Path:
    ns, path = split(rid)
    for suffix in (".png", ".mcmeta"):
        if path.endswith(suffix):
            path = path[: -len(suffix)]
    return ASSETS / ns / "textures" / f"{path}.png"


def walk_textures(node, out: list[str]) -> None:
    if isinstance(node, dict):
        for value in node.values():
            walk_textures(value, out)
    elif isinstance(node, list):
        for value in node:
            walk_textures(value, out)
    elif isinstance(node, str):
        out.append(node)


def main() -> int:
    _JAR_NAMES.update(_load_jars())
    missing_parents: dict[str, set[str]] = {}
    missing_textures: dict[str, set[str]] = {}
    checked = 0
    for path in sorted(ASSETS.glob("*/models/**/*.json")):
        checked += 1
        try:
            data = json.loads(path.read_text(encoding="utf-8-sig"))
        except json.JSONDecodeError as exc:
            print(f"invalid json: {path.relative_to(ROOT)}: {exc}")
            continue
        parent = data.get("parent")
        if isinstance(parent, str) and split(parent)[0] == "minecraft":
            continue
        if isinstance(parent, str) and not model_exists(parent):
            missing_parents.setdefault(parent, set()).add(str(path.relative_to(ROOT)))
        raw: list[str] = []
        walk_textures(data.get("textures", {}), raw)
        for value in raw:
            if not value or value.startswith("#"):
                continue
            if split(value)[0] == "minecraft":
                continue
            if not texture_path(value).exists():
                missing_textures.setdefault(value, set()).add(str(path.relative_to(ROOT)))
    print(f"checked {checked} models")
    for ref, users in sorted(missing_parents.items()):
        print(f"missing parent {ref} used by {len(users)} model(s), e.g. {sorted(users)[0]}")
    for ref, users in sorted(missing_textures.items()):
        print(f"missing texture {ref} used by {len(users)} model(s), e.g. {sorted(users)[0]}")
    print(f"summary: {len(missing_parents)} missing parents, {len(missing_textures)} missing textures")
    return 0


if __name__ == "__main__":
    sys.exit(main())
