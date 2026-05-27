package cash.p.terminal.wallet.storage

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.models.BlockchainEntity
import cash.p.terminal.wallet.models.TokenEntity
import io.horizontalsystems.core.entities.BlockchainType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class CoinStorageTokenQueryTest {

    private lateinit var database: MarketDatabase
    private lateinit var storage: CoinStorage

    private val blockchain = BlockchainEntity(
        uid = BlockchainType.BinanceSmartChain.uid,
        name = "BSC",
        eip3091url = null,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MarketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = CoinStorage(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getToken_referenceSuffixAlsoExact_returnsExactToken() {
        val exactReference = "abc"
        val suffixReference = "0xabc"
        val exactCoin = coin("exact-coin", marketCapRank = 2)
        val suffixCoin = coin("suffix-coin", marketCapRank = 1)

        storage.update(
            coins = listOf(suffixCoin, exactCoin),
            blockchainEntities = listOf(blockchain),
            tokenEntities = listOf(
                tokenEntity(suffixCoin, suffixReference),
                tokenEntity(exactCoin, exactReference),
            ),
        )

        val token = storage.getToken(tokenQuery(exactReference))

        assertEquals(exactCoin.uid, checkNotNull(token).coin.uid)
    }

    @Test
    fun getToken_referenceSuffixWithoutExact_returnsLegacySuffixToken() {
        val queryReference = "abc"
        val storedReference = "0xabc"
        val coin = coin("coin", marketCapRank = 1)

        storage.update(
            coins = listOf(coin),
            blockchainEntities = listOf(blockchain),
            tokenEntities = listOf(tokenEntity(coin, storedReference)),
        )

        val token = storage.getToken(tokenQuery(queryReference))

        assertEquals(coin.uid, checkNotNull(token).coin.uid)
    }

    @Test
    fun getTokens_mixedExactAndLegacySuffix_returnsBothTokens() {
        val exactCoin = coin("exact-coin", marketCapRank = 2)
        val suffixCoin = coin("suffix-coin", marketCapRank = 1)

        storage.update(
            coins = listOf(suffixCoin, exactCoin),
            blockchainEntities = listOf(blockchain),
            tokenEntities = listOf(
                tokenEntity(exactCoin, "exact"),
                tokenEntity(suffixCoin, "0xsuffix"),
            ),
        )

        val tokens = storage.getTokens(
            listOf(
                tokenQuery("exact"),
                tokenQuery("suffix"),
            )
        )

        assertEquals(listOf(exactCoin.uid, suffixCoin.uid), tokens.map { it.coin.uid })
    }

    private fun coin(uid: String, marketCapRank: Int) = Coin(
        uid = uid,
        name = uid,
        code = uid.uppercase(),
        marketCapRank = marketCapRank,
    )

    private fun tokenEntity(coin: Coin, reference: String) = TokenEntity(
        coinUid = coin.uid,
        blockchainUid = blockchain.uid,
        type = "eip20",
        decimals = 18,
        reference = reference,
    )

    private fun tokenQuery(reference: String) = TokenQuery(
        blockchainType = BlockchainType.BinanceSmartChain,
        tokenType = TokenType.Eip20(reference),
    )
}
