---
description: "Generate a conventional commit message for current rebranding changes. Examines staged or modified files and creates a properly formatted commit with type, module scope, description, and bullet-point changelist."
agent: "agent"
tools: [execute, search, read]
---

Help me create a conventional commit for the current rebranding changes.

1. Run `git status` to see what files have been changed
2. Run `git diff --stat` to understand the scope of changes
3. Determine the best commit type (`chore`, `build`, `docs`, `refactor`, etc.) and module scope
4. Generate a commit message following this exact format:

```
type(module): one-sentence description

- Specific change 1
- Specific change 2
- etc.
```

Then run `git add -A` and `git commit -m "..."` with the generated message.

**IMPORTANT**: Do NOT run `git push`. Only commit locally.
