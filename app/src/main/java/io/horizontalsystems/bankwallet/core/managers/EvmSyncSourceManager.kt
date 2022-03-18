package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EvmSyncSourceManager(appConfigProvider: AppConfigProvider, val accountSettingManager: AccountSettingManager) {

    private val syncSourceRelay = PublishSubject.create<Triple<Account, EvmBlockchain, EvmSyncSource>>()

    val syncSourceObservable: Observable<Triple<Account, EvmBlockchain, EvmSyncSource>>
        get() = syncSourceRelay

    val defaultSyncSources: Map<EvmBlockchain, List<EvmSyncSource>> =
        mapOf(
            EvmBlockchain.Ethereum to listOf(
                getSyncSource(
                    EvmBlockchain.Ethereum,
                    "MainNet Websocket",
                    RpcSource.ethereumInfuraWebSocket(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret),
                    TransactionSource.ethereumEtherscan(appConfigProvider.etherscanApiKey)
                ),
                getSyncSource(
                    EvmBlockchain.Ethereum,
                    "MainNet HTTP",
                    RpcSource.ethereumInfuraHttp(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret),
                    TransactionSource.ethereumEtherscan(appConfigProvider.etherscanApiKey)
                )
            ),

            EvmBlockchain.BinanceSmartChain to listOf(
                getSyncSource(EvmBlockchain.BinanceSmartChain, "MainNet HTTP", RpcSource.binanceSmartChainHttp(), TransactionSource.bscscan(appConfigProvider.bscscanApiKey)),
                getSyncSource(EvmBlockchain.BinanceSmartChain, "MainNet Websocket", RpcSource.binanceSmartChainWebSocket(), TransactionSource.bscscan(appConfigProvider.bscscanApiKey))
            ),

            EvmBlockchain.Polygon to listOf(
                getSyncSource(EvmBlockchain.Polygon, "Polygon-RPC HTTP", RpcSource.polygonRpcHttp(), TransactionSource.polygonscan(appConfigProvider.polygonscanApiKey))
            ),

            EvmBlockchain.Optimism to listOf(
                getSyncSource(EvmBlockchain.Optimism, "Optimism.io HTTP", RpcSource.optimismRpcHttp(), TransactionSource.optimisticEtherscan(""))
            ),

            EvmBlockchain.ArbitrumOne to listOf(
                getSyncSource(EvmBlockchain.ArbitrumOne, "Arbitrum.io HTTP", RpcSource.arbitrumOneRpcHttp(), TransactionSource.arbiscan(""))
            )
        )

    private fun getSyncSource(evmBlockchain: EvmBlockchain, name: String, rpcSource: RpcSource, transactionSource: TransactionSource) =
        EvmSyncSource(
            "${evmBlockchain.uid}|${name}|${transactionSource.name}|${rpcSource.urls.joinToString(separator = ",") { it.toString() }}",
            name,
            rpcSource,
            transactionSource
        )

    fun getAllBlockchains(blockchain: EvmBlockchain): List<EvmSyncSource> =
        defaultSyncSources[blockchain] ?: listOf()

    fun getSyncSource(account: Account, blockchain: EvmBlockchain): EvmSyncSource {
        val syncSources = getAllBlockchains(blockchain)

        val syncSourceName = accountSettingManager.getEvmSyncSourceName(account, blockchain)
        val syncSource = syncSources.firstOrNull { it.name == syncSourceName }

        return syncSource ?: syncSources[0]
    }

    fun save(syncSource: EvmSyncSource, account: Account, blockchain: EvmBlockchain) {
        accountSettingManager.save(syncSource.name, account, blockchain)
        syncSourceRelay.onNext(Triple(account, blockchain, syncSource))
    }

}
