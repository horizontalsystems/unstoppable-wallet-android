package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.core.orNull
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CurrencyValue
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
import java.math.BigDecimal

class NftCollectionsService(
    private val accountManager: IAccountManager,
    private val itemsRepository: NftAssetItemsRepository,
    private val itemsPricedRepository: NftAssetItemsPricedRepository,
    private val itemsPricedWithCurrencyRepository: NftAssetItemsPricedWithCurrencyRepository
) {
    val priceType by itemsPricedRepository::priceType

    private val _serviceItemState =
        MutableStateFlow<DataState<Pair<Map<NftCollectionRecord, List<NftAssetItemPricedWithCurrency>>, CurrencyValue>>>(
            DataState.Loading
        )
    val serviceItemState = _serviceItemState.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val disposables = CompositeDisposable()

    fun start() {
        coroutineScope.launch {
            itemsPricedWithCurrencyRepository.itemsFlow
                .collect { assetItemsPriced ->
                    val totalValue = assetItemsPriced.map { (_, assets) ->
                        assets.sumOf {
                            it.currencyPrice?.value?.multiply(it.assetItem.ownedCount.toBigDecimal()) ?: BigDecimal.ZERO
                        }
                    }.sumOf { it }

                    val totalCurrencyValue = CurrencyValue(itemsPricedWithCurrencyRepository.baseCurrency, totalValue)
                    _serviceItemState.update {
                        DataState.Success(Pair(assetItemsPriced, totalCurrencyValue))
                    }
                }
        }

        coroutineScope.launch {
            itemsPricedRepository.itemsFlow
                .collect { assetItemsPriced ->
                    itemsPricedWithCurrencyRepository.setItems(assetItemsPriced)
                }
        }

        coroutineScope.launch {
            itemsRepository.itemsFlow
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

    fun updatePriceType(priceType: PriceType) {
        itemsPricedRepository.setPriceType(priceType)
    }

    fun stop() {
        itemsRepository.stop()
        itemsPricedWithCurrencyRepository.stop()
    }

    suspend fun refresh() {
        itemsRepository.refresh()
        itemsPricedWithCurrencyRepository.refresh()
    }

    private fun handleAccount(account: Account?) {
        if (account != null) {
            itemsRepository.setAccount(account)
        } else {
            _serviceItemState.update {
                DataState.Error(NoActiveAccount())
            }
        }
    }
}
