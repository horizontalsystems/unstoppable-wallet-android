package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.RestoreSettingRecord
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.createaccount.CreateAccountModule
import io.horizontalsystems.walletkit.testutil.FakeAccountManager
import io.horizontalsystems.walletkit.testutil.FakeRestoreSettingsStorage
import io.horizontalsystems.walletkit.testutil.FakeWalletStorage
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletActivatorTest {

    private companion object {
        val NEW_WALLET_HEIGHTS = mapOf(
            BlockchainType.Zcash to 3_000_000L,
            BlockchainType.Monero to 3_500_000L,
        )
    }

    private fun account(origin: AccountOrigin) = Account(
        id = "account",
        name = "Account",
        type = AccountType.Mnemonic(List(12) { "word" }, ""),
        origin = origin,
        level = 0,
    )

    private fun token(query: TokenQuery) = Token(
        coin = Coin("uid-${query.blockchainType.uid}", "Name", "CODE"),
        blockchain = Blockchain(query.blockchainType, "Name", null),
        type = query.tokenType,
        decimals = 8,
    )

    private class Fixture(account: Account, knownQueries: List<TokenQuery>, token: (TokenQuery) -> Token) {
        val walletStorage = FakeWalletStorage()
        val restoreSettingsStorage = FakeRestoreSettingsStorage()
        val activator = WalletActivator(
            WalletManager(FakeAccountManager(account), walletStorage),
            RestoreSettingsManager(restoreSettingsStorage),
            token = { query -> if (query in knownQueries) token(query) else null },
            newWalletSettingValue = { _, blockchainType -> NEW_WALLET_HEIGHTS[blockchainType]?.toString() },
        )

        fun savedBirthdayHeight(blockchainType: BlockchainType) = restoreSettingsStorage
            .restoreSettings("account", blockchainType.uid)
            .firstOrNull { it.key == RestoreSettingType.BirthdayHeight.name }
            ?.value
    }

    private val defaults = CreateAccountModule.defaultTokenQueries

    @Test
    fun `new account starts with BTC, ETH, ZEC and XMR`() {
        assertEquals(
            listOf(
                BlockchainType.Bitcoin,
                BlockchainType.Ethereum,
                BlockchainType.Zcash,
                BlockchainType.Monero,
            ),
            defaults.map { it.blockchainType },
        )
    }

    @Test
    fun `activates a wallet for every default token`() {
        val account = account(AccountOrigin.Created)
        val fixture = Fixture(account, defaults, ::token)

        fixture.activator.activateWallets(account, defaults)

        assertEquals(defaults.map { Wallet(token(it), account) }, fixture.walletStorage.saved)
    }

    @Test
    fun `skips a token that is not known`() {
        val account = account(AccountOrigin.Created)
        val fixture = Fixture(account, defaults - defaults.last(), ::token)

        fixture.activator.activateWallets(account, defaults)

        assertEquals((defaults - defaults.last()).map { Wallet(token(it), account) }, fixture.walletStorage.saved)
    }

    @Test
    fun `saves the new-wallet birthday height of ZEC and XMR for a created account`() {
        val account = account(AccountOrigin.Created)
        val fixture = Fixture(account, defaults, ::token)

        fixture.activator.activateWallets(account, defaults)

        assertEquals("3000000", fixture.savedBirthdayHeight(BlockchainType.Zcash))
        assertEquals("3500000", fixture.savedBirthdayHeight(BlockchainType.Monero))
        assertEquals(null, fixture.savedBirthdayHeight(BlockchainType.Bitcoin))
    }

    @Test
    fun `keeps a saved birthday height`() {
        val account = account(AccountOrigin.Created)
        val fixture = Fixture(account, defaults, ::token)
        fixture.restoreSettingsStorage.save(
            listOf(
                RestoreSettingRecord(
                    account.id,
                    BlockchainType.Zcash.uid,
                    RestoreSettingType.BirthdayHeight.name,
                    "2500000",
                )
            )
        )

        fixture.activator.activateWallets(account, defaults)

        assertEquals("2500000", fixture.savedBirthdayHeight(BlockchainType.Zcash))
    }

    @Test
    fun `saves no birthday height for a restored account`() {
        val account = account(AccountOrigin.Restored)
        val fixture = Fixture(account, defaults, ::token)

        fixture.activator.activateTokens(account, defaults.map(::token))

        assertEquals(null, fixture.savedBirthdayHeight(BlockchainType.Zcash))
        assertEquals(null, fixture.savedBirthdayHeight(BlockchainType.Monero))
    }
}
