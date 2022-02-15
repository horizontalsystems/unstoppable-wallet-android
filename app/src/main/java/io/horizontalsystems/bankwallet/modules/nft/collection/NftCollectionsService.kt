package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.core.orNull
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.nft.NftAsset
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update

class NftCollectionsService(
    private val nftManager: NftManager,
    private val accountManager: IAccountManager,
    private val nftItemFactory: NftItemFactory
) {
    private val _nftCollections =
        MutableStateFlow<DataState<List<NftCollectionItem>>>(DataState.Loading)
    val nftCollections = _nftCollections.asStateFlow()

    var priceType = PriceType.Days7
        private set

    private val disposables = CompositeDisposable()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var handleActiveAccountJob: Job? = null

    fun start() {
        accountManager.activeAccountObservable
            .subscribeIO {
                handleAccount(it.orNull)
            }
            .let {
                disposables.add(it)
            }

        handleAccount(accountManager.activeAccount)
    }

    fun updatePriceType(priceType: PriceType) {
        this.priceType = priceType

        handleUpdatedPriceType()
    }

    suspend fun refresh() {
        accountManager.activeAccount?.let { account ->
            nftManager.refresh(account, getAddress(account))
        }
    }

    fun stop() {
        disposables.clear()
        coroutineScope.cancel()
    }

    private fun handleAccount(account: Account?) {
        unsubscribeFromCollectionAssetUpdates()

        if (account != null) {
            subscribeForCollectionAssetUpdates(account) {
                handleUpdatedCollectionAssets(it)
            }

            coroutineScope.launch {
                nftManager.refresh(account, getAddress(account))
            }
        } else {
            _nftCollections.update {
                DataState.Error(NoActiveAccount())
            }
        }
    }

    private fun handleUpdatedCollectionAssets(collectionAssets: Map<NftCollection, List<NftAsset>>) {
        _nftCollections.update {
            DataState.Success(collectionAssets.map { (collection, assets) ->
                nftItemFactory.createNftCollectionItem(collection, assets, priceType)
            })
        }
    }

    private fun handleUpdatedPriceType() {
        _nftCollections.value.dataOrNull?.let { collections ->
            val list = collections.map { collectionItem ->
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

            _nftCollections.update {
                DataState.Success(list)
            }
        }
    }

    private fun getAddress(account: Account): Address {
        val addressStr = when (val type = account.type) {
            is AccountType.Address -> type.address
            is AccountType.Mnemonic -> Signer.address(type.seed, EthereumKit.NetworkType.EthMainNet).hex
            else -> throw Exception("Not Supported")
        }

        return Address(addressStr)
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
