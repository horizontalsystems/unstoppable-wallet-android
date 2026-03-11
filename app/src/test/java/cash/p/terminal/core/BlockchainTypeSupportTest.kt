package cash.p.terminal.core

import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.hdwalletkit.ExtendedKeyCoinType
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockchainTypeSupportTest {

    private fun hdExtendedKeyAccount(
        coinTypes: List<ExtendedKeyCoinType>,
        purposes: List<HDWallet.Purpose>
    ): AccountType.HdExtendedKey {
        val hdExtendedKey = mockk<HDExtendedKey> {
            every { this@mockk.coinTypes } returns coinTypes
            every { this@mockk.purposes } returns purposes
        }
        return mockk {
            every { this@mockk.hdExtendedKey } returns hdExtendedKey
        }
    }

    // --- Dash: BlockchainType.supports() ---

    @Test
    fun supports_dashWithNativeDashCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dash),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(BlockchainType.Dash.supports(account))
    }

    @Test
    fun supports_dashWithOldBitcoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Bitcoin, ExtendedKeyCoinType.Litecoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(BlockchainType.Dash.supports(account))
    }

    @Test
    fun supports_dashWithBip84Purpose_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dash),
            purposes = listOf(HDWallet.Purpose.BIP84)
        )
        assertFalse(BlockchainType.Dash.supports(account))
    }

    @Test
    fun supports_dashWithLitecoinCoinType_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Litecoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertFalse(BlockchainType.Dash.supports(account))
    }

    // --- Dogecoin: BlockchainType.supports() ---

    @Test
    fun supports_dogecoinWithNativeDogecoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dogecoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(BlockchainType.Dogecoin.supports(account))
    }

    @Test
    fun supports_dogecoinWithOldBitcoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Bitcoin, ExtendedKeyCoinType.Litecoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(BlockchainType.Dogecoin.supports(account))
    }

    @Test
    fun supports_dogecoinWithLitecoinOnlyCoinType_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Litecoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertFalse(BlockchainType.Dogecoin.supports(account))
    }

    // --- Bitcoin/Litecoin unchanged behavior ---

    @Test
    fun supports_bitcoinWithBitcoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Bitcoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(BlockchainType.Bitcoin.supports(account))
    }

    @Test
    fun supports_bitcoinWithDashCoinType_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dash),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertFalse(BlockchainType.Bitcoin.supports(account))
    }

    @Test
    fun supports_litecoinWithLitecoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Litecoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(BlockchainType.Litecoin.supports(account))
    }

    // --- BitcoinCash/ECash unchanged: only Bitcoin coin type ---

    @Test
    fun supports_bitcoinCashWithBitcoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Bitcoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(BlockchainType.BitcoinCash.supports(account))
    }

    @Test
    fun supports_bitcoinCashWithDashCoinType_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dash),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertFalse(BlockchainType.BitcoinCash.supports(account))
    }

    @Test
    fun supports_ecashWithBitcoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Bitcoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(BlockchainType.ECash.supports(account))
    }

    // --- EVM blockchains should not support extended keys ---

    @Test
    fun supports_ethereumWithBitcoinCoinType_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Bitcoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertFalse(BlockchainType.Ethereum.supports(account))
    }

    // --- Token.supports() tests ---

    private fun token(blockchainType: BlockchainType, tokenType: TokenType = TokenType.Native): Token {
        return Token(
            coin = Coin("test", "Test", "TST"),
            blockchain = Blockchain(blockchainType, "Test", null),
            type = tokenType,
            decimals = 8
        )
    }

    @Test
    fun tokenSupports_dashWithNativeDashCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dash),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(token(BlockchainType.Dash).supports(account))
    }

    @Test
    fun tokenSupports_dashWithOldBitcoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Bitcoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertTrue(token(BlockchainType.Dash).supports(account))
    }

    @Test
    fun tokenSupports_dashWithBip84Purpose_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dash),
            purposes = listOf(HDWallet.Purpose.BIP84)
        )
        assertFalse(token(BlockchainType.Dash).supports(account))
    }

    @Test
    fun tokenSupports_bitcoinCashWithDashCoinType_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dash),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertFalse(token(BlockchainType.BitcoinCash).supports(account))
    }

    @Test
    fun tokenSupports_ecashWithDashCoinType_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dash),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        assertFalse(token(BlockchainType.ECash).supports(account))
    }

    @Test
    fun tokenSupports_dogecoinDerivedBip44WithNativeCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Dogecoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        val derivedToken = token(BlockchainType.Dogecoin, TokenType.Derived(TokenType.Derivation.Bip44))
        assertTrue(derivedToken.supports(account))
    }

    @Test
    fun tokenSupports_dogecoinDerivedBip44WithOldBitcoinCoinType_returnsTrue() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Bitcoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        val derivedToken = token(BlockchainType.Dogecoin, TokenType.Derived(TokenType.Derivation.Bip44))
        assertTrue(derivedToken.supports(account))
    }

    @Test
    fun tokenSupports_dogecoinDerivedBip44WithLitecoinCoinType_returnsFalse() {
        val account = hdExtendedKeyAccount(
            coinTypes = listOf(ExtendedKeyCoinType.Litecoin),
            purposes = listOf(HDWallet.Purpose.BIP44)
        )
        val derivedToken = token(BlockchainType.Dogecoin, TokenType.Derived(TokenType.Derivation.Bip44))
        assertFalse(derivedToken.supports(account))
    }
}
