package cash.p.terminal.core.managers

import cash.p.terminal.core.adapters.nft.INftAdapter
import cash.p.terminal.core.storage.NftStorage
import cash.p.terminal.entities.nft.NftAddressMetadata
import cash.p.terminal.entities.nft.NftKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NftMetadataSyncer(
    private val nftAdapterManager: NftAdapterManager,
    private val nftMetadataManager: NftMetadataManager,
    private val nftStorage: NftStorage
) {
    private val syncThreshold: Long = 1 * 60 * 60 // 1 hour in seconds
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        coroutineScope.launch {
            nftAdapterManager.adaptersUpdatedFlow.collect { adaptersMap ->
                launch {
                    sync(adaptersMap)
                }
                launch {
                    subscribeToAdapterRecords(adaptersMap)
                }
            }
        }
    }

    fun refresh() {
        coroutineScope.launch {
            sync(nftAdapterManager.adaptersUpdatedFlow.value, true)
        }
    }

    private suspend fun subscribeToAdapterRecords(adaptersMap: Map<NftKey, INftAdapter>) = withContext(Dispatchers.IO) {
        adaptersMap.forEach { (nftKey, adapter) ->
            launch {
                adapter.nftRecordsFlow.collect { sync(nftKey, adapter, true) }
            }
        }
    }

    private suspend fun sync(adaptersMap: Map<NftKey, INftAdapter>, force: Boolean = false) =
        adaptersMap.forEach { (nftKey, adapter) ->
            sync(nftKey, adapter, force)
        }

    private suspend fun sync(nftKey: NftKey, adapter: INftAdapter, force: Boolean) {
        val currentTimestamp = System.currentTimeMillis() / 1000
        val lastSyncTimestamp = nftStorage.lastSyncTimestamp(nftKey)

        if (!force && lastSyncTimestamp != null && currentTimestamp - lastSyncTimestamp < syncThreshold) {
            return
        }

        try {
            val addressMetadata = nftMetadataManager.addressMetadata(nftKey.blockchainType, adapter.userAddress)
            handle(addressMetadata, nftKey, currentTimestamp)
        } catch (noProviderError: NftMetadataManager.ProviderError.NoProviderForBlockchainType) {
            //TODO
        } catch (error: Throwable) {
            error.printStackTrace()
        }
    }

    private fun handle(addressMetadata: NftAddressMetadata, nftKey: NftKey, currentTimestamp: Long) {
        nftStorage.save(currentTimestamp, nftKey)
        nftMetadataManager.handle(addressMetadata, nftKey)
    }

}