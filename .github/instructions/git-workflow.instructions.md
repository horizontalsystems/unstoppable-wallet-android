---
description: "Use when committing code, creating commits, using git, staging changes, or any version control operation. Covers commit message format, push restrictions, and branch conventions."
---

# Git Workflow Rules

## CRITICAL — Never Push

**NEVER** run `git push`, `git push origin`, `git push --force`, or any variant of push.
Commits are local only. The user will push manually when ready.

## CRITICAL — Never Commit to Master

**NEVER** commit directly to `master` (or `main`). All work must happen on feature branches.

### Auto-Branch Procedure

Before making **any** changes or commits, run `git branch --show-current`. If on `master` or `main`:

1. **Automatically** create and switch to a feature branch — do not ask, just do it
2. Pick the branch name based on the task type:
   - `rebrand/<scope>` — for rebranding work (e.g., `rebrand/packages`, `rebrand/build`, `rebrand/resources`)
   - `feat/<name>` — for new features
   - `fix/<name>` — for bug fixes
   - `chore/<name>` — for maintenance tasks
3. Run `git checkout -b <branch-name>` and confirm the switch before proceeding

### Branch Rules

- Do **not** run `git checkout master` or `git switch main` unless explicitly asked by the user
- Do **not** merge into `master`/`main` — the user handles merges manually
- If already on a feature branch, stay on it — do not create a new one unless the task is unrelated

## Commit Message Format

Use conventional commits with the affected module in parentheses:

```
type(module): one-sentence description

- Bullet point describing specific change
- Another specific change
- etc.
```

### Types

| Type | When to Use |
|------|-------------|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `refactor` | Code restructuring without behavior change |
| `chore` | Maintenance, dependency updates, renaming |
| `docs` | Documentation only |
| `style` | Formatting, whitespace, no code change |
| `build` | Build system, gradle, dependencies |
| `ci` | CI/CD pipeline changes |

### Module Names

Use the Gradle module or logical area affected:

| Module | Scope |
|--------|-------|
| `app` | Main application module |
| `core` | Core library module |
| `icons` | Icons component |
| `chartview` | Chart component |
| `subscriptions` | Any subscriptions module |
| `build` | Root build config, settings.gradle, libs.versions.toml |
| `docs` | README, CONTRIBUTE, RELEASE, CLAUDE.md |
| `resources` | String resources, layouts, drawables |
| `fastlane` | Fastlane metadata |
| `ci` | GitHub Actions workflows |
| `docker` | Docker build scripts |

If a change spans multiple modules, use the primary one or `app` as default.

### Examples

```
chore(app): rebrand package namespace to com.quantum.wallet

- Moved source directory io/horizontalsystems/bankwallet → com/quantum/wallet/bankwallet
- Updated all package declarations in 130+ Kotlin files
- Updated import statements referencing the old package
- Preserved external kit imports (io.horizontalsystems.bitcoinkit etc.)
```

```
build(build): update gradle namespaces for Quantum Wallet rebrand

- Changed applicationId to com.quantum.wallet.bankwallet
- Updated namespace in app, core, icons, chartview build.gradle.kts
- Updated subscriptions module namespaces
- Changed rootProject.name to "Quantum Wallet"
```

```
fix(app): resolve crash on wallet connect session timeout

- Added null check in WalletConnectService.onSessionExpired()
- Wrapped coroutine launch in try-catch for CancellationException
```

## Other Git Rules

- **Staging**: Use `git add -A` or `git add <specific-files>` before committing
- **Branches**: Always verify you are on a feature branch before committing. Create one if needed (`git checkout -b rebrand/<scope>`). Never commit to `master`/`main`.
- **Reset/Rebase**: Do not run `git reset --hard` or `git rebase` without user confirmation
- **Amend**: `git commit --amend` is allowed for the most recent unpushed commit only
