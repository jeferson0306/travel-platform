# Prompt: run a ROADMAP milestone end to end

The standing process every milestone (M4-M17 so far) has followed. Give
this to an agent together with the milestone's ROADMAP line.

---

Execute ROADMAP milestone `[MXX]` (`[roadmap text]`) following this
repo's established flow:

1. Branch `feature/JLJS-[NNNN]` off up-to-date `develop` (next sequential
   JLJS number).
2. Read docs/context/ first; scope the milestone against what actually
   exists (prior milestones have re-scoped honestly when the ROADMAP text
   assumed things that never materialized - see ADR 0014 for the
   precedent).
3. Build the work in small verifiable steps. Anything claimed must be run
   for real: tests, full stack, measurements ("measured, not estimated").
4. Documentation is part of the milestone, not an afterthought: ADR
   (docs/prompts/new-adr.md), ROADMAP checkbox + summary, CHANGELOG
   bullet, ARCHITECTURE.md if the story changed, docs/context/ files if
   any fact they state changed.
5. Verify: full reactor `mvn -f backend/pom.xml test` when Java changed;
   the relevant real-stack checks when infra changed.
6. Commit as `[NNNN] - [Summary sentence].` (docs/context/conventions.md,
   "Git workflow"), push, open a PR against `develop` with a Summary +
   Test plan body.
7. STOP - merging requires explicit user confirmation, every time.
