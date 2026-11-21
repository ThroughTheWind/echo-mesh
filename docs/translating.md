**Authoritative for:** how Echo is translated, how to add a language, and the words each language uses for Echo's terms (extended spec F18, FR-18.2).
**Not authoritative for:** what a safety sentence may say ([threat model §7](threat-model.md)) or how copy is written in English ([Content and voice](design/foundations/ContentAndVoice/README.md)).

# Translating Echo

Echo ships in English, French and Spanish (FR-18.1). The phone's language picks one; on Android 13
and later, Settings > Apps > Echo > Language picks another for Echo alone.

## Where the words are

- `app/src/main/res/values/strings.xml` and `core/designsystem/src/main/res/values/strings.xml` are
  English, the source.
- Each language has a `values-<code>/strings.xml` beside each of them, with the same names in the
  same order. The English comments are kept, so the two files read side by side.
- `scripts/gates/locales.mjs` fails when a shipped language lacks a string, has one English does
  not, changes a string's format arguments (`%1$s`, `%2$d`: a missing one crashes the app), or gives
  a plural no `one` or `other`. It also fails on layout that says left or right instead of start and
  end (FR-18.4).

## Changing an English string

Change it in every language in the same change. If you cannot translate it, ask in the pull
request; do not copy the English in, since the gate cannot tell and the reader can.

## Adding a language

1. Copy both English `strings.xml` files to `values-<code>/` and translate every string. Keep each
   `%1$s` and `%2$d` exactly; move them where your grammar needs them.
2. Add the language to the glossary below.
3. Add it to `SHIPPED` in `scripts/gates/locales.mjs` and to `app/src/main/res/xml/locales_config.xml`.
4. Run `node scripts/gates/locales.mjs`, then the debug build in the `en-XA` pseudo-locale and in
   your language: nothing cut off, nothing left in English.
5. **The safety set is reviewed by a fluent speaker against threat model §7 before the language
   ships** (FR-18.3): onboarding, What Echo protects, the panic wipe, the join code's warning, and the
   steering warning. These are the strings whose comment cites a §7 row, plus `code_warning`. Record
   the review, who did it and when, in the language's work item. Community drafts are welcome; they
   do not ship unreviewed.

No code change is needed (FR-18.2). Report labels always show with their icon (FR-18.5), so a
report reads without words. Right-to-left languages are prepared for (start and end, mirrored
icons where the design says so) but none ships at 1.0.

## Glossary

One word per term, everywhere in a language. A term not here is translated plainly.

| English | French | Spanish | Note |
| --- | --- | --- | --- |
| Echo | Echo | Echo | Never translated. |
| mesh | maillage | malla | The phones relaying to each other. Not "réseau"/"red": that suggests the internet. |
| report | signalement | aviso | |
| to report | signaler | avisar | |
| group | groupe | grupo | |
| nickname | pseudo | apodo | |
| panic wipe / wipe | effacement d'urgence / effacer | borrado de emergencia / borrar | |
| discreet mode | mode discret | modo discreto | |
| kettle | nasse | encapsulamiento | Police containment of a crowd. |
| police line | cordon de police | cordón policial | |
| medic | médic | médico | FR "médic": street medics' own word. |
| all clear | voie libre | vía libre | |
| exit open | sortie ouverte | salida abierta | |
| street closed | rue fermée | calle cortada | |
| still there / gone (votes) | toujours là / terminé | sigue ahí / ya no | |
| probably gone | probablement terminé | probablemente terminado | |
| could be a trap | peut-être un piège | podría ser una trampa | The steering warning (FR-15.2, §7 N7). |
| relay | relayer | retransmitir | |
| nearby | à proximité | cerca | |
| mute (a member) | masquer | silenciar | Hides on this phone only. |
| field diagnostics | diagnostics de terrain | diagnóstico de campo | |

Address the reader as "vous" in French and "tú" in Spanish.

## Review record

| Language | Safety set reviewed by | Date | Against §7 rows |
| --- | --- | --- | --- |
| French | not yet | | |
| Spanish | not yet | | |
