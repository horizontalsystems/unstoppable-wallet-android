package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.core.orNull
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update

class NftCollectionsService(
    private val repository: NftCollectionsRepository,
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager
) {
    var priceType = PriceType.Days7
        private set

    private val _collectionRecords =
        MutableStateFlow<DataState<Map<NftCollectionRecord, List<NftAssetItemPriced>>>>(DataState.Loading)
    val collectionRecords = _collectionRecords.asStateFlow()

    private var collectionRecordsCache = mapOf<NftCollectionRecord, List<NftAssetItem>>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun updatePriceType(priceType: PriceType) {
        this.priceType = priceType

//        handleUpdatedPriceType()
    }

    fun start() {
        coroutineScope.launch {
            repository.collectionRecords
                .collect {
                    it.dataOrNull?.let {
                        val items = it.map { (collectionRecord, assetItems) ->
                            collectionRecord to assetItems.map { assetItem ->
                                val coinPrice = getAssetPrice(assetItem, priceType)

                                val baseCurrency = currencyManager.baseCurrency
                                val currencyPrice = coinPrice?.let { coinValue ->
                                    marketKit.coinPrice(
                                        coinValue.coin.uid,
                                        baseCurrency.code
                                    )?.value?.let { rate ->
                                        CurrencyValue(baseCurrency, coinValue.value.multiply(rate))
                                    }
                                }

                                NftAssetItemPriced(
                                    assetItem = assetItem,
                                    coinPrice = coinPrice,
                                    currencyPrice = currencyPrice
                                )
                            }
                        }.toMap()

                        _collectionRecords.update {
                            DataState.Success(items)
                        }

                        collectionRecordsCache = it
                    }
                }
        }

        repository.start()
    }

    private fun getAssetPrice(assetItem: NftAssetItem, priceType: PriceType) = when (priceType) {
        PriceType.Days7 -> assetItem.prices.average7d
        PriceType.Days30 -> assetItem.prices.average30d
        PriceType.LastPrice -> assetItem.prices.last
    }


    fun stop() {
        repository.stop()
    }

    suspend fun refresh() {
        repository.refresh()
    }

//    private fun handleUpdatedPriceType() {
//        _collectionItems.value.dataOrNull?.let { collections ->
//            val list = collections.map { collectionItem ->
//                val assets = collectionItem.assets.map { assetItem ->
//                    val coinPrice = when (priceType) {
//                        PriceType.Days7 -> assetItem.prices.average7d
//                        PriceType.Days30 -> assetItem.prices.average30d
//                        PriceType.LastPrice -> assetItem.prices.last
//                    }
//                    assetItem.copy(coinPrice = coinPrice)
//                }
//                collectionItem.copy(assets = assets)
//            }
//
//            _collectionItems.update {
//                DataState.Success(list)
//            }
//        }
//    }


}

class NftCollectionsRepository(
    private val nftManager: NftManager,
    private val accountManager: IAccountManager,
    private val nftItemFactory: NftItemFactory
) {
    private val _collectionRecords =
        MutableStateFlow<DataState<Map<NftCollectionRecord, List<NftAssetItem>>>>(DataState.Loading)
    val collectionRecords = _collectionRecords.asStateFlow()

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
            _collectionRecords.update {
                DataState.Error(NoActiveAccount())
            }
        }
    }

    private fun handleUpdatedCollectionAssets(collectionAssets: Map<NftCollectionRecord, List<NftAssetRecord>>) {
        _collectionRecords.update {
            DataState.Success(
                collectionAssets.map { (collection, assets) ->
                    val assetItems = assets.map { asset ->
                        nftItemFactory.createNftAssetItem(asset, collection.stats)
                    }
                    collection to assetItems
                }.toMap()
            )
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
        callback: suspend (value: Map<NftCollectionRecord, List<NftAssetRecord>>) -> Unit
    ) {
        handleActiveAccountJob = coroutineScope.launch {
            nftManager.getCollectionAndAssets(account.id)
                .collect(callback)
        }
    }
}
