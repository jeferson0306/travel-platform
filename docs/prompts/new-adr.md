# Prompt: write an ADR

Use this template when a change involves a design decision worth
recording.

---

Write `docs/adr/[NNNN]-[kebab-title].md` (next sequential number) for
[the decision], matching the house style (read docs/adr/0014 and 0016 as
references):

- Header: title, `Status: Accepted`, `Date:` (today, absolute).
- `## Context` - the real constraint or problem, citing prior ADRs by
  number and ROADMAP milestone. State what existed before.
- `## Decision` - what was chosen AND the rejected alternative(s) with the
  concrete reason (this house style always names what was ruled out).
- `## Consequences` - trade-offs accepted, follow-ups deferred, anything a
  future change must know.
- Hard rule: only claims that are true of the actual code/measurements.
  "Measured, not estimated" - if a number appears, it was observed; if a
  behavior is described, it was run. Speculative content is a defect.
- Link it from the milestone's CHANGELOG bullet and ROADMAP line.
