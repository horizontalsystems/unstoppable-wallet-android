package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.EnabledWallet
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.policy.HardwareWalletTokenPolicy
import cash.p.terminal.wallet.useCases.GetHardwarePublicKeyForWalletUseCase
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WalletStorageTest {

    private val account = Account(
        id = "account-id",
        name = "Account",
        type = AccountType.Mnemonic(
            words = List(12) { "abandon" },
            passphrase = ""
        ),
        origin = AccountOrigin.Created,
        level = 0
    )

    private val blockchain = Blockchain(
        type = BlockchainType.BinanceSmartChain,
        name = "BSC",
        eip3091url = null
    )

    private val token = Token(
        coin = Coin(
            uid = "coin-uid",
            name = "Coin",
            code = "COIN"
        ),
        blockchain = blockchain,
        type = TokenType.Native,
        decimals = 18
    )

    private val walletFactory = WalletFactory(object : HardwareWalletTokenPolicy {
        override fun isSupported(blockchainType: BlockchainType, tokenType: TokenType) = true
    })

    private val hardwareStorage = object : IHardwarePublicKeyStorage {
        override fun deleteAll() = Unit
        override suspend fun save(keys: List<cash.p.terminal.wallet.entities.HardwarePublicKey>) = Unit
        override suspend fun getKey(
            accountId: String,
            blockchainType: BlockchainType,
            tokenType: TokenType
        ) = null

        override suspend fun getKeyByBlockchain(
            accountId: String,
            blockchainType: BlockchainType
        ) = null

        override suspend fun getAllPublicKeys(accountId: String) = emptyList<cash.p.terminal.wallet.entities.HardwarePublicKey>()
    }

    private val deletedWalletChecker = object : IDeletedWalletChecker {
        override suspend fun getDeletedTokenQueryIds(accountId: String) = emptySet<String>()
    }

    @Test
    fun `save ignores duplicates within the same batch`() {
        val enabledWalletStorage = InMemoryEnabledWalletStorage()
        val walletStorage = WalletStorage(
            marketKit = mockk(relaxed = true),
            storage = enabledWalletStorage,
            getHardwarePublicKeyForWalletUseCase = GetHardwarePublicKeyForWalletUseCase(hardwareStorage),
            walletFactory = walletFactory,
            deletedWalletChecker = deletedWalletChecker
        )

        val wallet = walletFactory.create(token, account, null)!!

        walletStorage.save(listOf(wallet, wallet))

        assertSingleWalletStored(enabledWalletStorage, wallet)
    }

    @Test
    fun `save skips wallets already persisted for account`() {
        val enabledWalletStorage = InMemoryEnabledWalletStorage()
        val walletStorage = WalletStorage(
            marketKit = mockk(relaxed = true),
            storage = enabledWalletStorage,
            getHardwarePublicKeyForWalletUseCase = GetHardwarePublicKeyForWalletUseCase(hardwareStorage),
            walletFactory = walletFactory,
            deletedWalletChecker = deletedWalletChecker
        )

        val wallet = walletFactory.create(token, account, null)!!

        // seed storage with existing wallet
        enabledWalletStorage.save(
            listOf(
                EnabledWallet(
                    tokenQueryId = wallet.token.tokenQuery.id,
                    accountId = account.id,
                    coinName = wallet.coin.name,
                    coinCode = wallet.coin.code,
                    coinDecimals = wallet.decimal,
                    coinImage = wallet.coin.image
                )
            )
        )

        walletStorage.save(listOf(wallet))

        assertSingleWalletStored(enabledWalletStorage, wallet)
    }

    @Test
    fun `walletFactory skips unsupported token for monero mnemonic account`() {
        val moneroAccount = Account(
            id = "monero-account-id",
            name = "Monero",
            type = AccountType.MnemonicMonero(
                words = List(25) { "abandon" },
                password = "",
                height = 0,
                walletInnerName = "wallet"
            ),
            origin = AccountOrigin.Created,
            level = 0
        )
        val evmToken = Token(
            coin = Coin(uid = "eth", name = "Ethereum", code = "ETH"),
            blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
            type = TokenType.Native,
            decimals = 18
        )
        val moneroToken = Token(
            coin = Coin(uid = "monero", name = "Monero", code = "XMR"),
            blockchain = Blockchain(BlockchainType.Monero, "Monero", null),
            type = TokenType.Native,
            decimals = 12
        )

        assertNull(walletFactory.create(evmToken, moneroAccount, null))
        assertNotNull(walletFactory.create(moneroToken, moneroAccount, null))
    }

    private fun assertSingleWalletStored(
        storage: InMemoryEnabledWalletStorage,
        wallet: Wallet
    ) {
        val storedWallets = storage.enabledWallets(wallet.account.id)
        assertEquals(1, storedWallets.size)
        assertEquals(
            expected = wallet.token.tokenQuery.id,
            actual = storedWallets.single().tokenQueryId
        )
    }

    private class InMemoryEnabledWalletStorage : IEnabledWalletStorage {
        private val items = mutableListOf<EnabledWallet>()
        private var idSequence = 0L

        override val enabledWallets: List<EnabledWallet>
            get() = items.toList()

        override fun enabledWallets(accountId: String): List<EnabledWallet> =
            items.filter { it.accountId == accountId }

        override fun save(enabledWallets: List<EnabledWallet>): List<Long> {
            return enabledWallets.map { wallet ->
                val id = ++idSequence
                items += wallet.copy(id = id)
                id
            }
        }

        override fun deleteAll() {
            items.clear()
        }

        override fun delete(enabledWalletIds: List<Long>) {
            items.removeAll { it.id in enabledWalletIds.toSet() }
        }

        override fun deleteByTokenQueryId(accountId: String, tokenQueryId: String) {
            items.removeAll { it.accountId == accountId && it.tokenQueryId == tokenQueryId }
        }
    }
}
