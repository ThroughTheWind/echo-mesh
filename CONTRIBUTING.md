# Contributing

Thanks for helping out. Issues and pull requests are welcome. **Security issues go through
[SECURITY.md](SECURITY.md), never a public issue.**

## Before you start

- Read the decisions in [`docs/decisions/`](docs/decisions/README.md), especially the
  [transport contract](docs/decisions/ADR-0002-transport-adapters.md) and the
  [no servers, no internet, no proprietary dependencies rule](docs/decisions/ADR-0003-gpl-no-servers-no-proprietary-dependencies.md).
- For anything larger than a small fix, open an issue first so we can agree on the approach.

## Making a change

1. Set up your machine as described in the [README](README.md).
2. Create a branch off `main`: `feature/WI-###-slug` for tracked work, or `fix/`, `chore/` or
   `docs/` otherwise. How work is tracked is explained in the [backlog guide](docs/backlog/README.md).
3. Keep radio-specific code inside its `adapters/<radio>` module, and cryptography inside
   `core/crypto`. The build fails otherwise.
4. Run the checks:

   ```bash
   ./gradlew spotlessCheck checkTransportBoundaries checkCryptoBoundaries checkForbiddenDependencies test lint
   node .ai/rungs.mjs check
   ```

5. Use conventional commit prefixes (`feat:`, `fix:`, `docs:`, `chore:`) and add a line to the
   [changelog](CHANGELOG.md) when users would notice the change.

## Dependencies

Every new dependency needs a GPL-3.0-compatible license, must make no network call of its own, and
must not be on [`gradle/forbidden-dependencies.txt`](gradle/forbidden-dependencies.txt). Say in the
pull request why it is needed and what it can see.

## License of contributions

By contributing you agree that your contribution is licensed under the [GPL-3.0](LICENSE), the
project's license.
