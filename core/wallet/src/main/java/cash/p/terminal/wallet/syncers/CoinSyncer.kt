package cash.p.terminal.wallet.syncers

import android.util.Log
import cash.p.terminal.wallet.SyncInfo
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
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
    private val syncerStateDao: SyncerStateDao
) {
    private val keyCoinsLastSyncTimestamp = "coin-syncer-coins-last-sync-timestamp"
    private val keyBlockchainsLastSyncTimestamp = "coin-syncer-blockchains-last-sync-timestamp"
    private val keyTokensLastSyncTimestamp = "coin-syncer-tokens-last-sync-timestamp"

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
                handleFetched(coinsData.first, coinsData.second, coinsData.third)
                saveLastSyncTimestamps(coinsTimestamp, blockchainsTimestamp, tokensTimestamp)
            }, {
                Log.e("CoinSyncer", "sync() error", it)
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
        storage.update(coins, blockchainEntities, transform(tokenEntities))
        fullCoinsUpdatedObservable.onNext(Unit)
    }

    private fun transform(tokenEntities: List<TokenEntity>): List<TokenEntity> {
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
        return SyncInfo(
            coinsTimestamp = syncerStateDao.get(keyCoinsLastSyncTimestamp),
            blockchainsTimestamp = syncerStateDao.get(keyBlockchainsLastSyncTimestamp),
            tokensTimestamp = syncerStateDao.get(keyTokensLastSyncTimestamp)
        )
    }

}
