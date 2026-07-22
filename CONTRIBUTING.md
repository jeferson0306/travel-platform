# Contributing

This document defines how work moves through this repository. It applies to every
change, including ones made by the maintainer.

## Branching model

- `main` — production. Always deployable. Protected: no direct commits, no
  force-push, cannot be deleted, merges only through an approved pull request
  with passing CI.
- `develop` — integration / staging. Protected the same way as `main`. All
  feature branches merge here first.
- `feature/JLJS-XXXX` — one branch per unit of work, branched from `develop`.
  `JLJS` is the internal ticket prefix used across this repository; `XXXX` is a
  zero-padded, strictly increasing sequence (`0001`, `0002`, ...). Never reuse
  or skip a number.
- `bugfix/JLJS-XXXX` — same rules as `feature/*`, used when the branch fixes a
  defect rather than adding capability.
- `hotfix/JLJS-XXXX` — branched from `main` for an urgent production fix,
  merged back into both `main` and `develop`.
- `release/x.y.z` — cut from `develop` when preparing a release; only
  stabilization fixes land here before it merges into `main` and `develop`.

## Flow

1. Branch `feature/JLJS-XXXX` off the latest `develop`.
2. Commit in small, working increments (see Commit messages below).
3. Open a pull request into `develop` using the PR template. CI must pass.
4. After review and approval, merge (do not force-push over review history).
5. Periodically, when `develop` is stable, open a release PR from `develop`
   into `main`.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/), always in
English:

```
<type>(<optional scope>): <short summary>

<optional body>
```

Types: `feat`, `fix`, `refactor`, `perf`, `docs`, `style`, `build`, `ci`,
`test`, `chore`, `revert`.

Examples:

```
feat(identity): add refresh token rotation
fix(booking): prevent double charge on retry
docs(adr): record decision to use MongoDB as primary store
```

Rules:

- One logical change per commit; the codebase should build and pass tests at
  every commit, not just at the tip of the branch.
- No secrets, credentials, tokens, personal data, or internal-only URLs in any
  commit — see [SECURITY.md](SECURITY.md).
- No mention of the tooling used to write the code in commit messages, PR
  descriptions, or comments.

## Pull requests

- Fill in every section of the PR template — summary, changes, how to test,
  breaking changes, checklist.
- A PR should be reviewable in one sitting. Split large work into a stack of
  smaller PRs against `develop` when possible.
- CI (lint, tests, coverage, security scans) must be green before merge.

## Code style

- Backend (Java/Quarkus): follow the formatting and static analysis rules
  enforced by Checkstyle/PMD/SpotBugs in the build — see
  [docs/development](docs/development).
- Frontend (React/TypeScript): follow the project's ESLint/Prettier config.
- All identifiers, comments, and log messages are in English. User-facing text
  goes through i18n (PT/EN/ES), never hardcoded strings.

## Architecture changes

Any change that introduces a new dependency, a new service boundary, or
deviates from an existing pattern needs an ADR in `docs/adr/` *before* the
implementation PR, following the template in
[docs/adr/0001-record-architecture-decisions.md](docs/adr/0001-record-architecture-decisions.md).
