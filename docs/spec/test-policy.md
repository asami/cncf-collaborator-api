# Test Policy

The authoritative executable tests are Java JUnit Jupiter tests under
`src/test/java`. The SBT build supplies the JUnit Jupiter interface, and
`sbt test` is the admitted project test entry point.

For new or materially modified tests, use `// Given`, `// When`, and `// Then`
comments when distinct setup, action, and observation phases exist; keep the
action separate from the assertions. This does not retroactively require those
comments in untouched legacy tests. Tests must be deterministic and offline: use fixed inputs and
local fakes or fixtures rather than network services, wall-clock assumptions,
or externally mutable state.

Identity and error-contract tests cover accepted and rejected identity forms,
canonical projections and release coordinates, collision admission, and
stable error codes or diagnostics. Prose records the policy but never replaces
source behavior or executable coverage.

Add a regression test in the closest existing Java test class or a focused new
class under `src/test/java`. State the observed contract with JUnit Jupiter
assertions, include Given/When/Then comments where applicable for new or
materially modified phase-distinct tests, keep its inputs
offline and deterministic, and run it through the SBT test suite.
