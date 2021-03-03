package io.horizontalsystems.bankwallet.modules.addressformat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class AddressFormatViewModel(
        private val service: AddressFormatModule.IService,
        private val stringProvider: StringProvider
) : ViewModel() {

    val sections = MutableLiveData<List<AddressFormatModule.SectionItem>>()
    val showAddressFormatChangeAlert = SingleLiveEvent<Pair<String, String>>()

    private var disposables = CompositeDisposable()
    private var currentIndices: Pair<Int, Int>? = null

    init {
        service.itemsAsync
                .subscribeOn(Schedulers.io())
                .subscribe {
                    sync(it)
                }
                .let { disposables.add(it) }


        sync(service.items)
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onSelect(sectionIndex: Int, index: Int) {
        val item = service.items[sectionIndex]
        currentIndices = Pair(sectionIndex, index)

        when (item.type) {
            is AddressFormatModule.ItemType.Derivation -> {
                val selectedDerivation = item.type.derivations[index]
                if (selectedDerivation != item.type.current) {
                    showAddressFormatChangeAlert.postValue(Pair(item.coinType.title, selectedDerivation.name.toUpperCase()))
                }
            }
            is AddressFormatModule.ItemType.BitcoinCashType -> {
                val selectedType = item.type.types[index]
                if (selectedType != item.type.current) {
                    showAddressFormatChangeAlert.postValue(Pair(item.coinType.title, stringProvider.string(selectedType.title)))
                }
            }
        }
    }

    fun onConfirm() {
        val (sectionIndex, index) = currentIndices ?: return

        val item = service.items[sectionIndex]

        when (item.type) {
            is AddressFormatModule.ItemType.Derivation -> {
                service.set(item.type.derivations[index], item.coinType)
            }
            is AddressFormatModule.ItemType.BitcoinCashType -> {
                service.set(item.type.types[index])
            }
        }
    }

    private fun sync(items: List<AddressFormatModule.Item>) {
        val sectionViewItems = items.map {
            AddressFormatModule.SectionItem(it.coinType.title, viewItems(it.type, it.coinType))
        }

        sections.postValue(sectionViewItems)
    }

    private fun viewItems(itemType: AddressFormatModule.ItemType, coinType: CoinType): List<AddressFormatModule.ViewItem> {
        return when (itemType) {
            is AddressFormatModule.ItemType.Derivation -> {
                itemType.derivations.mapIndexed { index, derivation ->
                    val title = "${derivation.addressType()} - ${derivation.title()}"
                    val subtitle = stringProvider.string(derivation.description(), (derivation.addressPrefix(coinType)
                            ?: ""))
                    AddressFormatModule.ViewItem(
                            title,
                            subtitle,
                            derivation == itemType.current,
                            listPosition = ListPosition.getListPosition(itemType.derivations.size, index)
                    )
                }
            }
            is AddressFormatModule.ItemType.BitcoinCashType -> {
                itemType.types.mapIndexed { index, type ->
                    AddressFormatModule.ViewItem(
                            stringProvider.string(type.title),
                            stringProvider.string(type.description),
                            type == itemType.current,
                            listPosition = ListPosition.getListPosition(itemType.types.size, index)
                    )
                }
            }
        }
    }

}
