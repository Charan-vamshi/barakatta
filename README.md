# Barakatta

Traditional South Indian Ludo-style board game, built as an Android APK.

## Board

- 7x7 grid, spiral path: outer ring → middle ring → inner ring → center (finish)
- Columns A–G, Rows 1–7
- Movement direction: anti-clockwise
- Center cell **D4** = finish point

### Rings

| Ring | Cells | Safe Cells | Spacing |
|------|-------|------------|---------|
| Outer | 24 | A1, A4, A7, D1, D7, G1, G4, G7 | 3 apart |
| Middle | 16 | B2, B6, F2, F6 | 4 apart |
| Inner | 8 | none | - |
| Center | D4 | Finish | - |

Safe cells (X marks) and home cells = no-kill zones.

## Players

- 2, 3, or 4 players
- Each player has 6 coins
- All coins start unlocked/active (no toss-6-to-open rule like Ludo)
- Player homes are mirrored around the board (e.g. one player's home is G4, another's is A4)

## Movement / Dice

- Number generator values: 1, 2, 3, 4, 5, 6, 12 (digital, no physical dice)
- Rolling **1, 5, 6, or 12** = bonus → roll again
- Held numbers are **not summed** — each is used individually to move a separate coin
- Killing an opponent's coin also triggers a bonus reroll
- Turn/holding ends only on a non-bonus, non-kill roll (2, 3, or 4)

## Killing

- Only happens on exact landing on an opponent's coin (not by passing over)
- Applies anywhere except safe/home cells
- Killed coin returns to its home cell
- Only one coin allowed per non-safe cell (no stacking)

## Ring Advancement

- A player must get at least **one kill** (against any opponent) before their coins can move from ring 1 → ring 2
- The same unlock also permits ring 2 → ring 3 entry (no additional kill required)
- This requirement is per-player, not shared

## Finish / Win

- Reaching center **D4** requires an exact move count
- Win condition: all 6 of a player's coins reach D4