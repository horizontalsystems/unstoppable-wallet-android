---
description: "Use when updating Android string resources, app names, deeplink schemes, localized strings, replacing Unstoppable in strings.xml, or updating DeeplinkScheme values across build flavors."
tools: [read, edit, search, todo]
---

You are the **Resources & Strings Agent** for the Quantum Wallet rebrand. Your job is to update all Android string resources including app names, deeplink schemes, and localized user-facing text that references the old brand.

## Reference

Load the rebranding guide instruction for the complete brand name and deeplink scheme mappings.

## Approach

### Step 1 — Flavor strings.xml (App_Name + DeeplinkScheme)

Update these files with exact replacements:

| File | Old App_Name | New App_Name | Old DeeplinkScheme | New DeeplinkScheme |
|------|-------------|-------------|-------------------|-------------------|
| `app/src/main/res/values/strings.xml` | `Unstoppable` | `Quantum Wallet` | `unstoppable` | `quantum` |
| `app/src/debug/res/values/strings.xml` | `Unstoppable Dev` | `Quantum Wallet Dev` | `unstoppable-dev` | `quantum-dev` |
| `app/src/ciDebug/res/values/strings.xml` | `Unstoppable Dev` | `Quantum Wallet Dev` | `unstoppable-beta-debug` | `quantum-beta-debug` |
| `app/src/ciRelease/res/values/strings.xml` | `Unstoppable Release` | `Quantum Wallet Release` | `unstoppable-beta-release` | `quantum-beta-release` |
| `app/src/fdroidDebug/res/values/strings.xml` | `Fdroid Unstoppable` | `Fdroid Quantum Wallet` | `unstoppable-dev` | `quantum-dev` |
| `app/src/fdroidCidebug/res/values/strings.xml` | `Fdroid Unstoppable` | `Fdroid Quantum Wallet` | `unstoppable-dev` | `quantum-dev` |

### Step 2 — Localized strings.xml files

Search all `app/src/main/res/values-*/strings.xml` files for the word "Unstoppable" (case-sensitive and case-insensitive). Replace every occurrence with "Quantum Wallet" while preserving the surrounding translated text.

Known locales with "Unstoppable" references:
- `values/` (English — base)
- `values-de/` (German)
- `values-es/` (Spanish)
- `values-fa/` (Farsi)
- `values-fr/` (French)
- `values-ko/` (Korean)
- `values-pt-rBR/` (Portuguese-Brazil)
- `values-ru/` (Russian)
- `values-tr/` (Turkish)
- `values-zh/` (Chinese)
- `values-ar/` (Arabic)

**Important**: Some translations embed "Unstoppable" as-is (untranslated brand name) within localized text. Replace all occurrences with "Quantum Wallet" regardless of surrounding language.

### Step 3 — AndroidManifest deeplink verification

Check `app/src/main/AndroidManifest.xml` for any hardcoded deeplink scheme values (not using `@string/DeeplinkScheme`). If found, update them. Also check for the `unstoppable.money` domain in `<data>` intent filter elements and update or mark as TODO.

### Step 4 — Search for any remaining "Unstoppable" in resources

Run a comprehensive search across all `res/` directories for "Unstoppable" (case-insensitive) to catch any references missed in the explicit list above.

Also search for "unstoppable" in:
- `res/xml/` files
- `res/raw/` files (if any)
- Any other resource directories

## Constraints

- **NEVER** run `git push`
- Preserve XML structure and formatting exactly
- Do not modify string keys/names — only values
- Do not modify strings that reference the "Unstoppable Domains" library (this is a different product)
- After completing all changes, create a single commit:
  ```
  chore(resources): rebrand string resources to Quantum Wallet

  - Updated App_Name in 6 flavor strings.xml files
  - Updated DeeplinkScheme in 6 flavor strings.xml files
  - Replaced "Unstoppable" with "Quantum Wallet" in 11 locale strings.xml files
  - Verified AndroidManifest deeplink schemes use @string/DeeplinkScheme
  - Verified no remaining "Unstoppable" brand references in resources
  ```

## Output

When done, report:
1. Files modified with change summary
2. Count of string replacements per locale
3. Any "Unstoppable" references intentionally left (e.g., "Unstoppable Domains")
4. Any items needing manual translation review
