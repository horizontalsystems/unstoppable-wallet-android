package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem<String>>>()
    var showBirthdayHeightLiveEvent = SingleLiveEvent<BirthdayHeightViewItem>()

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        val viewItems = items.map { viewItem(it) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(
        item: ManageWalletsService.Item,
    ) = CoinViewItem(
        item = item.fullCoin.coin.uid,
        imageSource = ImageSource.Remote(item.fullCoin.coin.iconUrl, item.fullCoin.iconPlaceholder),
        title = item.fullCoin.coin.code,
        subtitle = item.fullCoin.coin.name,
        enabled = item.enabled,
        hasSettings = item.hasSettings,
        hasInfo = item.hasInfo
    )

    fun enable(fullCoin: FullCoin) {
        service.enable(fullCoin)
    }

    fun enable(uid: String) {
        service.enable(uid)
    }

    fun disable(uid: String) {
        service.disable(uid)
    }

    fun onClickSettings(uid: String) {
        service.configure(uid)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    fun onClickInfo(uid: String) {
        val (blockchain, birthdayHeight) = service.birthdayHeight(uid) ?: return
        showBirthdayHeightLiveEvent.postValue(
            BirthdayHeightViewItem(
                blockchainIcon = ImageSource.Remote(blockchain.type.imageUrl),
                blockchainName = blockchain.name,
                birthdayHeight = birthdayHeight.toString()
            )
        )
    }

    fun onCloseBirthdayHeight() {
        showBirthdayHeightLiveEvent.postValue(null)
    }

    private val accountTypeDescription: String
        get() = service.accountType?.description ?: ""

    val addTokenEnabled: Boolean
        get() = service.accountType?.canAddTokens ?: false

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    data class BirthdayHeightViewItem(
        val blockchainIcon: ImageSource,
        val blockchainName: String,
        val birthdayHeight: String
    )
}
