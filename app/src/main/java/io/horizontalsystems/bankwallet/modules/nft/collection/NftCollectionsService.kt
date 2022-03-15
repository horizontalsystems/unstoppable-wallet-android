package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.NoActiveAccount
import io.horizontalsystems.bankwallet.core.orNull
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.nft.DataWithError
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

class NftCollectionsService(
    private val accountManager: IAccountManager,
    private val itemsRepository: NftAssetItemsRepository,
    private val itemsPricedRepository: NftAssetItemsPricedRepository,
    private val itemsPricedWithCurrencyRepository: NftAssetItemsPricedWithCurrencyRepository
) {
    val priceType by itemsPricedRepository::priceType

    private val _serviceItemDataFlow =
        MutableSharedFlow<DataWithError<Pair<Map<NftCollectionRecord, List<NftAssetItemPricedWithCurrency>>, CurrencyValue>?>>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val serviceItemDataFlow = _serviceItemDataFlow.asSharedFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val disposables = CompositeDisposable()

    suspend fun start() {
        coroutineScope.launch {
            itemsPricedWithCurrencyRepository.itemsDataFlow
                .collect { data ->
                    val itemsWithTotalValue = data.value?.let { assetItemsPriced ->
                        val totalValue = assetItemsPriced.map { (_, assets) ->
                            assets.sumOf {
                                it.currencyPrice?.value ?: BigDecimal.ZERO
                            }
                        }.sumOf { it }
                        Pair(
                            assetItemsPriced,
                            CurrencyValue(itemsPricedWithCurrencyRepository.baseCurrency, totalValue)
                        )
                    }
                    _serviceItemDataFlow.tryEmit(DataWithError(itemsWithTotalValue, data.error))
                }
        }

        coroutineScope.launch {
            itemsPricedRepository.itemsDataFlow
                .collect { assetItemsPriced ->
                    itemsPricedWithCurrencyRepository.setItems(assetItemsPriced)
                }
        }

        coroutineScope.launch {
            itemsRepository.itemsDataFlow
                .collect {
                    itemsPricedRepository.setAssetItems(it)
                }
        }

        accountManager.activeAccountObservable
            .subscribeIO {
                coroutineScope.launch {
                    handleAccount(it.orNull)
                }
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
        itemsPricedWithCurrencyRepository.stop()
    }

    suspend fun refresh() {
        itemsRepository.refresh()
        itemsPricedWithCurrencyRepository.refresh()
    }

    private suspend fun handleAccount(account: Account?) {
        if (account != null) {
            itemsRepository.setAccount(account)
        } else {
            _serviceItemDataFlow.tryEmit(DataWithError(null, NoActiveAccount()))
        }
    }
}
