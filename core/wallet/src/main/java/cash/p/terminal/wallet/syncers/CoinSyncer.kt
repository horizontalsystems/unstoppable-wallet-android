package cash.p.terminal.wallet.syncers

import android.util.Log
import cash.p.terminal.wallet.SyncInfo
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.managers.VirtualCoinMapper
import cash.p.terminal.wallet.models.BlockchainEntity
import cash.p.terminal.wallet.models.BlockchainResponse
import cash.p.terminal.wallet.models.CoinResponse
import cash.p.terminal.wallet.models.TokenEntity
import cash.p.terminal.wallet.models.TokenResponse
import cash.p.terminal.wallet.providers.HsProvider
import cash.p.terminal.wallet.storage.CoinStorage
import cash.p.terminal.wallet.storage.SyncerStateDao
import io.horizontalsystems.core.entities.BlockchainType
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class CoinSyncer(
    private val hsProvider: HsProvider,
    private val storage: CoinStorage,
    private val syncerStateDao: SyncerStateDao,
    private val virtualCoinMapper: VirtualCoinMapper
) {
    private val keyCoinsLastSyncTimestamp = "coin-syncer-coins-last-sync-timestamp"
    private val keyBlockchainsLastSyncTimestamp = "coin-syncer-blockchains-last-sync-timestamp"
    private val keyTokensLastSyncTimestamp = "coin-syncer-tokens-last-sync-timestamp"
    private val keyCoinsCount = "coin-syncer-coins-count"
    private val keyBlockchainsCount = "coin-syncer-blockchains-count"
    private val keyTokensCount = "coin-syncer-tokens-count"
    private val keyLastRequestTimestamp = "coin-syncer-last-request-timestamp"
    private val keyServerAvailable = "coin-syncer-server-available"

    private var disposable: Disposable? = null

    val fullCoinsUpdatedObservable = PublishSubject.create<Unit>()

    fun sync(
        coinsTimestamp: Long,
        blockchainsTimestamp: Long,
        tokensTimestamp: Long,
        forceUpdate: Boolean
    ) {
        val lastCoinsSyncTimestamp = syncerStateDao.get(keyCoinsLastSyncTimestamp)?.toLong() ?: 0
        val coinsOutdated = lastCoinsSyncTimestamp != coinsTimestamp

        val lastBlockchainsSyncTimestamp =
            syncerStateDao.get(keyBlockchainsLastSyncTimestamp)?.toLong() ?: 0
        val blockchainsOutdated = lastBlockchainsSyncTimestamp != blockchainsTimestamp

        val lastTokensSyncTimestamp = syncerStateDao.get(keyTokensLastSyncTimestamp)?.toLong() ?: 0
        val tokensOutdated = lastTokensSyncTimestamp != tokensTimestamp

        if (!forceUpdate && !coinsOutdated && !blockchainsOutdated && !tokensOutdated) return

        syncerStateDao.save(keyLastRequestTimestamp, System.currentTimeMillis().toString())
        syncerStateDao.save(keyServerAvailable, "false")

        disposable = Single.zip(
            hsProvider.allCoinsSingle().map { it.map { coinResponse -> coinEntity(coinResponse) } },
            hsProvider.allBlockchainsSingle()
                .map { it.map { blockchainResponse -> blockchainEntity(blockchainResponse) } },
            hsProvider.allTokensSingle()
                .map { it.map { tokenResponse -> tokenEntity(tokenResponse) } }
        ) { r1, r2, r3 -> Triple(r1, r2, r3) }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({ coinsData ->
                val (coins, blockchains, tokens) = coinsData
                if (coins.isNotEmpty() && blockchains.isNotEmpty() && tokens.isNotEmpty()) {
                    handleFetched(coins, blockchains, tokens)
                    saveLastSyncTimestamps(coinsTimestamp, blockchainsTimestamp, tokensTimestamp)
                    syncerStateDao.save(keyServerAvailable, "true")
                }
            }, {
                Log.e("CoinSyncer", "sync() error", it)
                syncerStateDao.save(keyServerAvailable, "false")
            })
    }

    private fun coinEntity(response: CoinResponse): Coin =
        Coin(
            response.uid,
            response.name,
            response.code.uppercase(),
            response.market_cap_rank,
            response.coingecko_id,
            response.image,
            response.priority
        )

    private fun blockchainEntity(response: BlockchainResponse): BlockchainEntity =
        BlockchainEntity(response.uid, response.name, response.url)

    private fun tokenEntity(response: TokenResponse): TokenEntity =
        TokenEntity(
            response.coin_uid,
            response.blockchain_uid,
            response.type,
            response.decimals,

            when (response.type) {
                "eip20" -> response.address
                "spl" -> response.address
                else -> response.address
            } ?: ""
        )

    fun stop() {
        disposable?.dispose()
        disposable = null
    }

    private fun handleFetched(
        coins: List<Coin>,
        blockchainEntities: List<BlockchainEntity>,
        tokenEntities: List<TokenEntity>
    ) {
        val transformedTokens = transform(tokenEntities)
        val validTokens = filterValidTokens(transformedTokens, blockchainEntities)
        val tokensWithVirtual = injectVirtualTokens(coins, validTokens)

        storage.update(coins, blockchainEntities, tokensWithVirtual)

        updateCounts()

        fullCoinsUpdatedObservable.onNext(Unit)
    }

    internal fun injectVirtualTokens(coins: List<Coin>, tokens: List<TokenEntity>): List<TokenEntity> {
        val coinsMap = coins.associateBy { it.code }
        val coinsUidMap = coins.associateBy { it.uid }
        val tokensIndex = tokens.associateBy { it.coinUid to it.blockchainUid }

        val virtualTokens = virtualCoinMapper.allMappings.mapNotNull { mapping ->
            coinsUidMap[mapping.virtualCoinUid] ?: return@mapNotNull null
            val realCoin = coinsMap[mapping.realCoinCode] ?: return@mapNotNull null
            val realToken = tokensIndex[realCoin.uid to mapping.blockchainType.uid]
                ?: return@mapNotNull null

            realToken.copy(coinUid = mapping.virtualCoinUid)
        }

        return tokens + virtualTokens
    }

    private fun updateCounts() {
        val coinsCount = storage.marketDatabase.coinDao().getCoinsCount()
        val blockchainsCount = storage.marketDatabase.coinDao().getBlockchainsCount()
        val tokensCount = storage.marketDatabase.coinDao().getTokensCount()

        syncerStateDao.save(keyCoinsCount, coinsCount.toString())
        syncerStateDao.save(keyBlockchainsCount, blockchainsCount.toString())
        syncerStateDao.save(keyTokensCount, tokensCount.toString())
    }

    internal fun transform(tokenEntities: List<TokenEntity>): List<TokenEntity> {
        val derivationReferences = TokenType.Derivation.values().map { it.name }
        val addressTypes = TokenType.AddressType.values().map { it.name }
        val addressSpecTypes = TokenType.AddressSpecType.values().map { it.name }

        var result = tokenEntities
        result = transform(
            tokenEntities = result,
            blockchainUid = BlockchainType.Bitcoin.uid,
            transformedType = "derived",
            references = derivationReferences
        )
        result = transform(
            tokenEntities = result,
            blockchainUid = BlockchainType.Zcash.uid,
            transformedType = "address_spec_type",
            references = addressSpecTypes
        )
        result = transform(
            tokenEntities = result,
            blockchainUid = BlockchainType.Litecoin.uid,
            transformedType = "derived",
            references = derivationReferences
        )
        result = transform(
            tokenEntities = result,
            blockchainUid = BlockchainType.BitcoinCash.uid,
            transformedType = "address_type",
            references = addressTypes
        )

        return result
    }

    private fun transform(
        tokenEntities: List<TokenEntity>,
        blockchainUid: String,
        transformedType: String,
        references: List<String>
    ): List<TokenEntity> {
        val tokenEntitiesMutable = tokenEntities.toMutableList()
        val indexOfFirst = tokenEntitiesMutable.indexOfFirst {
            it.blockchainUid == blockchainUid
        }
        if (indexOfFirst != -1) {
            val tokenEntity = tokenEntitiesMutable.removeAt(indexOfFirst)
            val entities = references.map {
                tokenEntity.copy(type = transformedType, reference = it)
            }
            tokenEntitiesMutable.addAll(entities)
        }
        return tokenEntitiesMutable
    }

    private fun saveLastSyncTimestamps(coins: Long, blockchains: Long, tokens: Long) {
        syncerStateDao.save(keyCoinsLastSyncTimestamp, coins.toString())
        syncerStateDao.save(keyBlockchainsLastSyncTimestamp, blockchains.toString())
        syncerStateDao.save(keyTokensLastSyncTimestamp, tokens.toString())
    }

    fun syncInfo(): SyncInfo {
        if (syncerStateDao.get(keyCoinsCount) == null ||
            syncerStateDao.get(keyBlockchainsCount) == null ||
            syncerStateDao.get(keyTokensCount) == null
        ) {
            updateCounts()
        }

        val coinsCount = syncerStateDao.get(keyCoinsCount)?.toIntOrNull()
        val blockchainsCount = syncerStateDao.get(keyBlockchainsCount)?.toIntOrNull()
        val tokensCount = syncerStateDao.get(keyTokensCount)?.toIntOrNull()

        return SyncInfo(
            coinsTimestamp = syncerStateDao.get(keyCoinsLastSyncTimestamp),
            blockchainsTimestamp = syncerStateDao.get(keyBlockchainsLastSyncTimestamp),
            tokensTimestamp = syncerStateDao.get(keyTokensLastSyncTimestamp),
            coinsCount = coinsCount,
            blockchainsCount = blockchainsCount,
            tokensCount = tokensCount,
            serverAvailable = syncerStateDao.get(keyServerAvailable)?.toBooleanStrictOrNull()
        )
    }

    companion object {
        internal fun filterValidTokens(
            tokens: List<TokenEntity>,
            blockchainEntities: List<BlockchainEntity>
        ): List<TokenEntity> {
            val blockchainUids = blockchainEntities.map { it.uid }.toSet()
            return tokens.filter { it.blockchainUid in blockchainUids }
        }
    }
}
