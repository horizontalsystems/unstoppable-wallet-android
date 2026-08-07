package io.horizontalsystems.walletkit.modules.address

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainScriptCheckTest {

    // --- spoofed names must be flagged ---

    @Test
    fun `flags a latin name carrying a cyrillic lookalike`() {
        // "аave.eth" — the leading "a" is U+0430 CYRILLIC SMALL LETTER A
        assertTrue(DomainScriptCheck.isMixedScript("аave.eth"))
    }

    @Test
    fun `flags a latin name carrying a greek lookalike`() {
        // "pаypal.eth" with GREEK SMALL LETTER OMICRON standing in for "o"
        assertTrue(DomainScriptCheck.isMixedScript("paypοl.eth"))
    }

    @Test
    fun `flags mixing in any label of the name`() {
        assertTrue(DomainScriptCheck.isMixedScript("safe.аave.eth"))
    }

    // --- legitimate names must not be flagged ---

    @Test
    fun `allows plain ascii names`() {
        assertFalse(DomainScriptCheck.isMixedScript("aave.eth"))
        assertFalse(DomainScriptCheck.isMixedScript("vitalik.eth"))
        assertFalse(DomainScriptCheck.isMixedScript("brad.crypto"))
    }

    @Test
    fun `allows accented latin`() {
        assertFalse(DomainScriptCheck.isMixedScript("café.eth"))
    }

    @Test
    fun `allows names written wholly in one non-latin script`() {
        assertFalse(DomainScriptCheck.isMixedScript("المال.eth")) // Arabic
        assertFalse(DomainScriptCheck.isMixedScript("中文.eth")) // Han
        assertFalse(DomainScriptCheck.isMixedScript("ааве.eth")) // all Cyrillic
    }

    @Test
    fun `allows digits and hyphens alongside letters`() {
        assertFalse(DomainScriptCheck.isMixedScript("web3-wallet-01.eth"))
    }

    @Test
    fun `allows emoji names`() {
        // emoji are not letters and carry no script, so they must not trip the check
        assertFalse(DomainScriptCheck.isMixedScript("🚀🚀.eth"))
        assertFalse(DomainScriptCheck.isMixedScript("moon🚀.eth"))
    }

    @Test
    fun `handles empty and degenerate input`() {
        assertFalse(DomainScriptCheck.isMixedScript(""))
        assertFalse(DomainScriptCheck.isMixedScript("."))
    }
}
