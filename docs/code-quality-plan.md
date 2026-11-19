# Code quality plan

A pull request is reviewed against its own diff. Several pull requests that
are each fine can still leave the app worse as a whole: the same helper
written twice, an abstraction with one user, a layer rule bent a little at a
time. This plan adds checks that look at the whole project, in three steps.

Two tools carry it:

- **Detekt** looks inside a file: complexity, long methods, swallowed
  exceptions, Compose mistakes. Its rules come ready-made.
- **Konsist** looks across files: which module imports which, how a class is
  named given what it extends, what is banned project-wide. Its rules are
  this project's own decisions, written as unit tests.

How to run them and how to treat the baseline is in the Static Analysis
section of `AGENTS.md`.

## Step 1: checks while writing a change (done)

Both checks exist and pass on the current code. Whoever makes a change runs
them before opening a pull request and fixes what they report.

- Detekt records the findings that predate it in
  `config/detekt/baseline.xml`, so only new ones fail.
- Konsist has no baseline. Its rules in `architecture-tests` hold for the
  whole project.

Nothing enforces this step yet; it relies on the author running the checks.

## Step 2: checks on every pull request

A CI workflow runs `./gradlew detekt :architecture-tests:test` on each pull
request, beside the translation check, and blocks the merge when either
fails. A new smell or a broken rule then no longer depends on the author
remembering or a reviewer noticing.

Open points:

- The job has to configure the Android modules, so it needs the Android SDK
  on the runner even though nothing is compiled.
- Whether the job is a required status check on `develop`.

## Step 3: weekly review of the whole project

A scheduled run, once a week, that looks at what was merged in the context
of the app and not of one diff.

1. **Rank hotspots.** Commits per file over the last twelve months times
   the file's complexity, from `git log` and Detekt. A messy file nobody
   touches costs nothing; a messy file edited every week costs on every pull
   request.
2. **Review the week's merges** against the areas they touched as those
   areas now stand: duplication across pull requests, abstractions with one
   user, a second pattern beside an existing one, layer drift.
3. **Report.** Each finding with its location, why it matters and the size
   of the fix. The first runs stop here, so the findings can be judged
   before anything is changed.
4. **Refactor one target.** Once the reports are trusted: the top hotspot,
   one pull request, behaviour unchanged. Tests that pin the current
   behaviour are written and merged first; code without such tests is
   reported, not refactored. Code that signs, or computes amounts or fees,
   is always report-only.

What the review leaves behind:

- **The Detekt baseline shrinks.** After a refactor, regenerate it with
  `./gradlew detektBaseline`. Its entry count over time shows whether the
  code is improving.
- **The Konsist rule set grows.** A drift the review finds twice becomes a
  rule in `architecture-tests`, and is not reviewed by hand again.

Needed before this step starts:

- A standards file naming what the review measures against, in order of
  precedence: `AGENTS.md` and recorded project decisions first, then the
  Android architecture guide, the Kotlin and coroutines conventions, the
  Compose API guidelines and OWASP MASVS where the project is silent.
- In the same file, the accepted deviations (manual dependency injection
  through `App`, for one), so they are not raised every week.
