# Iconography

Material Symbols **Rounded**, filled, weight 500 (Apache-2.0), bundled as vector drawables: only the glyphs listed here ship. Previews use the same glyphs from [`assets/icons.svg`](../../assets/icons.svg), built by `tools/icons.py`.

## Sizes

`icon-pin` (24) in pins, `icon-sm` (20) in chips and inline text, `icon-md` (28) in buttons, `icon-lg` (36) where a glyph must read at arm's length.

## One glyph per concept

| Concept | Glyph |
|---|---|
| Police line · Kettle · Tear gas · Arrests | `local_police` · `fence` · `foggy` · `person_alert` |
| Street closed | `block` |
| Medic · Water · Exit open · All clear | `medical_services` · `water_drop` · `door_open` · `check_circle` |
| Report (add a pin) | `add_location_alt` |
| Mesh on / off | `radar` / `sensors_off` |
| Your position · centre on me | `my_location` |
| Group · add someone · scan | `group` · `qr_code_2` · `qr_code_scanner` |
| Position sharing | `share_location` |
| Discreet mode | `visibility_off` |
| Panic wipe | `delete_forever` |
| Group (encrypted) report | `lock` |
| Public report, information | `info` |
| Warning | `warning` |

## Rules

- Icons are never the only label: every icon button and pin has a content description naming the action or the report.
- Add a glyph by adding it to `ICONS` in `tools/icons.py` and to this table; don't reuse a glyph for a second concept.
