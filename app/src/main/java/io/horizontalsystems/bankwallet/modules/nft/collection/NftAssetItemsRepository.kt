package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Chain
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect

class NftAssetItemsRepository(
    private val nftManager: NftManager,
    private val nftItemFactory: NftItemFactory
) {
    private var account: Account? = null
    private val _itemsFlow =
        MutableSharedFlow<Map<NftCollectionRecord, List<NftAssetItem>>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val itemsFlow = _itemsFlow.asSharedFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var handleActiveAccountJob: Job? = null

    suspend fun refresh() {
        account?.let { account ->
            nftManager.refresh(account, getAddress(account))
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun setAccount(account: Account?) {
        this.account = account

        unsubscribeFromCollectionAssetUpdates()

        if (account != null) {
            subscribeForCollectionAssetUpdates(account) {
                handleUpdatedCollectionAssets(it)
            }

            coroutineScope.launch {
                nftManager.refresh(account, getAddress(account))
            }
        }
    }

    private fun handleUpdatedCollectionAssets(collectionAssets: Map<NftCollectionRecord, List<NftAssetRecord>>) {
        _itemsFlow.tryEmit(
            collectionAssets.map { (collection, assets) ->
                val assetItems = assets.map { asset ->
                    nftItemFactory.createNftAssetItem(asset, collection)
                }
                collection to assetItems
            }.toMap()
        )
    }

    private fun getAddress(account: Account): Address {
        val addressStr = when (val type = account.type) {
            is AccountType.Address -> type.address
            is AccountType.Mnemonic -> Signer.address(
                type.seed,
                Chain.Ethereum
            ).hex
            else -> throw Exception("Not Supported")
        }

        return Address(addressStr)
    }

    private fun unsubscribeFromCollectionAssetUpdates() {
        handleActiveAccountJob?.cancel()
    }

    private fun subscribeForCollectionAssetUpdates(
        account: Account,
        callback: suspend (value: Map<NftCollectionRecord, List<NftAssetRecord>>) -> Unit
    ) {
        handleActiveAccountJob = coroutineScope.launch {
            nftManager.getCollectionAndAssets(account.id)
                .collect(callback)
        }
    }
}