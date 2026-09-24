package io.horizontalsystems.walletkit.entities

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * Every signing gate in the app keys off [AccountType.isWatchAccountType], so what it answers
 * for each account type is the one thing worth pinning down: a type wrongly reported as
 * watch-only loses features, and one wrongly reported as signing offers actions it cannot do.
 */
class AccountTypeWatchAccountTest {

    @Test
    fun addressOnlyTypesAreWatchAccounts() {
        val addressOnly = listOf(
            AccountType.EvmAddress("0x0"),
            AccountType.SolanaAddress("solana"),
            AccountType.TronAddress("tron"),
            AccountType.TonAddress("ton"),
            AccountType.StellarAddress("stellar"),
            AccountType.XrpAddress("xrp"),
            AccountType.ThorchainAddress("thor1"),
            AccountType.MayachainAddress("maya1"),
            AccountType.BitcoinAddress("bc1", BlockchainType.Bitcoin, TokenType.Native),
            AccountType.MoneroWatchAccount("monero", "viewKey", 0),
        )

        addressOnly.forEach {
            assertTrue(it.javaClass.simpleName, it.isWatchAccountType)
        }
    }

    @Test
    fun typesHoldingKeyMaterialAreNotWatchAccounts() {
        val signing = listOf(
            AccountType.Mnemonic(List(12) { "word" }, ""),
            AccountType.MoneroMnemonic(List(25) { "word" }, ""),
            AccountType.EvmPrivateKey(BigInteger.ONE),
            AccountType.TronPrivateKey(BigInteger.ONE),
            AccountType.StellarSecretKey("secret"),
        )

        signing.forEach {
            assertFalse(it.javaClass.simpleName, it.isWatchAccountType)
        }
    }

    /** A passkey signs through the credential, so it keeps every signing entry point. */
    @Test
    fun passkeyIsNotAWatchAccount() {
        assertFalse(AccountType.Passkey("credential").isWatchAccountType)
    }

    /** The one type that is watch-only or not depending on its value rather than its class. */
    @Test
    fun extendedKeyIsAWatchAccountOnlyWhenPublic() {
        assertFalse("xprv", AccountType.HdExtendedKey(XPRV).isWatchAccountType)
        assertTrue("xpub", AccountType.HdExtendedKey(XPUB).isWatchAccountType)
    }

    companion object {
        // BIP-32 test vector 1, master key (depth 0; HDExtendedKey rejects any depth but 0 and 3)
        private const val XPRV =
            "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi"
        private const val XPUB =
            "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8"
    }
}
