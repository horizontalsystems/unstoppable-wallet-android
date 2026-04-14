---
description: "Audit the codebase for remaining old brand references (Unstoppable, io.horizontalsystems, old deeplink schemes). Use after running rebranding agents to verify completeness."
agent: "rebrand-verify"
---

Run a full rebranding verification audit. Search the entire codebase for any remaining references to:

1. Old internal packages: `io.horizontalsystems.bankwallet`, `io.horizontalsystems.core`, `io.horizontalsystems.chartview`, `io.horizontalsystems.icons`, `io.horizontalsystems.subscriptions`
2. Old brand name: "Unstoppable" (excluding Unstoppable Domains library)
3. Old deeplink schemes: "unstoppable", "unstoppable-dev", "unstoppable-beta-*"
4. Old company name: "Horizontal Systems"
5. Old URLs: unstoppable.money, horizontalsystems.io

Output a structured report with Critical/Warning/Info categorization.
