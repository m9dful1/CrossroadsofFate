/* Crossroads of Fate — exploration map editor.
 * Talks to serve.py, edits app/src/main/assets/maps.json in place.
 * Rendering mirrors ExplorationScreen.kt so what you see is what the game draws.
 */
"use strict";

const PLAYER_RADIUS = 12;   // keep in sync with ExplorationManager.PLAYER_RADIUS
const NAV_CELL = 25;        // keep in sync with ExplorationManager.NAV_CELL
const ENTITY_TYPES = ["STORY", "NPC", "EXIT"];
const ICONS = [
  "❗", "🚪", "🏠", "🏘", "🏪", "🏛", "🛡", "🌑", "🌲", "🏡", "🏚", "📚", "⛩", "💀", "🔥", "🌟",
  "🧙", "🧝‍♀️", "🧑‍🍳", "🧒", "👮", "🧕", "🧔", "🕵", "🦹", "🥷", "👻", "👼", "🔮", "👺", "🐈", "🐦‍⬛",
  "⛲", "🛏", "🪑", "📦", "🪨", "🌳", "🌿", "🌸", "💧", "🕯", "🏮", "⚱", "🎯", "🗼", "💰", "⛓",
  "🃏", "🦴", "🕳", "🥀", "🌋", "🖤", "☁", "✨", "👑", "🗿", "🌀", "⚰", "🕸", "🛢", "🌼", "🪵"
];

let data = null;             // {maps:[...]}
let scenarioLocations = [];
let mapIdx = 0;
let tool = "select";
let selection = null;        // {kind:'obstacle'|'decor'|'entity'|'spawn', index}
let dirty = false;
let undoStack = [];
let drag = null;
let paletteIcon = "🌳";
let entityCounter = 1;

const canvas = document.getElementById("canvas");
const ctx = canvas.getContext("2d");
const wrap = document.getElementById("canvasWrap");
const statusEl = document.getElementById("status");

// ---------------------------------------------------------------- utilities

const curMap = () => data.maps[mapIdx];
const snapOn = () => document.getElementById("snapChk").checked;
const snapV = (v) => (snapOn() ? Math.round(v / 5) * 5 : Math.round(v * 10) / 10);
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

function hashCode(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) h = (Math.imul(31, h) + str.charCodeAt(i)) | 0;
  return h;
}

function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a |= 0; a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function markDirty() {
  dirty = true;
  document.getElementById("saveBtn").classList.add("dirty");
  setStatus("unsaved changes", "");
}

function setStatus(text, cls) {
  statusEl.textContent = text;
  statusEl.className = cls || "";
}

function pushUndo() {
  undoStack.push(JSON.stringify({ data, mapIdx }));
  if (undoStack.length > 100) undoStack.shift();
}

function undo() {
  const prev = undoStack.pop();
  if (!prev) return;
  const parsed = JSON.parse(prev);
  data = parsed.data;
  mapIdx = Math.min(parsed.mapIdx, data.maps.length - 1);
  selection = null;
  refreshAll();
  markDirty();
}

// ---------------------------------------------------------------- transform

let view = { scale: 1, ox: 0, oy: 0 };

function layoutCanvas() {
  const dpr = window.devicePixelRatio || 1;
  const pad = 20;
  const availW = wrap.clientWidth - pad;
  const availH = wrap.clientHeight - pad;
  const m = curMap();
  const scale = Math.min(availW / m.width, availH / m.height);
  canvas.style.width = `${m.width * scale}px`;
  canvas.style.height = `${m.height * scale}px`;
  canvas.width = Math.round(m.width * scale * dpr);
  canvas.height = Math.round(m.height * scale * dpr);
  view = { scale: scale * dpr, ox: 0, oy: 0 };
}

const wx = (x) => view.ox + x * view.scale;
const wy = (y) => view.oy + y * view.scale;
const ws = (s) => s * view.scale;

function worldFromEvent(e) {
  const r = canvas.getBoundingClientRect();
  const dpr = window.devicePixelRatio || 1;
  return {
    x: ((e.clientX - r.left) * dpr * (canvas.width / (r.width * dpr))) / view.scale,
    y: ((e.clientY - r.top) * dpr * (canvas.height / (r.height * dpr))) / view.scale,
  };
}

// ---------------------------------------------------------------- rendering

function drawEmoji(icon, x, y, size) {
  ctx.font = `${size}px sans-serif`;
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(icon, x, y);
}

function drawLabel(text, x, y, size) {
  ctx.font = `${Math.max(9, size)}px sans-serif`;
  ctx.textAlign = "center";
  ctx.textBaseline = "alphabetic";
  ctx.fillStyle = "#fff";
  ctx.shadowColor = "#000";
  ctx.shadowBlur = 3;
  ctx.fillText(text, x, y);
  ctx.shadowBlur = 0;
}

function render() {
  const m = curMap();
  const t = m.theme || {};
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // ground + seeded detail dots (same look as the game)
  ctx.fillStyle = t.ground || "#6B8F5A";
  ctx.fillRect(wx(0), wy(0), ws(m.width), ws(m.height));
  const rand = mulberry32(hashCode(m.id));
  const dots = clamp(Math.floor((m.width * m.height) / 5000), 40, 220);
  ctx.fillStyle = hexWithAlpha(t.groundDetail || "#5C7C4D", 0.55);
  for (let i = 0; i < dots; i++) {
    const dx = rand() * m.width, dy = rand() * m.height, r = 1.2 + rand() * 2.2;
    ctx.beginPath();
    ctx.arc(wx(dx), wy(dy), ws(r), 0, Math.PI * 2);
    ctx.fill();
  }

  // nav-grid overlay
  ctx.strokeStyle = "rgba(255,255,255,0.05)";
  ctx.lineWidth = 1;
  for (let gx = NAV_CELL; gx < m.width; gx += NAV_CELL) {
    ctx.beginPath(); ctx.moveTo(wx(gx), wy(0)); ctx.lineTo(wx(gx), wy(m.height)); ctx.stroke();
  }
  for (let gy = NAV_CELL; gy < m.height; gy += NAV_CELL) {
    ctx.beginPath(); ctx.moveTo(wx(0), wy(gy)); ctx.lineTo(wx(m.width), wy(gy)); ctx.stroke();
  }

  // obstacles
  (m.obstacles || []).forEach((o, i) => {
    const corner = ws(Math.min(10, Math.min(o.width, o.height) / 4));
    ctx.fillStyle = t.obstacleFill || "#5B4A3A";
    roundRect(wx(o.x), wy(o.y), ws(o.width), ws(o.height), corner);
    ctx.fill();
    ctx.strokeStyle = t.obstacleStroke || "#3E3228";
    ctx.lineWidth = ws(2);
    roundRect(wx(o.x), wy(o.y), ws(o.width), ws(o.height), corner);
    ctx.stroke();
    if (o.icon) drawEmoji(o.icon, wx(o.x + o.width / 2), wy(o.y + o.height / 2), ws(Math.min(o.width, o.height) * 0.55));
    if (o.label) drawLabel(o.label, wx(o.x + o.width / 2), wy(o.y + o.height) - ws(4), ws(10));
    if (isSel("obstacle", i)) drawSelectionRect(o);
  });

  // decor
  (m.decor || []).forEach((d, i) => {
    drawEmoji(d.icon, wx(d.x), wy(d.y), ws(24 * (d.scale || 1)));
    if (isSel("decor", i)) drawSelectionRing(d.x, d.y, 18);
  });

  // entities
  (m.entities || []).forEach((e, i) => {
    const cx = wx(e.x), cy = wy(e.y);
    if (e.type === "STORY") {
      ctx.strokeStyle = "rgba(255,215,0,0.9)";
      ctx.lineWidth = ws(2);
      ctx.beginPath(); ctx.arc(cx, cy, ws(19), 0, Math.PI * 2); ctx.stroke();
    }
    ctx.fillStyle = e.type === "STORY" ? "rgba(64,48,16,0.7)" : "rgba(0,0,0,0.45)";
    ctx.beginPath(); ctx.arc(cx, cy, ws(17), 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = "rgba(255,255,255,0.75)";
    ctx.lineWidth = ws(1.5);
    ctx.beginPath(); ctx.arc(cx, cy, ws(17), 0, Math.PI * 2); ctx.stroke();
    drawEmoji(e.icon || "❓", cx, cy, ws(22));
    if (e.label) drawLabel(e.label, cx, cy + ws(30), ws(11));
    if (isSel("entity", i)) drawSelectionRing(e.x, e.y, 24);
  });

  // spawn crosshair
  const s = m.spawn;
  ctx.strokeStyle = "#6fce6f";
  ctx.lineWidth = ws(2);
  ctx.beginPath(); ctx.arc(wx(s.x), wy(s.y), ws(10), 0, Math.PI * 2); ctx.stroke();
  ctx.beginPath();
  ctx.moveTo(wx(s.x) - ws(15), wy(s.y)); ctx.lineTo(wx(s.x) + ws(15), wy(s.y));
  ctx.moveTo(wx(s.x), wy(s.y) - ws(15)); ctx.lineTo(wx(s.x), wy(s.y) + ws(15));
  ctx.stroke();
  drawLabel("spawn", wx(s.x), wy(s.y) - ws(18), ws(10));
  if (isSel("spawn", 0)) drawSelectionRing(s.x, s.y, 20);
}

function roundRect(x, y, w, h, r) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

function hexWithAlpha(hex, a) {
  const v = parseInt(hex.slice(1), 16);
  return `rgba(${(v >> 16) & 255},${(v >> 8) & 255},${v & 255},${a})`;
}

const isSel = (kind, i) => selection && selection.kind === kind && selection.index === i;

function drawSelectionRing(x, y, r) {
  ctx.strokeStyle = "#ffd700";
  ctx.setLineDash([ws(4), ws(3)]);
  ctx.lineWidth = ws(2);
  ctx.beginPath(); ctx.arc(wx(x), wy(y), ws(r), 0, Math.PI * 2); ctx.stroke();
  ctx.setLineDash([]);
}

function drawSelectionRect(o) {
  ctx.strokeStyle = "#ffd700";
  ctx.setLineDash([ws(4), ws(3)]);
  ctx.lineWidth = ws(2);
  ctx.strokeRect(wx(o.x), wy(o.y), ws(o.width), ws(o.height));
  ctx.setLineDash([]);
  ctx.fillStyle = "#ffd700";
  cornerHandles(o).forEach((h) => ctx.fillRect(wx(h.x) - 4, wy(h.y) - 4, 8, 8));
}

const cornerHandles = (o) => [
  { x: o.x, y: o.y, cx: -1, cy: -1 },
  { x: o.x + o.width, y: o.y, cx: 1, cy: -1 },
  { x: o.x, y: o.y + o.height, cx: -1, cy: 1 },
  { x: o.x + o.width, y: o.y + o.height, cx: 1, cy: 1 },
];

// ---------------------------------------------------------------- hit tests

function hitTest(p) {
  const m = curMap();
  const near = (x, y, r) => (p.x - x) ** 2 + (p.y - y) ** 2 <= r * r;
  for (let i = (m.entities || []).length - 1; i >= 0; i--) {
    if (near(m.entities[i].x, m.entities[i].y, 20)) return { kind: "entity", index: i };
  }
  for (let i = (m.decor || []).length - 1; i >= 0; i--) {
    if (near(m.decor[i].x, m.decor[i].y, 16)) return { kind: "decor", index: i };
  }
  if (near(m.spawn.x, m.spawn.y, 16)) return { kind: "spawn", index: 0 };
  for (let i = (m.obstacles || []).length - 1; i >= 0; i--) {
    const o = m.obstacles[i];
    if (p.x >= o.x && p.x <= o.x + o.width && p.y >= o.y && p.y <= o.y + o.height) {
      return { kind: "obstacle", index: i };
    }
  }
  return null;
}

function selectedObject() {
  if (!selection) return null;
  const m = curMap();
  switch (selection.kind) {
    case "obstacle": return m.obstacles[selection.index];
    case "decor": return m.decor[selection.index];
    case "entity": return m.entities[selection.index];
    case "spawn": return m.spawn;
    default: return null;
  }
}

// ---------------------------------------------------------------- pointer

canvas.addEventListener("mousedown", (e) => {
  const p = worldFromEvent(e);
  const m = curMap();

  if (tool === "select") {
    // corner-resize handles take priority when an obstacle is selected
    if (selection && selection.kind === "obstacle") {
      const o = m.obstacles[selection.index];
      const grabR = 8 / view.scale * (window.devicePixelRatio || 1);
      const h = cornerHandles(o).find((c) => Math.abs(p.x - c.x) < grabR && Math.abs(p.y - c.y) < grabR);
      if (h) {
        pushUndo();
        drag = { mode: "resize", corner: h, start: { ...o } };
        return;
      }
    }
    const hit = hitTest(p);
    selection = hit;
    if (hit) {
      pushUndo();
      const obj = selectedObject();
      drag = { mode: "move", offX: p.x - obj.x, offY: p.y - obj.y };
    }
    refreshForms();
    render();
  } else if (tool === "obstacle") {
    pushUndo();
    m.obstacles = m.obstacles || [];
    m.obstacles.push({ x: snapV(p.x), y: snapV(p.y), width: 10, height: 10, icon: null, label: null });
    selection = { kind: "obstacle", index: m.obstacles.length - 1 };
    drag = { mode: "draw", startX: snapV(p.x), startY: snapV(p.y) };
    markDirty();
  } else if (tool === "entity") {
    pushUndo();
    m.entities = m.entities || [];
    m.entities.push({
      id: nextEntityId("npc"), type: "NPC", icon: paletteIcon, label: "New NPC",
      x: snapV(p.x), y: snapV(p.y), targetMapId: null, dialog: ["..."],
    });
    selection = { kind: "entity", index: m.entities.length - 1 };
    finishPlacement();
  } else if (tool === "decor") {
    pushUndo();
    m.decor = m.decor || [];
    m.decor.push({ x: snapV(p.x), y: snapV(p.y), icon: paletteIcon, scale: 1.0 });
    selection = { kind: "decor", index: m.decor.length - 1 };
    finishPlacement();
  } else if (tool === "spawn") {
    pushUndo();
    m.spawn.x = snapV(clamp(p.x, 0, m.width));
    m.spawn.y = snapV(clamp(p.y, 0, m.height));
    selection = { kind: "spawn", index: 0 };
    finishPlacement();
  }
});

function nextEntityId(prefix) {
  const ids = new Set(data.maps.flatMap((m) => (m.entities || []).map((e) => e.id)));
  while (ids.has(`${prefix}_${entityCounter}`)) entityCounter++;
  return `${prefix}_${entityCounter}`;
}

function finishPlacement() {
  markDirty();
  refreshForms();
  render();
  validate();
}

canvas.addEventListener("mousemove", (e) => {
  if (!drag) return;
  const p = worldFromEvent(e);
  const m = curMap();

  if (drag.mode === "move") {
    const obj = selectedObject();
    const nx = snapV(clamp(p.x - drag.offX, 0, m.width));
    const ny = snapV(clamp(p.y - drag.offY, 0, m.height));
    if (nx !== obj.x || ny !== obj.y) {
      obj.x = nx;
      obj.y = ny;
      markDirty();
      render();
    }
  } else if (drag.mode === "draw") {
    const o = m.obstacles[selection.index];
    o.x = Math.min(drag.startX, snapV(p.x));
    o.y = Math.min(drag.startY, snapV(p.y));
    o.width = Math.max(10, Math.abs(snapV(p.x) - drag.startX));
    o.height = Math.max(10, Math.abs(snapV(p.y) - drag.startY));
    markDirty();
    render();
  } else if (drag.mode === "resize") {
    const o = m.obstacles[selection.index];
    const s = drag.start, c = drag.corner;
    if (c.cx < 0) { o.x = snapV(Math.min(p.x, s.x + s.width - 10)); o.width = s.x + s.width - o.x; }
    else o.width = Math.max(10, snapV(p.x) - s.x);
    if (c.cy < 0) { o.y = snapV(Math.min(p.y, s.y + s.height - 10)); o.height = s.y + s.height - o.y; }
    else o.height = Math.max(10, snapV(p.y) - s.y);
    markDirty();
    render();
  }
});

window.addEventListener("mouseup", () => {
  if (drag) {
    drag = null;
    refreshForms();
    validate();
  }
});

// ---------------------------------------------------------------- keyboard

window.addEventListener("keydown", (e) => {
  const tag = document.activeElement && document.activeElement.tagName;
  const typing = tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT";

  if ((e.ctrlKey || e.metaKey) && e.key === "s") { e.preventDefault(); save(); return; }
  if ((e.ctrlKey || e.metaKey) && e.key === "z") { e.preventDefault(); undo(); return; }
  if (typing) return;

  const tools = { v: "select", o: "obstacle", e: "entity", d: "decor", s: "spawn" };
  if (tools[e.key]) { setTool(tools[e.key]); return; }

  if ((e.key === "Delete" || e.key === "Backspace") && selection && selection.kind !== "spawn") {
    pushUndo();
    const m = curMap();
    if (selection.kind === "obstacle") m.obstacles.splice(selection.index, 1);
    if (selection.kind === "decor") m.decor.splice(selection.index, 1);
    if (selection.kind === "entity") m.entities.splice(selection.index, 1);
    selection = null;
    finishPlacement();
    return;
  }

  const nudge = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] }[e.key];
  if (nudge && selection) {
    e.preventDefault();
    const step = e.shiftKey ? 10 : 1;
    const obj = selectedObject();
    obj.x += nudge[0] * step;
    obj.y += nudge[1] * step;
    markDirty();
    refreshForms();
    render();
  }
});

// ---------------------------------------------------------------- forms

function field(labelText, inputHtml) {
  return `<label><span>${labelText}</span>${inputHtml}</label>`;
}

function refreshForms() {
  renderMapForm();
  renderSelForm();
}

function renderMapForm() {
  const m = curMap();
  const t = m.theme;
  const el = document.getElementById("mapForm");
  el.innerHTML =
    field("id", `<input type="text" id="mf_id" value="${esc(m.id)}">`) +
    field("name", `<input type="text" id="mf_name" value="${esc(m.name)}">`) +
    field("locations", `<textarea id="mf_locs" title="Scenario location strings this map covers, one per line">${esc((m.locationNames || []).join("\n"))}</textarea>`) +
    field("size", `<span><input type="number" id="mf_w" value="${m.width}" style="width:88px"> × <input type="number" id="mf_h" value="${m.height}" style="width:88px"></span>`) +
    field("ground", `<input type="color" id="mf_ground" value="${t.ground}">`) +
    field("detail", `<input type="color" id="mf_detail" value="${t.groundDetail}">`) +
    field("obstacle", `<input type="color" id="mf_ofill" value="${t.obstacleFill}">`) +
    field("outline", `<input type="color" id="mf_ostroke" value="${t.obstacleStroke}">`) +
    field("accent", `<input type="color" id="mf_accent" value="${t.accent}">`);

  const bind = (id, fn) => {
    const input = document.getElementById(id);
    input.addEventListener("focus", () => pushUndo(), { once: true });
    input.addEventListener("input", () => { fn(input.value); markDirty(); render(); validate(); });
  };
  bind("mf_id", (v) => renameMap(m.id, v.trim()));
  bind("mf_name", (v) => { m.name = v; });
  bind("mf_locs", (v) => { m.locationNames = v.split("\n").map((s) => s.trim()).filter(Boolean); });
  bind("mf_w", (v) => { m.width = +v || m.width; layoutCanvas(); });
  bind("mf_h", (v) => { m.height = +v || m.height; layoutCanvas(); });
  bind("mf_ground", (v) => { t.ground = v.toUpperCase(); });
  bind("mf_detail", (v) => { t.groundDetail = v.toUpperCase(); });
  bind("mf_ofill", (v) => { t.obstacleFill = v.toUpperCase(); });
  bind("mf_ostroke", (v) => { t.obstacleStroke = v.toUpperCase(); });
  bind("mf_accent", (v) => { t.accent = v.toUpperCase(); });
}

function renameMap(oldId, newId) {
  if (!newId || newId === oldId) return;
  curMap().id = newId;
  data.maps.forEach((m) => (m.entities || []).forEach((e) => {
    if (e.type === "EXIT" && e.targetMapId === oldId) e.targetMapId = newId;
  }));
  refreshMapSelect();
}

function renderSelForm() {
  const el = document.getElementById("selForm");
  const obj = selectedObject();
  if (!obj) {
    el.innerHTML = `<p class="hint">Nothing selected. Click an item on the canvas, or pick a tool and place something.</p>`;
    return;
  }
  const k = selection.kind;
  let html = `<p class="hint">${k}</p>`;
  if (k === "entity") {
    html +=
      field("id", `<input type="text" id="sf_id" value="${esc(obj.id)}">`) +
      field("type", `<select id="sf_type">${ENTITY_TYPES.map((t) => `<option ${t === obj.type ? "selected" : ""}>${t}</option>`).join("")}</select>`) +
      field("icon", `<input type="text" id="sf_icon" value="${esc(obj.icon)}">`) +
      field("label", `<input type="text" id="sf_label" value="${esc(obj.label)}">`) +
      field("x / y", `<span><input type="number" id="sf_x" value="${obj.x}" style="width:88px"> <input type="number" id="sf_y" value="${obj.y}" style="width:88px"></span>`);
    if (obj.type === "EXIT") {
      const opts = data.maps.filter((m) => m.id !== curMap().id)
        .map((m) => `<option ${m.id === obj.targetMapId ? "selected" : ""}>${m.id}</option>`).join("");
      html += field("target", `<select id="sf_target"><option value="">—</option>${opts}</select>`);
    }
    if (obj.type === "NPC") {
      html += field("dialog", `<textarea id="sf_dialog" title="One line per row">${esc((obj.dialog || []).join("\n"))}</textarea>`);
    }
  } else if (k === "obstacle") {
    html +=
      field("icon", `<input type="text" id="sf_icon" value="${esc(obj.icon || "")}">`) +
      field("label", `<input type="text" id="sf_label" value="${esc(obj.label || "")}">`) +
      field("x / y", `<span><input type="number" id="sf_x" value="${obj.x}" style="width:88px"> <input type="number" id="sf_y" value="${obj.y}" style="width:88px"></span>`) +
      field("w / h", `<span><input type="number" id="sf_w" value="${obj.width}" style="width:88px"> <input type="number" id="sf_h" value="${obj.height}" style="width:88px"></span>`);
  } else if (k === "decor") {
    html +=
      field("icon", `<input type="text" id="sf_icon" value="${esc(obj.icon)}">`) +
      field("scale", `<input type="number" id="sf_scale" step="0.1" value="${obj.scale || 1}">`) +
      field("x / y", `<span><input type="number" id="sf_x" value="${obj.x}" style="width:88px"> <input type="number" id="sf_y" value="${obj.y}" style="width:88px"></span>`);
  } else if (k === "spawn") {
    html += field("x / y", `<span><input type="number" id="sf_x" value="${obj.x}" style="width:88px"> <input type="number" id="sf_y" value="${obj.y}" style="width:88px"></span>`);
  }
  if (k !== "spawn") html += `<div class="row"><button id="sf_delete">🗑 Delete ${k}</button></div>`;
  el.innerHTML = html;

  const bind = (id, fn) => {
    const input = document.getElementById(id);
    if (!input) return;
    input.addEventListener("focus", () => pushUndo(), { once: true });
    input.addEventListener("input", () => { fn(input.value); markDirty(); render(); validate(); });
  };
  bind("sf_id", (v) => { obj.id = v.trim(); });
  bind("sf_icon", (v) => { obj.icon = v; });
  bind("sf_label", (v) => { obj.label = v; });
  bind("sf_x", (v) => { obj.x = +v || 0; });
  bind("sf_y", (v) => { obj.y = +v || 0; });
  bind("sf_w", (v) => { obj.width = Math.max(10, +v || 10); });
  bind("sf_h", (v) => { obj.height = Math.max(10, +v || 10); });
  bind("sf_scale", (v) => { obj.scale = +v || 1; });
  bind("sf_target", (v) => { obj.targetMapId = v || null; });
  bind("sf_dialog", (v) => { obj.dialog = v.split("\n").filter((s) => s.length); });
  const typeSel = document.getElementById("sf_type");
  if (typeSel) typeSel.addEventListener("change", () => {
    pushUndo();
    obj.type = typeSel.value;
    if (obj.type !== "EXIT") obj.targetMapId = null;
    if (obj.type !== "NPC") obj.dialog = null;
    markDirty(); refreshForms(); render(); validate();
  });
  const del = document.getElementById("sf_delete");
  if (del) del.addEventListener("click", () => {
    pushUndo();
    const m = curMap();
    if (k === "obstacle") m.obstacles.splice(selection.index, 1);
    if (k === "decor") m.decor.splice(selection.index, 1);
    if (k === "entity") m.entities.splice(selection.index, 1);
    selection = null;
    finishPlacement();
  });
}

function esc(s) {
  return String(s == null ? "" : s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/"/g, "&quot;");
}

// ---------------------------------------------------------------- validation

function validate() {
  const problems = [];
  const ids = new Set(data.maps.map((m) => m.id));
  const claimed = new Map();

  data.maps.forEach((m) => {
    const stories = (m.entities || []).filter((e) => e.type === "STORY");
    if (stories.length !== 1) problems.push({ err: true, msg: `${m.id}: needs exactly 1 STORY entity (has ${stories.length})` });

    (m.locationNames || []).forEach((loc) => {
      const other = claimed.get(loc.toLowerCase());
      if (other) problems.push({ err: true, msg: `'${loc}' claimed by both ${other} and ${m.id}` });
      claimed.set(loc.toLowerCase(), m.id);
    });

    (m.entities || []).forEach((e) => {
      if (e.type === "EXIT") {
        if (!e.targetMapId || !ids.has(e.targetMapId)) {
          problems.push({ err: true, msg: `${m.id}/${e.id}: exit target '${e.targetMapId || "—"}' missing` });
        } else {
          const target = data.maps.find((x) => x.id === e.targetMapId);
          const back = (target.entities || []).some((x) => x.type === "EXIT" && x.targetMapId === m.id);
          if (!back) problems.push({ err: false, msg: `${m.id} → ${e.targetMapId}: no exit back` });
        }
      }
    });

    // reachability on the game's nav grid
    const grid = buildGrid(m);
    const reach = floodFill(m, grid, m.spawn);
    const points = [["spawn", m.spawn.x, m.spawn.y]]
      .concat((m.entities || []).map((e) => [e.id, e.x, e.y]));
    points.forEach(([id, x, y]) => {
      if (x < 0 || y < 0 || x > m.width || y > m.height) {
        problems.push({ err: true, msg: `${m.id}/${id}: out of bounds` });
        return;
      }
      const c = cellOf(m, x, y);
      if (grid[c.cy][c.cx]) problems.push({ err: true, msg: `${m.id}/${id}: inside an obstacle` });
      else if (!reach.has(c.cy * 1000 + c.cx)) problems.push({ err: true, msg: `${m.id}/${id}: unreachable from spawn` });
    });
  });

  scenarioLocations.forEach((loc) => {
    if (!claimed.has(loc.toLowerCase())) problems.push({ err: false, msg: `scenario location '${loc}' has no map` });
  });

  const ul = document.getElementById("problems");
  ul.innerHTML = problems.length
    ? problems.map((p) => `<li class="${p.err ? "err" : ""}">${esc(p.msg)}</li>`).join("")
    : `<li class="ok">✓ All checks pass</li>`;
  return problems;
}

function buildGrid(m) {
  const cols = Math.max(1, Math.floor(m.width / NAV_CELL));
  const rows = Math.max(1, Math.floor(m.height / NAV_CELL));
  const grid = [];
  for (let cy = 0; cy < rows; cy++) {
    grid.push([]);
    for (let cx = 0; cx < cols; cx++) {
      const px = (cx + 0.5) * NAV_CELL, py = (cy + 0.5) * NAV_CELL;
      grid[cy].push((m.obstacles || []).some((o) =>
        px >= o.x - PLAYER_RADIUS && px <= o.x + o.width + PLAYER_RADIUS &&
        py >= o.y - PLAYER_RADIUS && py <= o.y + o.height + PLAYER_RADIUS));
    }
  }
  return grid;
}

function cellOf(m, x, y) {
  const cols = Math.max(1, Math.floor(m.width / NAV_CELL));
  const rows = Math.max(1, Math.floor(m.height / NAV_CELL));
  return { cx: clamp(Math.floor(x / NAV_CELL), 0, cols - 1), cy: clamp(Math.floor(y / NAV_CELL), 0, rows - 1) };
}

function floodFill(m, grid, from) {
  const seen = new Set();
  const start = cellOf(m, from.x, from.y);
  const queue = [start];
  seen.add(start.cy * 1000 + start.cx);
  while (queue.length) {
    const { cx, cy } = queue.shift();
    [[1, 0], [-1, 0], [0, 1], [0, -1]].forEach(([dx, dy]) => {
      const nx = cx + dx, ny = cy + dy;
      if (ny < 0 || ny >= grid.length || nx < 0 || nx >= grid[0].length) return;
      const key = ny * 1000 + nx;
      if (!seen.has(key) && !grid[ny][nx]) { seen.add(key); queue.push({ cx: nx, cy: ny }); }
    });
  }
  return seen;
}

// ---------------------------------------------------------------- map mgmt

function refreshMapSelect() {
  const sel = document.getElementById("mapSelect");
  sel.innerHTML = data.maps.map((m, i) =>
    `<option value="${i}" ${i === mapIdx ? "selected" : ""}>${esc(m.name)} (${esc(m.id)})</option>`).join("");
}

document.getElementById("mapSelect").addEventListener("change", (e) => {
  mapIdx = +e.target.value;
  selection = null;
  refreshAll();
});

document.getElementById("addMapBtn").addEventListener("click", () => {
  const id = prompt("New map id (snake_case):", "new_place");
  if (!id) return;
  if (data.maps.some((m) => m.id === id)) { alert("That id already exists."); return; }
  pushUndo();
  data.maps.push({
    id, name: id.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase()),
    locationNames: [], width: 1000, height: 600,
    theme: { ground: "#6B8F5A", groundDetail: "#5C7C4D", obstacleFill: "#5B4A3A", obstacleStroke: "#3E3228", accent: "#FFD700" },
    spawn: { x: 500, y: 520 },
    obstacles: [], decor: [],
    entities: [{ id: "story", type: "STORY", icon: "❗", label: "Continue the story", x: 500, y: 300, targetMapId: null, dialog: null }],
  });
  mapIdx = data.maps.length - 1;
  selection = null;
  markDirty();
  refreshAll();
});

document.getElementById("dupMapBtn").addEventListener("click", () => {
  const src = curMap();
  const id = prompt("Duplicate as id:", `${src.id}_copy`);
  if (!id || data.maps.some((m) => m.id === id)) return;
  pushUndo();
  const copy = JSON.parse(JSON.stringify(src));
  copy.id = id;
  copy.name = `${src.name} Copy`;
  copy.locationNames = [];
  data.maps.push(copy);
  mapIdx = data.maps.length - 1;
  markDirty();
  refreshAll();
});

document.getElementById("delMapBtn").addEventListener("click", () => {
  if (data.maps.length <= 1) { alert("Can't delete the last map."); return; }
  const m = curMap();
  const referencing = data.maps.filter((x) => x !== m &&
    (x.entities || []).some((e) => e.type === "EXIT" && e.targetMapId === m.id)).map((x) => x.id);
  const extra = referencing.length ? `\nExits from: ${referencing.join(", ")} will dangle.` : "";
  if (!confirm(`Delete map '${m.id}'?${extra}`)) return;
  pushUndo();
  data.maps.splice(mapIdx, 1);
  mapIdx = Math.max(0, mapIdx - 1);
  selection = null;
  markDirty();
  refreshAll();
});

// ---------------------------------------------------------------- toolbar

function setTool(t) {
  tool = t;
  document.querySelectorAll("#tools .tool").forEach((b) =>
    b.classList.toggle("active", b.dataset.tool === t));
}

document.querySelectorAll("#tools .tool").forEach((b) =>
  b.addEventListener("click", () => setTool(b.dataset.tool)));

document.getElementById("undoBtn").addEventListener("click", undo);
document.getElementById("saveBtn").addEventListener("click", save);

async function save() {
  const problems = validate().filter((p) => p.err);
  if (problems.length && !confirm(`${problems.length} validation error(s) remain. Save anyway?`)) return;
  try {
    const res = await fetch("/api/maps", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(data) });
    const body = await res.json();
    if (!res.ok) {
      setStatus(`save failed: ${(body.problems || [body.error]).join("; ")}`, "err");
      return;
    }
    dirty = false;
    document.getElementById("saveBtn").classList.remove("dirty");
    setStatus(`saved ${body.maps} maps ✓`, "ok");
  } catch (err) {
    setStatus(`save failed: ${err}`, "err");
  }
}

window.addEventListener("beforeunload", (e) => { if (dirty) e.preventDefault(); });

// ---------------------------------------------------------------- palette

const paletteGrid = document.getElementById("paletteGrid");
ICONS.forEach((icon) => {
  const b = document.createElement("button");
  b.textContent = icon;
  b.title = "Set brush icon; applies to the selected item too";
  b.addEventListener("click", () => {
    paletteIcon = icon;
    document.querySelectorAll("#paletteGrid button").forEach((x) => x.classList.toggle("active", x === b));
    const obj = selectedObject();
    if (obj && "icon" in obj && selection.kind !== "spawn") {
      pushUndo();
      obj.icon = icon;
      markDirty();
      refreshForms();
      render();
    }
  });
  paletteGrid.appendChild(b);
});

// ---------------------------------------------------------------- boot

function refreshAll() {
  refreshMapSelect();
  layoutCanvas();
  refreshForms();
  render();
  validate();
}

async function boot() {
  const [mapsRes, locsRes] = await Promise.all([
    fetch("/api/maps"), fetch("/api/scenario-locations"),
  ]);
  data = await mapsRes.json();
  if (data.error) { setStatus(`load failed: ${data.error}`, "err"); return; }
  const locs = await locsRes.json();
  scenarioLocations = locs.locations || [];
  if (!data.maps || !data.maps.length) {
    data = { maps: [] };
    document.getElementById("addMapBtn").click();
  }
  refreshAll();
  setStatus("loaded", "ok");
}

window.addEventListener("resize", () => { layoutCanvas(); render(); });
boot();
