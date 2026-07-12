# Scenario Authoring Guide

How to write and edit storyline content in `app/src/main/assets/scenarios.json`.

---

## Scenario Structure

Each scenario is a JSON object in the `"scenarios"` array:

```json
{
  "id": "scenario1",
  "location": "Town Square",
  "text": "The scenario narrative the player reads.",
  "backgroundImage": "town_square",
  "isFixedBackground": false,
  "decisions": { ... },
  "statsGranted": { ... },
  "reputationChanges": { ... }
}
```

| Field | Required | Description |
|---|---|---|
| `id` | Yes | Unique identifier (e.g., `"scenario42"`). Referenced by `leadsTo`. |
| `location` | Yes | Display name shown above the narrative text. |
| `text` | Yes | The narrative paragraph. Supports dynamic tokens (see below). |
| `backgroundImage` | Yes | Drawable name without extension (e.g., `"town_square"` maps to `res/drawable/town_square.png`). Resolved by `painterForName()` in `UiUtils.kt`. |
| `isFixedBackground` | No | If `true`, background doesn't change on re-visit. Default `false`. |
| `decisions` | Yes | Up to 4 choices (see Decisions below). |
| `statsGranted` | No | Stats granted per decision position (see Stats below). |
| `reputationChanges` | No | Reputation changes per decision position (see Reputation below). |
| `isEnding` | No | Marks a final scenario: the game shows the ending screen (summary + Return to Title) instead of decisions/exploration. Every scenario whose decisions all lead back to itself must set this (enforced by `ScenarioContentIntegrityTest`). Default `false`. |

---

## Decisions

Decisions use positional keys: `"topLeft"`, `"topRight"`, `"bottomLeft"`, `"bottomRight"`. You can define 1-4 decisions.

### Simple decision (no conditions)

```json
"topLeft": {
  "text": "Help the merchant carry goods",
  "fallbackText": null,
  "condition": null,
  "leadsTo": "scenario10"
}
```

### Conditional decision (item-gated)

The player needs an item. If they don't have it, they see `fallbackText` and get routed to the `ifConditionNotMet` scenario:

```json
"bottomLeft": {
  "text": "Unlock the ancient door",
  "fallbackText": "The door is sealed. You need a key.",
  "condition": {
    "requiredItem": "holy_key",
    "removeOnUse": false
  },
  "leadsTo": {
    "ifConditionMet": "scenario45",
    "ifConditionNotMet": "scenario41"
  }
}
```

- `removeOnUse`: If `true`, the item is consumed when the player picks this choice.

### Conditional decision (stat-gated)

```json
"topRight": {
  "text": "Convince the guard to let you pass",
  "fallbackText": "You lack the charisma to persuade him",
  "condition": {
    "requiredStat": "charisma",
    "minStatValue": 3
  },
  "leadsTo": {
    "ifConditionMet": "scenario20",
    "ifConditionNotMet": "scenario18"
  }
}
```

Stats: `"strength"`, `"charisma"`, `"cunning"`, `"wisdom"` (start at 1).

### Conditional decision (reputation-gated)

```json
"bottomRight": {
  "text": "Call in a favor from the underworld",
  "fallbackText": "The underworld doesn't respect you enough",
  "condition": {
    "requiredReputation": "underworld",
    "minReputationValue": 2
  },
  "leadsTo": {
    "ifConditionMet": "scenario16",
    "ifConditionNotMet": "scenario12"
  }
}
```

Factions: `"guard"`, `"merchant"`, `"scholar"`, `"underworld"` (start at 0).

### Combined conditions (item + stat)

All conditions must be met simultaneously:

```json
"condition": {
  "requiredItem": "holy_key",
  "removeOnUse": false,
  "requiredStat": "wisdom",
  "minStatValue": 3
}
```

### Simple vs. conditional `leadsTo`

- **Simple** (string): All players go to the same scenario. `"leadsTo": "scenario10"`
- **Conditional** (object): Branches based on whether the condition is met.
  ```json
  "leadsTo": {
    "ifConditionMet": "scenario20",
    "ifConditionNotMet": "scenario18"
  }
  ```

Note: A decision can have a `condition` with a simple `leadsTo` — the condition still controls whether the player sees the normal text or fallback text, but both paths go to the same scenario.

---

## Stats Granted

Award stats when the player picks a choice. Keyed by decision position:

```json
"statsGranted": {
  "topLeft": { "wisdom": 1 },
  "topRight": { "charisma": 1 },
  "bottomLeft": { "strength": 1 },
  "bottomRight": { "cunning": 1 }
}
```

You can grant multiple stats per choice: `{ "strength": 1, "wisdom": 1 }`.

Stat and reputation grants are **one-time per (scenario, position)**: the engine records
granted decisions in the save (`PlayerProgress.grantedDecisions`), so revisiting a scenario
via map travel or a loop never re-awards them. `ScenarioContentIntegrityTest` also verifies
that every stat-gated decision has enough earnable points in the content to ever pass.

---

## Reputation Changes

Adjust faction reputation when the player picks a choice:

```json
"reputationChanges": {
  "topLeft": { "guard": 1 },
  "bottomLeft": { "underworld": 1, "guard": -1 },
  "bottomRight": { "underworld": 2, "guard": -2 }
}
```

Negative values decrease reputation. Omit positions that have no reputation effect.
Like stats, reputation changes apply only the first time a decision is taken.

---

## Revisit Variants (locations.json)

An interactive map location may declare `"revisitScenarioId"` next to its `"scenarioId"`.
The first travel to the location plays `scenarioId`; any later travel in the same save plays
the revisit variant instead of re-running the original story beat (e.g. `town_square` →
`town_square_revisit`, `scholars_retreat` → `scenario39`). Revisit scenarios must exist in
scenarios.json (enforced by `ScenarioContentIntegrityTest`).

---

## Dynamic Scenario Text

The `TextResolver` processes tokens in scenario `text` and decision `text`/`fallbackText` fields before display. Tokens let narrative text respond to the player's current state.

### Value tokens

Insert a player state value directly into text.

| Token | Resolves to | Example |
|---|---|---|
| `{item:NAME}` | Formatted item name if player has it, empty string if not | `{item:holy_key}` → `"Holy Key"` or `""` |
| `{stat:NAME}` | Numeric stat value (defaults to 0) | `{stat:strength}` → `"3"` |
| `{rep:NAME}` | Numeric reputation value (defaults to 0) | `{rep:guard}` → `"2"` |

**Example:**
```
"text": "Your strength is at {stat:strength} and wisdom at {stat:wisdom}."
```
Result: `"Your strength is at 3 and wisdom at 5."`

**Important:** `{item:NAME}` produces an empty string when the player doesn't have the item, which can leave awkward double spaces. Use conditional blocks (below) for item-dependent sentences instead.

### Conditional inline tokens (threshold comparison)

Show one of two text fragments based on whether a stat or reputation meets a threshold.

**Syntax:** `{stat:NAME:THRESHOLD:text_if_below|text_if_at_or_above}`

| Token | Condition | Example |
|---|---|---|
| `{stat:NAME:N:below\|above}` | stat < N → below, stat >= N → above | `{stat:wisdom:3:much to learn\|wise}` |
| `{rep:NAME:N:below\|above}` | rep < N → below, rep >= N → above | `{rep:guard:2:ignore you\|salute}` |

- The threshold may be **negative** — useful for reputation, which can drop below zero: `{rep:guard:-1:They draw weapons.|They let you pass.}`
- Either branch may be **empty** to say nothing on that side: `{stat:wisdom:3:|The runes make sense to you.}`
- Branch text may not contain `|` (below branch) or `}` (either branch).

**Examples:**
```
"text": "The mentor {stat:wisdom:3:senses you still have much to learn|nods, recognizing your wisdom}."
```
- Wisdom 2 → `"The mentor senses you still have much to learn."`
- Wisdom 3+ → `"The mentor nods, recognizing your wisdom."`

```
"text": "The guards {rep:guard:1:pay you no attention|nod respectfully} as you walk by."
```
- Guard rep 0 → `"The guards pay you no attention as you walk by."`
- Guard rep 1+ → `"The guards nod respectfully as you walk by."`

### Conditional blocks (item presence)

Show or hide entire sections of text based on whether the player has an item.

**Syntax:** `{if:has:ITEM}text shown if player has item{/if}`

**Negated:** `{if:!has:ITEM}text shown if player does NOT have item{/if}`

**Example:**
```
"text": "Before you stands a radiant door.{if:has:holy_key} Your Holy Key glows warmly.{/if}{if:!has:holy_key} Something is missing.{/if}"
```
- Has holy_key → `"Before you stands a radiant door. Your Holy Key glows warmly."`
- No holy_key → `"Before you stands a radiant door. Something is missing."`

**Nesting:** Tokens inside conditional blocks are resolved too:
```
"text": "{if:has:torch}Your {item:torch} lights the way.{/if}"
```
→ `"Your Torch lights the way."` (only if player has torch)

Blocks may also nest inside each other; each inner block pairs with its own `{/if}`:
```
"text": "{if:has:map}You unfold the map.{if:has:compass} The compass agrees.{/if} Onward.{/if}"
```

### Unrecognized tokens

Any `{...}` token that doesn't match a known pattern is silently stripped (with a Timber warning). This prevents raw tokens from ever appearing to the player, but also means typos in token names produce empty text rather than errors. `ScenarioTokenIntegrityTest` guards against this in CI: every token in scenarios.json must match the grammar above, use a known stat/faction name or an obtainable item, and every `{if:...}` must have its `{/if}`.

---

## Writing Tips

### Use conditional blocks for item-dependent sentences
Bad: `"Your {item:torch} lights the way."` — produces `"Your  lights the way."` without the item.
Good: `"{if:has:torch}Your torch lights the way.{/if}{if:!has:torch}It is dark.{/if}"`

### Keep conditional inline text short
The `below|above` fragments should be short phrases, not full sentences. Embed them in a surrounding sentence:
```
"The guard {rep:guard:2:eyes you suspiciously|tips his hat respectfully}."
```

### Decision text supports tokens too
Dynamic tokens work in decision `text` and `fallbackText`, not just scenario `text`:
```
"text": "Use your {item:guard_badge} to gain entry",
"fallbackText": "You need the Guard Badge to enter"
```

### Combine systems for rich narrative moments
A single scenario can use stats, reputation, items, and conditions together:
```json
{
  "id": "scenario50",
  "location": "Council Chamber",
  "text": "The council convenes. {rep:scholar:2:They barely acknowledge you.|They rise in respect as you enter.} {if:has:council_pass}Your council pass grants you a seat at the table.{/if}{if:!has:council_pass}You stand at the back, an observer only.{/if} Your charisma is at {stat:charisma}.",
  "decisions": {
    "topLeft": {
      "text": "{rep:merchant:3:Propose a trade agreement|Suggest economic reforms}",
      "condition": { "requiredReputation": "merchant", "minReputationValue": 1 },
      "fallbackText": "You lack the merchant standing to speak on trade",
      "leadsTo": { "ifConditionMet": "scenario51", "ifConditionNotMet": "scenario50" }
    }
  },
  "statsGranted": { "topLeft": { "charisma": 1 } },
  "reputationChanges": { "topLeft": { "merchant": 1, "scholar": -1 } }
}
```

---

## Quick Reference

| Pattern | Purpose | When shown |
|---|---|---|
| `{item:NAME}` | Insert item name | Only if player has it |
| `{stat:NAME}` | Insert stat number | Always (defaults to 0) |
| `{rep:NAME}` | Insert rep number | Always (defaults to 0) |
| `{stat:NAME:N:A\|B}` | Conditional on stat threshold | A if stat < N, B if stat >= N |
| `{rep:NAME:N:A\|B}` | Conditional on rep threshold | A if rep < N, B if rep >= N |
| `{if:has:NAME}...{/if}` | Block shown if item owned | Only if player has item |
| `{if:!has:NAME}...{/if}` | Block shown if item missing | Only if player lacks item |
