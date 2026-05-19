package cash.p.terminal.wallet.syncers

import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.managers.VirtualCoinMapper
import cash.p.terminal.wallet.models.BlockchainEntity
import cash.p.terminal.wallet.models.TokenEntity
import cash.p.terminal.wallet.providers.HsProvider
import cash.p.terminal.wallet.storage.CoinStorage
import cash.p.terminal.wallet.storage.SyncerStateDao
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoinSyncerTest {

    private val hsProvider: HsProvider = mockk(relaxed = true)
    private val coinStorage: CoinStorage = mockk(relaxed = true)
    private val syncerStateDao: SyncerStateDao = mockk(relaxed = true)
    private val virtualCoinMapper = VirtualCoinMapper()

    private val coinSyncer = CoinSyncer(
        hsProvider = hsProvider,
        storage = coinStorage,
        syncerStateDao = syncerStateDao,
        virtualCoinMapper = virtualCoinMapper
    )

    private fun createCoin(uid: String, code: String) = Coin(
        uid = uid,
        name = code,
        code = code,
        marketCapRank = null,
        coinGeckoId = null,
        image = null,
        priority = 0
    )

    // region injectVirtualTokens tests

    @Test
    fun injectVirtualTokens_bscUsdWithTether_addsVirtualUsdtToken() {
        val coins = listOf(
            createCoin("tether", "USDT"),
            createCoin("bsc-usd", "BSC-USD")
        )
        val tokens = listOf(
            createToken("bsc-usd", "binance-smart-chain")
        )

        val result = coinSyncer.injectVirtualTokens(coins, tokens)

        assertEquals(2, result.size)
        assertTrue(result.any { it.coinUid == "bsc-usd" && it.blockchainUid == "binance-smart-chain" })
        assertTrue(result.any { it.coinUid == "tether" && it.blockchainUid == "binance-smart-chain" })
    }

    @Test
    fun injectVirtualTokens_missingTetherCoin_returnsOriginalTokens() {
        val coins = listOf(
            createCoin("bsc-usd", "BSC-USD")
        )
        val tokens = listOf(
            createToken("bsc-usd", "binance-smart-chain")
        )

        val result = coinSyncer.injectVirtualTokens(coins, tokens)

        assertEquals(1, result.size)
        assertEquals("bsc-usd", result[0].coinUid)
    }

    @Test
    fun injectVirtualTokens_missingBscUsdCoin_returnsOriginalTokens() {
        val coins = listOf(
            createCoin("tether", "USDT")
        )
        val tokens = listOf(
            createToken("some-token", "binance-smart-chain")
        )

        val result = coinSyncer.injectVirtualTokens(coins, tokens)

        assertEquals(1, result.size)
        assertEquals("some-token", result[0].coinUid)
    }

    @Test
    fun injectVirtualTokens_bscUsdTokenOnWrongBlockchain_returnsOriginalTokens() {
        val coins = listOf(
            createCoin("tether", "USDT"),
            createCoin("bsc-usd", "BSC-USD")
        )
        val tokens = listOf(
            createToken("bsc-usd", "ethereum")
        )

        val result = coinSyncer.injectVirtualTokens(coins, tokens)

        assertEquals(1, result.size)
        assertEquals("bsc-usd", result[0].coinUid)
    }

    @Test
    fun injectVirtualTokens_emptyCoins_returnsOriginalTokens() {
        val coins = emptyList<Coin>()
        val tokens = listOf(
            createToken("bsc-usd", "binance-smart-chain")
        )

        val result = coinSyncer.injectVirtualTokens(coins, tokens)

        assertEquals(tokens, result)
    }

    @Test
    fun injectVirtualTokens_emptyTokens_returnsEmptyList() {
        val coins = listOf(
            createCoin("tether", "USDT"),
            createCoin("bsc-usd", "BSC-USD")
        )
        val tokens = emptyList<TokenEntity>()

        val result = coinSyncer.injectVirtualTokens(coins, tokens)

        assertTrue(result.isEmpty())
    }

    // endregion

    // region filterValidTokens tests

    @Test
    fun transform_litecoinNativeToken_createsDerivedTokens() {
        val result = coinSyncer.transform(
            listOf(createToken(coinUid = "litecoin", blockchainUid = "litecoin", decimals = 8))
        )

        assertEquals(
            listOf(
                "derived" to "Bip44",
                "derived" to "Bip49",
                "derived" to "Bip84",
                "derived" to "Bip86"
            ),
            result.map { it.type to it.reference }
        )
        assertTrue(result.all { it.coinUid == "litecoin" && it.blockchainUid == "litecoin" && it.decimals == 8 })
    }

    @Test
    fun transform_litecoinNativeAndMwebTokens_preservesMwebToken() {
        val result = coinSyncer.transform(
            listOf(
                createToken(coinUid = "litecoin", blockchainUid = "litecoin", decimals = 8),
                createToken(
                    coinUid = "litecoin",
                    blockchainUid = "litecoin",
                    type = "mweb",
                    decimals = 8
                )
            )
        )

        assertEquals(1, result.count { it.coinUid == "litecoin" && it.blockchainUid == "litecoin" && it.type == "mweb" })
        assertEquals(4, result.count { it.coinUid == "litecoin" && it.blockchainUid == "litecoin" && it.type == "derived" })
    }

    @Test
    fun filterValidTokens_validBlockchainUid_retainsToken() {
        val blockchains = listOf(
            BlockchainEntity(uid = "ethereum", name = "Ethereum", eip3091url = null),
            BlockchainEntity(uid = "bitcoin", name = "Bitcoin", eip3091url = null)
        )
        val tokens = listOf(
            createToken(coinUid = "eth", blockchainUid = "ethereum"),
            createToken(coinUid = "btc", blockchainUid = "bitcoin")
        )

        val result = CoinSyncer.filterValidTokens(tokens, blockchains)

        assertEquals(2, result.size)
        assertEquals("eth", result[0].coinUid)
        assertEquals("btc", result[1].coinUid)
    }

    @Test
    fun filterValidTokens_invalidBlockchainUid_filtersOutToken() {
        val blockchains = listOf(
            BlockchainEntity(uid = "ethereum", name = "Ethereum", eip3091url = null)
        )
        val tokens = listOf(
            createToken(coinUid = "eth", blockchainUid = "ethereum"),
            createToken(coinUid = "canton-token", blockchainUid = "canton-network")
        )

        val result = CoinSyncer.filterValidTokens(tokens, blockchains)

        assertEquals(1, result.size)
        assertEquals("eth", result[0].coinUid)
    }

    @Test
    fun filterValidTokens_emptyBlockchainEntities_filtersOutAllTokens() {
        val blockchains = emptyList<BlockchainEntity>()
        val tokens = listOf(
            createToken(coinUid = "eth", blockchainUid = "ethereum"),
            createToken(coinUid = "btc", blockchainUid = "bitcoin")
        )

        val result = CoinSyncer.filterValidTokens(tokens, blockchains)

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterValidTokens_emptyTokens_returnsEmptyList() {
        val blockchains = listOf(
            BlockchainEntity(uid = "ethereum", name = "Ethereum", eip3091url = null)
        )
        val tokens = emptyList<TokenEntity>()

        val result = CoinSyncer.filterValidTokens(tokens, blockchains)

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterValidTokens_mixedValidAndInvalidTokens_retainsOnlyValid() {
        val blockchains = listOf(
            BlockchainEntity(uid = "ethereum", name = "Ethereum", eip3091url = null),
            BlockchainEntity(uid = "binance-smart-chain", name = "BSC", eip3091url = null)
        )
        val tokens = listOf(
            createToken(coinUid = "eth", blockchainUid = "ethereum"),
            createToken(coinUid = "orphan1", blockchainUid = "canton-network"),
            createToken(coinUid = "bnb", blockchainUid = "binance-smart-chain"),
            createToken(coinUid = "orphan2", blockchainUid = "unknown-chain")
        )

        val result = CoinSyncer.filterValidTokens(tokens, blockchains)

        assertEquals(2, result.size)
        assertEquals("eth", result[0].coinUid)
        assertEquals("bnb", result[1].coinUid)
    }

    // endregion

    private fun createToken(
        coinUid: String,
        blockchainUid: String,
        type: String = "native",
        decimals: Int = 18,
        reference: String = ""
    ) = TokenEntity(
        coinUid = coinUid,
        blockchainUid = blockchainUid,
        type = type,
        decimals = decimals,
        reference = reference
    )
}
