---
description: "Run the full Quantum Wallet rebrand workflow. Delegates to specialized rebrand agents in the correct order: build config, packages, resources, metadata, then verification. Use to execute the complete rebranding pipeline or a specific phase."
agent: "agent"
tools: [read, edit, search, execute, todo]
argument-hint: "all | build | packages | resources | metadata | verify"
---

# Quantum Wallet — Full Rebrand

Execute the rebranding pipeline from Unstoppable Wallet to Quantum Wallet. Follow the [rebranding guide](../instructions/rebranding-guide.instructions.md) and [git workflow](../instructions/git-workflow.instructions.md).

## Before Starting

1. Check the current branch with `git branch --show-current`
2. If on `master`/`main`, create and switch to `rebrand/full` (or a scoped branch like `rebrand/build`)
3. Confirm the branch before making any changes

## Execution Order

Run these phases **in order**. Each phase should be committed separately.

### Phase 1 — Build Config (`@rebrand-build`)
Update Gradle build files: `namespace`, `applicationId`, `rootProject.name`, `resValue` brand strings, `buildConfigField` values, WalletConnect metadata.

### Phase 2 — Packages (`@rebrand-packages`)
Move source directories from `io/horizontalsystems/` to `com/quantum/wallet/`. Update all `package` declarations and `import` statements. Update XML references (AndroidManifest, navigation graphs, widget providers).

### Phase 3 — Resources (`@rebrand-resources`)
Update `strings.xml` app names across all flavors. Update `DeeplinkScheme` values. Update localized strings (11 locales). Verify AndroidManifest deeplink intent filters.

### Phase 4 — Metadata (`@rebrand-metadata`)
Update fastlane `title.txt` (14 locales), `full_description.txt`, `short_description.txt`. Update README.md, CONTRIBUTE.md, RELEASE.md, CLAUDE.md. Update docker scripts and CI workflows.

### Phase 5 — Verify (`@rebrand-verify`)
Run a read-only audit for any remaining old references. Output a structured report categorizing findings as Critical/Warning/Info.

## How to Run

If the user says **"all"** or gives no argument — run all 5 phases in order, committing after each phase.

If the user specifies a phase name (e.g., **"build"**, **"packages"**) — run only that phase.

After each phase:
1. Stage all changes: `git add -A`
2. Commit with a conventional message following the git-workflow instruction
3. Report what was changed before moving to the next phase

## Constraints

- **NEVER** run `git push`
- **NEVER** commit to `master`/`main`
- External blockchain kit imports (`io.horizontalsystems.bitcoinkit`, etc.) must NOT be renamed
- URLs without confirmed replacements get TODO placeholders
