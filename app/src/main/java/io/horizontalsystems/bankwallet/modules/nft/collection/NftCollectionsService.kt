package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.core.orNull
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NftCollectionsService(
    private val accountManager: IAccountManager,
    private val itemsRepository: NftAssetItemsRepository,
    private val itemsPricedRepository: NftAssetItemsPricedRepository,
    private val itemsPricedWithCurrencyRepository: NftAssetItemsPricedWithCurrencyRepository
) {
    val priceType by itemsPricedRepository::priceType

    private val _assetItemsPriced =
        MutableStateFlow<DataState<Map<NftCollectionRecord, List<NftAssetItemPricedWithCurrency>>>>(DataState.Loading)
    val assetItemsPriced = _assetItemsPriced.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val disposables = CompositeDisposable()

    fun updatePriceType(priceType: PriceType) {
        itemsPricedRepository.setPriceType(priceType)
    }

    fun start() {
        coroutineScope.launch {
            itemsPricedWithCurrencyRepository.items
                .collect { assetItemsPriced ->
                    _assetItemsPriced.update {
                        DataState.Success(assetItemsPriced)
                    }
                }
        }

        coroutineScope.launch {
            itemsPricedRepository.assetItemsPriced
                .collect { assetItemsPriced ->
                    itemsPricedWithCurrencyRepository.setItems(assetItemsPriced)
                }
        }

        coroutineScope.launch {
            itemsRepository.assetItems
                .collect {
                    itemsPricedRepository.setAssetItems(it)
                }
        }

        accountManager.activeAccountObservable
            .subscribeIO {
                handleAccount(it.orNull)
            }
            .let {
                disposables.add(it)
            }

        handleAccount(accountManager.activeAccount)
        itemsPricedWithCurrencyRepository.start()
    }

    private fun handleAccount(account: Account?) {
        if (account != null) {
            itemsRepository.setAccount(account)
        } else {
            _assetItemsPriced.update {
                DataState.Error(NoActiveAccount())
            }
        }
    }

    fun stop() {
        itemsRepository.stop()
        itemsPricedWithCurrencyRepository.stop()
    }

    suspend fun refresh() {
        itemsRepository.refresh()
        itemsPricedWithCurrencyRepository.refresh()
    }
}
