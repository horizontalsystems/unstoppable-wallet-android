package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.nft.DataWithError
import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class NftAssetItemsRepository(
    private val nftManager: NftManager,
    private val nftItemFactory: NftItemFactory
) {
    private var account: Account? = null

    private val _itemsDataFlow = MutableSharedFlow<DataWithError<Map<NftCollectionRecord, List<NftAssetItem>>?>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val itemsDataFlow = _itemsDataFlow.asSharedFlow()

    suspend fun refresh() {
        account?.let { account ->
            refresh(account, _itemsDataFlow.replayCache.lastOrNull()?.value)
        }
    }

    suspend fun setAccount(account: Account?) {
        this.account = account

        if (account != null) {
            val cachedItems = convertToItem(nftManager.getCollectionAndAssetsFromCache(account.id))

            if (cachedItems.isNotEmpty()) {
                _itemsDataFlow.tryEmit(DataWithError(cachedItems, null))
                refresh(account, cachedItems)
            } else {
                refresh(account, null)
            }
        }
    }

    private suspend fun refresh(account: Account, cachedItems: Map<NftCollectionRecord, List<NftAssetItem>>?) =
        withContext(Dispatchers.IO) {
            try {
                val collectionAndAssets = nftManager.getCollectionAndAssetsFromApi(account, getAddress(account))
                _itemsDataFlow.tryEmit(DataWithError(convertToItem(collectionAndAssets), null))
            } catch (e: Exception) {
                _itemsDataFlow.tryEmit(DataWithError(cachedItems, e))
            }
        }

    private fun convertToItem(collectionAssets: Map<NftCollectionRecord, List<NftAssetRecord>>) =
        collectionAssets.map { (collection, assets) ->
            val assetItems = assets.map { asset ->
                nftItemFactory.createNftAssetItem(asset, collection)
            }
            collection to assetItems
        }.toMap()

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
}
