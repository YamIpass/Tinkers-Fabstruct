#!/usr/bin/env python3
"""Publishes Tinkers' Fabstruct to Modrinth through the v2 API.

The script is intentionally dependency free (urllib only) so it runs with the bundled Python.
A token is required; pass it through the MODRINTH_TOKEN environment variable or put it in
`.modrinth-token` in the repository root (that file is gitignored).

Usage:
  python tools/publish_modrinth.py plan                     # show what would happen
  python tools/publish_modrinth.py create                   # create project + upload icon
  python tools/publish_modrinth.py upload [--project ID]    # upload build/libs jar as a version

Creating a project is a one time action; afterwards keep the project id in MODRINTH_PROJECT
(or pass --project) and use the upload subcommand for every release.
"""
from __future__ import annotations

import argparse
import json
import mimetypes
import os
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
API = "https://api.modrinth.com/v2"

PROJECT_SLUG = "tinkers-fabstruct"
PROJECT_TITLE = "Tinkers' Fabstruct"
PROJECT_SUMMARY = "Unofficial Fabric 1.20.1 port of Tinkers' Construct 3.12.1 (中文名：匠魂织布)"
CATEGORIES = ["equipment", "technology", "game-mechanics"]
ADDITIONAL_CATEGORIES = ["worldgen"]
GAME_VERSIONS = ["1.20.1"]
LOADERS = ["fabric"]
FABRIC_API_PROJECT = "P7dR8mSH"


def token() -> str:
  value = os.environ.get("MODRINTH_TOKEN", "").strip()
  if value:
    return value
  token_file = ROOT / ".modrinth-token"
  if token_file.is_file():
    return token_file.read_text(encoding="utf-8").strip()
  sys.exit("missing Modrinth token: set MODRINTH_TOKEN or create .modrinth-token")


def project_id(explicit: str | None) -> str:
  if explicit:
    return explicit
  value = os.environ.get("MODRINTH_PROJECT", "").strip()
  if value:
    return value
  stored = ROOT / ".modrinth-project"
  if stored.is_file():
    return stored.read_text(encoding="utf-8").strip()
  sys.exit("missing project id: pass --project, set MODRINTH_PROJECT or create .modrinth-project")


def jar_path() -> Path:
  candidates = sorted((ROOT / "build/libs").glob("Tinkers-Fabstruct-*.jar"))
  candidates = [c for c in candidates if not any(s in c.name for s in ("-sources", "-dev", "-javadoc"))]
  if not candidates:
    sys.exit("no release jar in build/libs; run ./gradlew build first")
  return candidates[-1]


def read(name: str) -> str:
  return (ROOT / name).read_text(encoding="utf-8")


def request(method: str, path: str, *, data: bytes | None = None, content_type: str | None = None) -> dict:
  req = urllib.request.Request(API + path, data=data, method=method)
  req.add_header("Authorization", token())
  req.add_header("User-Agent", "tinkers-fabstruct-release-script/0.1 (github.com/SlimeKnights/TinkersConstruct upstream derivative)")
  if content_type:
    req.add_header("Content-Type", content_type)
  try:
    with urllib.request.urlopen(req, timeout=180) as resp:
      body = resp.read().decode("utf-8") or "{}"
      return json.loads(body)
  except urllib.error.HTTPError as error:
    detail = error.read().decode("utf-8", "replace")
    sys.exit(f"Modrinth API {error.code} on {method} {path}:\n{detail}")


def multipart(fields: dict[str, str], files: dict[str, Path]) -> tuple[bytes, str]:
  boundary = f"----fabstruct{uuid.uuid4().hex}"
  chunks: list[bytes] = []
  for key, value in fields.items():
    chunks.append(f"--{boundary}\r\nContent-Disposition: form-data; name=\"{key}\"\r\n\r\n{value}\r\n".encode())
  for key, path in files.items():
    mime = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    chunks.append(
      f"--{boundary}\r\nContent-Disposition: form-data; name=\"{key}\"; filename=\"{path.name}\"\r\n"
      f"Content-Type: {mime}\r\n\r\n".encode()
    )
    chunks.append(path.read_bytes())
    chunks.append(b"\r\n")
  chunks.append(f"--{boundary}--\r\n".encode())
  return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def do_create() -> None:
  icon = ROOT / "docs/modrinth/icon.png"
  payload = {
    "slug": PROJECT_SLUG,
    "title": PROJECT_TITLE,
    "description": PROJECT_SUMMARY,
    "body": read("docs/modrinth/description.md"),
    "categories": CATEGORIES,
    "additional_categories": ADDITIONAL_CATEGORIES,
    "project_type": "mod",
    "client_side": "required",
    "server_side": "required",
    "license_id": "MIT",
    "source_url": "https://github.com/YamIpass/Tinkers-Fabstruct",
    "issues_url": "https://github.com/YamIpass/Tinkers-Fabstruct/issues",
    "is_draft": True,
    "initial_versions": [],
    "gallery_items": [],
  }
  data, ctype = multipart({"data": json.dumps(payload)}, {"icon": icon})
  project = request("POST", "/project", data=data, content_type=ctype)
  pid = project["id"]
  (ROOT / ".modrinth-project").write_text(pid, encoding="utf-8")
  print(f"created project {pid} ({project.get('slug')}) with icon {icon.name}")


def do_upload(explicit: str | None) -> None:
  pid = project_id(explicit)
  jar = jar_path()
  payload = {
    "project_id": pid,
    "name": f"{PROJECT_TITLE} 0.1.0",
    "version_number": "0.1.0",
    "changelog": read("CHANGELOG.md"),
    "version_type": "release",
    "game_versions": GAME_VERSIONS,
    "loaders": LOADERS,
    "dependencies": [{"project_id": FABRIC_API_PROJECT, "dependency_type": "required"}],
    "featured": True,
    "status": "listed",
    "file_parts": ["file"],
    "primary_file": "file",
  }
  body, ctype = multipart({"data": json.dumps(payload)}, {"file": jar})
  version = request("POST", "/version", data=body, content_type=ctype)
  print(f"uploaded version {version.get('version_number')} ({len(jar.read_bytes())} bytes): "
        f"https://modrinth.com/mod/{PROJECT_SLUG}/version/{version.get('id')}")


def do_plan(explicit: str | None) -> None:
  jar = jar_path()
  print(f"jar        : {jar.relative_to(ROOT)} ({jar.stat().st_size} bytes)")
  print(f"slug/title : {PROJECT_SLUG} / {PROJECT_TITLE}")
  print(f"summary    : {PROJECT_SUMMARY}")
  print(f"categories : {', '.join(CATEGORIES)}")
  print(f"version    : 0.1.0 release, {GAME_VERSIONS} / {LOADERS}")
  print(f"dependency : Fabric API ({FABRIC_API_PROJECT}) required")
  print(f"project id : {explicit or os.environ.get('MODRINTH_PROJECT') or '(create first)'}")
  print(f"token      : {'set' if os.environ.get('MODRINTH_TOKEN') else ('file' if (ROOT / '.modrinth-token').is_file() else 'MISSING')}")


def main() -> None:
  parser = argparse.ArgumentParser(description=__doc__)
  parser.add_argument("command", choices=["plan", "create", "upload"])
  parser.add_argument("--project", help="Modrinth project id or slug")
  args = parser.parse_args()
  if args.command == "plan":
    do_plan(args.project)
  elif args.command == "create":
    do_create()
  else:
    do_upload(args.project)


if __name__ == "__main__":
  main()
