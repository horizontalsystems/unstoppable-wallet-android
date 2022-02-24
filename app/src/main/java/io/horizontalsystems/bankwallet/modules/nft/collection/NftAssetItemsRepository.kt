package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update

class NftAssetItemsRepository(
    private val nftManager: NftManager,
    private val nftItemFactory: NftItemFactory
) {
    private var account: Account? = null
    private val _assetItems =
        MutableStateFlow<Map<NftCollectionRecord, List<NftAssetItem>>>(mapOf())
    val assetItems = _assetItems.asStateFlow()

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
        _assetItems.update {
            collectionAssets.map { (collection, assets) ->
                val assetItems = assets.map { asset ->
                    nftItemFactory.createNftAssetItem(asset, collection.stats)
                }
                collection to assetItems
            }.toMap()
        }
    }

    private fun getAddress(account: Account): Address {
        val addressStr = when (val type = account.type) {
            is AccountType.Address -> type.address
            is AccountType.Mnemonic -> Signer.address(
                type.seed,
                EthereumKit.NetworkType.EthMainNet
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