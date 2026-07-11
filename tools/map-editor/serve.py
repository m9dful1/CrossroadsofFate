#!/usr/bin/env python3
"""Crossroads of Fate — exploration map editor server.

Serves the editor UI and reads/writes the game's real map catalog at
app/src/main/assets/maps.json (with a .bak backup on every save).

Usage:
    python3 tools/map-editor/serve.py [port]

Then open http://127.0.0.1:8765 in a browser. Stdlib only, no dependencies.
"""
import json
import os
import shutil
import sys
from http.server import HTTPServer, SimpleHTTPRequestHandler

EDITOR_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(os.path.dirname(EDITOR_DIR))
ASSETS_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets")
MAPS_PATH = os.path.join(ASSETS_DIR, "maps.json")
SCENARIOS_PATH = os.path.join(ASSETS_DIR, "scenarios.json")
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8765


class EditorHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=EDITOR_DIR, **kwargs)

    def log_message(self, fmt, *args):  # quieter console
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

    def _send_json(self, obj, status=200):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/api/maps":
            try:
                with open(MAPS_PATH, encoding="utf-8") as f:
                    self._send_json(json.load(f))
            except FileNotFoundError:
                self._send_json({"maps": []})
            except Exception as e:  # noqa: BLE001 - report any parse error to the UI
                self._send_json({"error": str(e)}, 500)
        elif self.path == "/api/scenario-locations":
            try:
                with open(SCENARIOS_PATH, encoding="utf-8") as f:
                    data = json.load(f)
                locations = sorted({s["location"] for s in data.get("scenarios", [])})
                self._send_json({"locations": locations})
            except Exception as e:  # noqa: BLE001
                self._send_json({"error": str(e)}, 500)
        else:
            super().do_GET()

    def do_POST(self):
        if self.path != "/api/maps":
            self._send_json({"error": "unknown endpoint"}, 404)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            problems = validate(payload)
            if problems:
                self._send_json({"error": "validation failed", "problems": problems}, 400)
                return
            # Backup lives beside the editor, NOT in assets/ (anything under
            # assets/ would be packaged into the APK)
            if os.path.exists(MAPS_PATH):
                shutil.copyfile(MAPS_PATH, os.path.join(EDITOR_DIR, "maps.json.bak"))
            with open(MAPS_PATH, "w", encoding="utf-8") as f:
                json.dump(payload, f, indent=2, ensure_ascii=False)
            self._send_json({"ok": True, "maps": len(payload["maps"]), "path": MAPS_PATH})
        except Exception as e:  # noqa: BLE001
            self._send_json({"error": str(e)}, 500)


def validate(payload):
    """Structural checks that must hold for the game to load the file at all.
    Content-level warnings (reachability etc.) are the UI's job."""
    problems = []
    maps = payload.get("maps")
    if not isinstance(maps, list) or not maps:
        return ["payload must contain a non-empty 'maps' list"]
    ids = set()
    for m in maps:
        mid = m.get("id") or "?"
        if mid in ids:
            problems.append(f"duplicate map id '{mid}'")
        ids.add(mid)
        for key in ("name", "width", "height", "theme", "spawn"):
            if key not in m or m[key] in (None, ""):
                problems.append(f"{mid}: missing '{key}'")
        for e in m.get("entities", []):
            if e.get("type") not in ("STORY", "NPC", "EXIT"):
                problems.append(f"{mid}/{e.get('id')}: bad entity type '{e.get('type')}'")
            if e.get("type") == "EXIT" and not e.get("targetMapId"):
                problems.append(f"{mid}/{e.get('id')}: EXIT needs targetMapId")
    for m in maps:
        for e in m.get("entities", []):
            if e.get("type") == "EXIT" and e.get("targetMapId") not in ids:
                problems.append(
                    f"{m.get('id')}/{e.get('id')}: exit targets unknown map "
                    f"'{e.get('targetMapId')}'"
                )
    return problems


if __name__ == "__main__":
    os.chdir(EDITOR_DIR)
    print(f"Map editor: http://127.0.0.1:{PORT}")
    print(f"Editing:    {MAPS_PATH}")
    HTTPServer(("127.0.0.1", PORT), EditorHandler).serve_forever()
