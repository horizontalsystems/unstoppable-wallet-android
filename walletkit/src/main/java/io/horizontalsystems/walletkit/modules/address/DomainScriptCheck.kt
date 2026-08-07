package io.horizontalsystems.walletkit.modules.address

/**
 * Guards against homograph spoofing of resolved domain names.
 *
 * A resolved ENS or Unstoppable Domains name is shown to the user as the send destination, and the
 * name itself is what they read to decide the money is going to the right place. Characters from
 * different scripts can be visually identical, so "аave.eth" written with a Cyrillic "а" reads
 * exactly like "aave.eth" while resolving to an attacker's address. Normalization does not help:
 * the spoofed name is a well-formed name in its own right.
 *
 * A label that mixes scripts is the signal, since imitating an ASCII name means borrowing at least
 * one lookalike character from another script. Names written entirely in one script are left
 * alone: they are legitimate, and ENSIP-15 disallows the mixed case anyway.
 */
object DomainScriptCheck {

    /**
     * Returns true when any label of [domain] draws its letters from more than one script.
     */
    fun isMixedScript(domain: String): Boolean =
        domain.split('.').any { scriptsOf(it).size > 1 }

    private fun scriptsOf(label: String): Set<Character.UnicodeScript> {
        val scripts = mutableSetOf<Character.UnicodeScript>()
        var i = 0

        while (i < label.length) {
            val codePoint = label.codePointAt(i)
            i += Character.charCount(codePoint)

            // Digits, hyphens and emoji carry no script of their own and appear in legitimate
            // names, so only letters decide which scripts a label is written in.
            if (!Character.isLetter(codePoint)) continue

            val script = Character.UnicodeScript.of(codePoint)
            if (script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED) {
                scripts.add(script)
            }
        }

        return scripts
    }
}
