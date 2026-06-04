package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.solanakit.models.RpcSource
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

@javax.inject.Singleton
class SolanaRpcSourceManager @javax.inject.Inject constructor(
        private val blockchainSettingsStorage: BlockchainSettingsStorage,
        private val marketKitWrapper: MarketKitWrapper,
        private val appConfigProvider: AppConfigProvider,
) {

    private val blockchainType = BlockchainType.Solana
    private val rpcSourceSubjectUpdate = PublishSubject.create<Unit>()

    val rpcSourceUpdateObservable: Observable<Unit>
        get() = rpcSourceSubjectUpdate

    val allRpcSources = listOf(RpcSource.Alchemy(appConfigProvider.solanaAlchemyApiKey))

    val rpcSource: RpcSource
        get() {
            val rpcSourceName = blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)
            val rpcSource = allRpcSources.firstOrNull { it.name == rpcSourceName }

            return rpcSource ?: allRpcSources[0]
        }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(blockchainType.uid)

    fun save(rpcSource: RpcSource) {
        blockchainSettingsStorage.save(rpcSource.name, blockchainType)
        rpcSourceSubjectUpdate.onNext(Unit)
    }

}
