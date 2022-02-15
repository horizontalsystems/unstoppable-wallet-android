package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.nft.NftAsset
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update

class NftCollectionsService(
    private val nftManager: NftManager,
    private val accountRepository: NftCollectionsAccountRepository,
    private val nftItemFactory: NftItemFactory
) {
    private val _nftCollections = MutableStateFlow<DataState<List<NftCollectionItem>>>(DataState.Loading)
    val nftCollections = _nftCollections.asStateFlow()

    var priceType = PriceType.Days7
        set(value) {
            field = value

            handleUpdatedPriceType()
        }

    private val disposables = CompositeDisposable()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var handleActiveAccountJob: Job? = null

    fun start() {
        coroutineScope.launch {
            accountRepository.account.collect {
                handleAccount(it?.first, it?.second)
            }
        }

        accountRepository.start()
    }

    suspend fun refresh() {
        accountRepository.account.value?.let { (account, address) ->
            nftManager.refresh(account, address)
        }
    }

    fun stop() {
        accountRepository.stop()
        disposables.clear()
        coroutineScope.cancel()
    }

    private fun handleAccount(account: Account?, address: Address?) {
        unsubscribeFromCollectionAssetUpdates()

        if (account != null && address != null) {
            subscribeForCollectionAssetUpdates(account) {
                handleUpdatedCollectionAssets(it)
            }

            coroutineScope.launch {
                nftManager.refresh(account, address)
            }
        } else {
            _nftCollections.update {
                DataState.Error(NoActiveAccount())
            }
        }
    }

    private fun handleUpdatedCollectionAssets(collectionAssets: Map<NftCollection, List<NftAsset>>) {
        val collectionItems = collectionAssets.map { (collection, assets) ->
            NftCollectionItem(
                slug = collection.slug,
                name = collection.name,
                imageUrl = collection.imageUrl,
                ownedAssetCount = collectionAssets.size,
                assets = assets.map { asset ->
                    nftItemFactory.createNftAssetItem(asset, collection.stats, priceType)
                }
            )
        }

        _nftCollections.update {
            DataState.Success(collectionItems)
        }
    }

    private fun handleUpdatedPriceType() {
        _nftCollections.update {
            when (it) {
                is DataState.Success -> {
                    val list = it.data.map { collectionItem ->
                        val assets = collectionItem.assets.map { assetItem ->
                            val coinPrice = when (priceType) {
                                PriceType.Days7 -> assetItem.prices.average7d
                                PriceType.Days30 -> assetItem.prices.average30d
                                PriceType.LastPrice -> assetItem.prices.last
                            }
                            assetItem.copy(coinPrice = coinPrice)
                        }
                        collectionItem.copy(assets = assets)
                    }
                    DataState.Success(list)
                }
                else -> it
            }
        }
    }

    private fun unsubscribeFromCollectionAssetUpdates() {
        handleActiveAccountJob?.cancel()
    }

    private fun subscribeForCollectionAssetUpdates(
        account: Account,
        callback: suspend (value: Map<NftCollection, List<NftAsset>>) -> Unit
    ) {
        handleActiveAccountJob = coroutineScope.launch {
            nftManager.getCollectionAndAssets(account.id)
                .collect(callback)
        }
    }
}
