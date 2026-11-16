# Theming & customisation

Less to customise than Zapper on purpose: in a crowd, people describe pins to each other, so everybody's map should look the same. Everything here lives in `DisplayPrefs` (`Theme.kt`).

| Setting | Options | Default |
|---|---|---|
| Theme | Auto (light sensor), Dark, Sunlight | Auto |
| Discreet mode | On, Off (also on the map's top bar and as a Quick Settings tile) | Off |
| Large pins | On, Off (`pin-size-large`) | Off |
| Haptics | Off, Light, Strong (forced Off in discreet mode) | Light |
| Nearby hazard buzz | On, Off | Off |

Not offered, and why:

- **Colour presets and dynamic colour.** Pins must look the same on every phone, and a wallpaper-derived accent could collide with a report family.
- **Pin shapes and family colours.** They are the meaning.
- **Custom report types or free-text labels.** Out of scope in the SPEC (free text is a channel for doxxing and fingerprinting).

**Auto theme.** Reads the ambient light sensor only while the app is in front (no permission needed, nothing stored). Switches to Sunlight above about 10,000 lux and back below about 3,000, after the reading holds for 3 seconds, with the `slowEffects` cross-fade. The thresholds are a starting point to tune on the reference phones.

**Discreet mode** wins over everything: theme, haptics, Large pins (pins drop to standard size so the screen shows less colour). Turning it off restores the previous settings.
