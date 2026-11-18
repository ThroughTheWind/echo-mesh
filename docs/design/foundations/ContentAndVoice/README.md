# Content and voice

All strings externalised (SPEC NFR-17). English for the MVP; English, French and Spanish at 1.0 (NFR-21). Write copy that survives translation: French and Spanish run about 20-30% longer, so no label may depend on fitting one line, and layouts use start and end, never left and right.

## Voice

- **Plain.** Short sentences, second person, everyday words. Name report types the way people shout them: "Tear gas", "Exit open".
- **Honest.** Say what's true and stop. Say what is protected and from whom; never "secure", "anonymous", "safe" or "verified" as absolutes. Every safety claim traces to a line in the threat model (SPEC NFR-14).
- **Calm.** No exclamation marks, no alarm words in the UI's own voice. Reports carry the urgency; the app doesn't add to it.

## Mechanics

- Sentence case for everything, buttons included.
- Buttons use the verb of what happens: "Send to Crew", "Wipe", "Leave Crew", "Withdraw report". Never "OK" or "Yes" for a destructive action.
- Numbers as digits, units with a space: "120 m", "14 min", "5 s". Times left in minutes, never seconds (seconds invite staring); under one minute, "under 1 min".
- Distances are rounded (10 m steps under 100 m, 50 m above) and directions are compass words ("north-east").
- Errors say what happened and what to do: "Radio off · Fix".
- Name the source of a report by class, never by person: "From Crew", "From a phone nearby".

## Words

| Use | Avoid | Why |
|---|---|---|
| nearby phones | users, peers, nodes | People think of phones, not accounts |
| group | channel, room, network | The SPEC's word |
| confirmed | verified | A public confirmation proves nothing |
| from a phone nearby | anonymous | It isn't necessarily anonymous |
| wipe | delete account, reset | There is no account |
| mesh | network, Bluetooth | Capabilities, not radios |
