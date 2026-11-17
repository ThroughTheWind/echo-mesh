# AGENTS.md — Echo

Canonical agent policy for this repository. Read in full before making changes.

This is the **always-on** document: facts every session needs. Anything that applies to one part of
the tree belongs in [`.ai/rules/`](.ai/rules/README.md) instead, and anything that is a multi-step
procedure belongs in a skill. Both load only when relevant, and this file has a line budget that
`node .ai/rungs.mjs check` enforces. Use `node .ai/rungs.mjs <command>` for every Rungs command;
the committed launcher pins the exact CLI version this repository has reviewed. To upgrade, choose
the next immutable version explicitly: `node .ai/rungs.mjs upgrade --to <exact-version> --apply`.

## What this is

A free, open-source (GPL-3.0) Android app that works like Waze for people at a protest: phones
nearby share short-lived, map-pinned reports with each other over Bluetooth LE and Wi-Fi
peer-to-peer, with no server and no internet. A sibling of [Zapper](https://github.com/ThroughTheWind/zapper),
built the same way: Kotlin and Compose, multi-module ([ADR-0001](docs/decisions/ADR-0001-kotlin-compose-multi-module.md));
every radio is an adapter behind the `MeshTransport` capability contract, and core and UI never
know which radio they use ([ADR-0002](docs/decisions/ADR-0002-transport-adapters.md)). People who use it
may be targeted, so **security and privacy are the product.** **The property that must not break:
the app talks to nothing but other phones running it over peer-to-peer radio, puts no stable
identifier on the air, relays nothing unauthenticated, and ships no proprietary or tracking
dependency** ([ADR-0003](docs/decisions/ADR-0003-gpl-no-servers-no-proprietary-dependencies.md)).

## Validation matrix

**Run the narrowest validation that covers what you touched**, then the standard gates. Running
everything is slow enough to get skipped; running the wrong subset proves nothing.

| Change surface | Run |
| --- | --- |
| `core/model/`, `core/mesh/`, `core/crypto/`, `adapters/loopback/` | `./gradlew :core:model:test :core:mesh:test :core:crypto:test :adapters:loopback:test` |
| a wire decoder in `core/mesh/` | the above, then `JAZZER_FUZZ=1 ./gradlew :core:mesh:test --rerun --tests '*.WireFuzzTest'` |
| `adapters/<radio>/` | `./gradlew :adapters:<radio>:test :adapters:<radio>:lint` |
| imports in `app/`, `core/` or `adapters/` | `./gradlew checkTransportBoundaries checkCryptoBoundaries checkCompressionImports` |
| a dependency, a manifest, or a permission | `./gradlew checkForbiddenDependencies checkManifestPermissions` |
| `build-logic/` (convention plugins and gate logic) | `./gradlew -p build-logic test` |
| `core/data/`, `core/map/`, `core/designsystem/`, `app/` | `./gradlew :app:assembleDebug lint` |
| any `*.kt` or `*.kts` | `./gradlew spotlessCheck` (`spotlessApply` to fix); if Spotless cannot resolve, `java -jar ktlint-cli-<ktlint>-all.jar --format` (Maven Central, the catalog's version) gives the same result |
| `gradle/libs.versions.toml`, build files | `./gradlew test :app:assembleDebug`, then CI |
| anything | `node .ai/rungs.mjs check` |

## Task loop

1. **Start from the narrowest concrete anchor** — a file, a symbol, a failing test, a route.
2. Read the scoped rule for that surface before editing broadly.
3. Make the smallest change that proves or disproves the current hypothesis.
4. Run the narrowest validation above.
5. Update docs when public behaviour or developer workflow changed.

Choosing the anchor **before** reading instructions is the point: which instructions apply is then a
consequence of scope rather than a guess.

## Non-negotiables

### When you get something wrong, harden the instruction — without being asked

**A mistake an instruction could have prevented is a defect in the instruction**, and repairing it
is part of repairing the mistake. Do it in the same change, unprompted: a reviewer reads the fix,
not the counterfactual in which you had been told the right thing first, so the repair that does not
happen here does not happen at all.

Pick the cheapest rung that actually holds:

1. **A sentence at the point of use** — where the mistake is made, not in a preamble.
2. **A line in the relevant skill**, where an agent meets the rule during execution.
3. **A path-scoped rule**, when it applies to a surface rather than a task.
4. **A gate or a hook**, when the rule has already been broken *after* being written down.

**If the rule already existed and you broke it anyway, do not restate it — make it mechanical.** A
louder sentence in a file you have already read changes nothing. `/harden-rule` walks the ladder.

### Editing files from the shell

**Never pipe a multi-line edit through `node -e "…"`, `python -c "…"`, or a shell-expanded
heredoc.** Write the script to a file and run it. Inside a double-quoted shell string backticks are
command substitution, so a `node -e` that writes documentation deletes the backticked words and
leaves grammatical text behind — and exits 0.

**Chain with `&&`, never `;`**, when a later step consumes an earlier one. A failed producer
followed by `;` yields an empty variable, and an empty variable written into a file is not a crash;
it is a wrong value that passes review.

### Claims and numbers

- **A number a machine can compute is never typed by a human.** If it can be derived, generate it
  and gate it.
- **A control that cannot fail loudly is not a control.** Filtering a command's output through
  `| tail` or `| grep` reports *that* command's exit status, not the one you care about.
- **Check the artifact, not the bookkeeping about it.** A status field, a board row, or a triage is
  a claim about the work; the branch, the test, and the file are the work.

## Conventions

- Commit messages: conventional prefixes (`feat:`, `fix:`, `docs:`, `chore:`…).
- Every version and SDK level lives in `gradle/libs.versions.toml`. Never hard-code one in a build file.
- Module build files apply a convention plugin from `build-logic/` (`conventions.jvm.library`,
  `conventions.android.library`, `conventions.android.application`), which also wires the gates.
- Package root is `app.packageRoot` in `gradle.properties`; module namespaces append the module path
  (`…echo.core.mesh`).
- Never log a key, a report, a position or a peer handle, in any build ([crypto rule](.ai/rules/crypto.md)).
- English only, no translations until the spec says otherwise.
- No dependency injection framework until the container passes about thirty bindings (ADR-0001).

## Routing

| If you need… | Go to |
| --- | --- |
| Rules for one part of the tree | [`.ai/rules/`](.ai/rules/README.md) |
| A multi-step procedure | the skills in this repo — invoke by name |
| What the app must do | `docs/SPEC.md` |
| What to build next | `docs/ROADMAP.md` and [`docs/backlog/`](docs/backlog/BACKLOG.md) |
| Who we defend against, and how | `docs/threat-model.md` |
| Radio transports | `docs/research/` |
| How a screen looks | `docs/design/` |
| Why a decision was made | [`docs/decisions/`](docs/decisions/README.md) |
| Releasing, and reporting a vulnerability | [`docs/releasing.md`](docs/releasing.md), [`SECURITY.md`](SECURITY.md) |

<!-- rungs:begin instructions@1.4.0 -->
## Repo map

<!-- rungs:begin repo-map -->
<!-- Generated. Run `node .ai/rungs.mjs render` after moving directories. -->
<!-- rungs:end repo-map -->
## Execution boundary

Before an agent command runs, name the execution unit, what filesystem/environment/credential/network
surfaces cross into it, and which controls are absent. This repository guidance declares boundaries;
it does not install a sandbox, transport, rollback, or least-privilege runtime.
<!-- rungs:end instructions -->

<!-- rungs:begin gates@1.1.0 -->
## Gates

`node .ai/rungs.mjs check` runs everything in [`.ai/gates.toml`](.ai/gates.toml) — fast tier
constantly, full tier at a boundary. **Never weaken a gate to make a change pass**; if a gate is
wrong that is its own work. Every rule you add declares `gated` or `review-only` — there is no third
option. Broke a rule that already existed? Do not restate it, make it mechanical:
**`/harden-rule`**.
<!-- rungs:end gates -->

<!-- rungs:begin backlog@1.2.0 -->
## Work tracking

Non-trivial work is a **work item** (`WI-###`) under
[`docs/backlog/`](docs/backlog/README.md) — that file is the methodology and is mandatory reading
before your first item. Branch `feature/WI-###-slug` off
`main`; claim ids from `NEXT-ID` on your own branch; never scope-creep an item.
Status must agree with git, and `node .ai/rungs.mjs check` enforces it. Execute one with **`/work-item`**.
<!-- rungs:end backlog -->

<!-- rungs:begin findings@1.0.0 -->
## Findings

Noticed something out of scope? Record it in [`docs/backlog/FINDINGS.md`](docs/backlog/FINDINGS.md) via **`/record-finding`** —
rows, not files, because recording one must cost almost nothing. A finding is the observation; a
work item is the decision. Closing one always carries a written reason, dismissals included.
<!-- rungs:end findings -->

<!-- rungs:begin adr@1.0.0 -->
## Decisions

Significant decisions are `ADR-####` records in [`docs/decisions/`](docs/decisions/README.md).
**Check the admission rule before writing one** — five criteria, all must hold, or the content
belongs in the document that already owns the topic. Records are immutable: supersede, never edit,
never delete.
<!-- rungs:end adr -->

<!-- rungs:begin session@1.1.0 -->
## Session state

Read [`.ai/session.md`](.ai/session.md) first. Treat **Active constraints — do not reopen** as binding:
settled decisions, not suggestions. Close a session with **`/close-session`**.
The handoff is narrative continuity, not a checkpoint, event stream, conversation memory, or audit
record. Events and archived notes remain inputs to accountability; they do not prove actor identity,
retention, access control, request binding, or decision reasons by themselves.
<!-- rungs:end session -->

<!-- rungs:begin rules-routing -->
## Rules for specific paths

This harness has no glob scoping, so these load only if you open them. **Read the one that
matches what you are editing before editing broadly.**

- `core/crypto/**`, `core/mesh/**` → [`.ai/rules/crypto.md`](.ai/rules/crypto.md)
- `adapters/**`, `core/mesh/**` → [`.ai/rules/transports.md`](.ai/rules/transports.md)
<!-- rungs:end rules-routing -->
