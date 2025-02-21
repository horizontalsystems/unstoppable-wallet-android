package io.horizontalsystems.bankwallet.core.managers

import android.net.Uri
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.EvmSyncSourceStorage
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.horizontalsystems.bankwallet.entities.EvmSyncSourceRecord
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URI

class EvmSyncSourceManager(
    private val appConfigProvider: AppConfigProvider,
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val evmSyncSourceStorage: EvmSyncSourceStorage,
) {

    private val syncSourceSubject = PublishSubject.create<BlockchainType>()

    private val _syncSourcesUpdatedFlow =
        MutableSharedFlow<BlockchainType>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val syncSourcesUpdatedFlow = _syncSourcesUpdatedFlow.asSharedFlow()

    private fun defaultTransactionSource(blockchainType: BlockchainType): TransactionSource {
        return when (blockchainType) {
            BlockchainType.Ethereum -> TransactionSource.ethereumEtherscan(appConfigProvider.etherscanApiKey)
            BlockchainType.BinanceSmartChain -> TransactionSource.bscscan(appConfigProvider.bscscanApiKey)
            BlockchainType.Polygon -> TransactionSource.polygonscan(appConfigProvider.polygonscanApiKey)
            BlockchainType.Avalanche -> TransactionSource.snowtrace(appConfigProvider.snowtraceApiKey)
            BlockchainType.Optimism -> TransactionSource.optimisticEtherscan(appConfigProvider.optimisticEtherscanApiKey)
            BlockchainType.ArbitrumOne -> TransactionSource.arbiscan(appConfigProvider.arbiscanApiKey)
            BlockchainType.Gnosis -> TransactionSource.gnosis(appConfigProvider.gnosisscanApiKey)
            BlockchainType.Fantom -> TransactionSource.fantom(appConfigProvider.ftmscanApiKey)
            BlockchainType.Base -> TransactionSource.basescan(appConfigProvider.basescanApiKey)
            BlockchainType.ZkSync -> TransactionSource.eraZkSync(appConfigProvider.eraZkSyncApiKey)
            else -> throw Exception("Non-supported EVM blockchain")
        }
    }

    val syncSourceObservable: Observable<BlockchainType>
        get() = syncSourceSubject

    fun defaultSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> {
        return when (blockchainType) {
            BlockchainType.Ethereum -> listOf(
                evmSyncSource(
                    blockchainType,
                    "BlocksDecoded",
                    RpcSource.Http(listOf(URI(appConfigProvider.blocksDecodedEthereumRpc)), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "LlamaNodes",
                    RpcSource.Http(listOf(URI("https://eth.llamarpc.com")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.BinanceSmartChain -> listOf(
                evmSyncSource(
                    blockchainType,
                    "BlockRazor",
                    RpcSource.Http(listOf(URI("https://unstoppable.bsc.blockrazor.xyz")), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "48club",
                    RpcSource.Http(listOf(URI("https://unstoppable.rpc.48.club")), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Binance",
                    RpcSource.binanceSmartChainHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "BSC RPC",
                    RpcSource.bscRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Omnia",
                    RpcSource.Http(listOf(URI("https://endpoints.omniatech.io/v1/bsc/mainnet/public")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Polygon -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Polygon RPC",
                    RpcSource.polygonRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "LlamaNodes",
                    RpcSource.Http(listOf(URI("https://polygon.llamarpc.com")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Avalanche -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Avax Network",
                    RpcSource.avaxNetworkHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "PublicNode",
                    RpcSource.Http(listOf(URI("https://avalanche-evm.publicnode.com")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Optimism -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Optimism",
                    RpcSource.optimismRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Omnia",
                    RpcSource.Http(
                        listOf(URI("https://endpoints.omniatech.io/v1/op/mainnet/public")),
                        null
                    ),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Base -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Base",
                    RpcSource.baseRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "LlamaNodes",
                    RpcSource.Http(
                        listOf(URI("https://base.llamarpc.com")),
                        null
                    ),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Omnia",
                    RpcSource.Http(
                        listOf(URI("https://endpoints.omniatech.io/v1/base/mainnet/public")),
                        null
                    ),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.ZkSync -> listOf(
                evmSyncSource(
                    blockchainType,
                    "ZKsync",
                    RpcSource.zkSyncRpcHttp(),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.ArbitrumOne -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Arbitrum",
                    RpcSource.arbitrumOneRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Omnia",
                    RpcSource.Http(listOf(URI("https://endpoints.omniatech.io/v1/arbitrum/one/public")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Gnosis -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Gnosis Chain",
                    RpcSource.gnosisRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Ankr",
                    RpcSource.Http(listOf(URI("https://rpc.ankr.com/gnosis")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            BlockchainType.Fantom -> listOf(
                evmSyncSource(
                    blockchainType,
                    "Fantom Chain",
                    RpcSource.fantomRpcHttp(),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Fantom Chain (Mirror)",
                    RpcSource.Http(listOf(URI("https://rpcapi.fantom.network/")), null),
                    defaultTransactionSource(blockchainType)
                ),
                evmSyncSource(
                    blockchainType,
                    "Ankr",
                    RpcSource.Http(listOf(URI("https://rpc.ankr.com/fantom")), null),
                    defaultTransactionSource(blockchainType)
                )
            )

            else -> listOf()
        }
    }

    fun customSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> {
        val records = evmSyncSourceStorage.evmSyncSources(blockchainType)
        return try {
            records.mapNotNull { record ->
                val uri = Uri.parse(record.url)
                val rpcSource = when (uri.scheme) {
                    "http",
                    "https" -> RpcSource.Http(listOf(URI(record.url)), record.auth)

                    "ws",
                    "wss" -> RpcSource.WebSocket(URI(record.url), record.auth)

                    else -> return@mapNotNull null
                }
                EvmSyncSource(
                    id = blockchainType.uid + "|" + record.url,
                    name = uri.host ?: "",
                    rpcSource = rpcSource,
                    transactionSource = defaultTransactionSource(blockchainType)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun evmSyncSource(
        blockchainType: BlockchainType,
        name: String,
        rpcSource: RpcSource,
        transactionSource: TransactionSource
    ) =
        EvmSyncSource(
            id = "${blockchainType.uid}|${name}|${transactionSource.name}|${
                rpcSource.uris.joinToString(separator = ",") { it.toString() }
            }",
            name = name,
            rpcSource = rpcSource,
            transactionSource = transactionSource
        )

    fun allSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> =
        defaultSyncSources(blockchainType) + customSyncSources(blockchainType)

    fun getSyncSource(blockchainType: BlockchainType): EvmSyncSource {
        val syncSources = allSyncSources(blockchainType)

        val syncSourceUrl = blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)
        val syncSource = syncSources.firstOrNull { it.uri.toString() == syncSourceUrl }

        return syncSource ?: syncSources[0]
    }

    fun getHttpSyncSource(blockchainType: BlockchainType): EvmSyncSource? {
        val syncSources = allSyncSources(blockchainType)
        blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)?.let { url ->
            syncSources.firstOrNull { it.uri.toString() == url && it.isHttp }?.let { syncSource ->
                return syncSource
            }
        }

        return syncSources.firstOrNull { it.isHttp }
    }

    fun save(syncSource: EvmSyncSource, blockchainType: BlockchainType) {
        blockchainSettingsStorage.save(syncSource.uri.toString(), blockchainType)
        syncSourceSubject.onNext(blockchainType)
    }

    fun saveSyncSource(blockchainType: BlockchainType, url: String, auth: String?) {
        val record = EvmSyncSourceRecord(
            blockchainTypeUid = blockchainType.uid,
            url = url,
            auth = auth
        )

        evmSyncSourceStorage.save(record)

        customSyncSources(blockchainType).firstOrNull { it.uri.toString() == url }?.let {
            save(it, blockchainType)
        }

        _syncSourcesUpdatedFlow.tryEmit(blockchainType)
    }

    fun delete(syncSource: EvmSyncSource, blockchainType: BlockchainType) {
        val isCurrent = getSyncSource(blockchainType) == syncSource

        evmSyncSourceStorage.delete(blockchainType.uid, syncSource.uri.toString())

        if (isCurrent) {
            syncSourceSubject.onNext(blockchainType)
        }

        _syncSourcesUpdatedFlow.tryEmit(blockchainType)
    }

}
