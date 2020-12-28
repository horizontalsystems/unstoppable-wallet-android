package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class DerivationSettingsViewModel(
        private val service: DerivationSettingsModule.IService,
        private val stringProvider: StringProvider
) : ViewModel() {

    val sections = MutableLiveData<List<DerivationSettingsModule.SectionItem>>()
    val showDerivationChangeAlert = SingleLiveEvent<Pair<String, String>>()

    private var disposables = CompositeDisposable()
    private var currentIndices: Pair<Int, Int>? = null

    init {
        service.itemsObservable
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
            is DerivationSettingsModule.ItemType.Derivation -> {
                val selectedDerivation = item.type.derivations[index]
                if (selectedDerivation != item.type.current) {
                    showDerivationChangeAlert.postValue(Pair(item.coinType.title, selectedDerivation.name.toUpperCase()))
                }
            }
            is DerivationSettingsModule.ItemType.BitcoinCashType -> {
                val selectedType = item.type.types[index]
                if (selectedType != item.type.current) {
                    showDerivationChangeAlert.postValue(Pair(item.coinType.title, stringProvider.string(selectedType.title)))
                }
            }
        }
    }

    fun onConfirm() {
        val (sectionIndex, index) = currentIndices ?: return

        val item = service.items[sectionIndex]

        when (item.type) {
            is DerivationSettingsModule.ItemType.Derivation -> {
                service.set(item.type.derivations[index], item.coinType)
            }
            is DerivationSettingsModule.ItemType.BitcoinCashType -> {
                service.set(item.type.types[index])
            }
        }
    }

    private fun sync(items: List<DerivationSettingsModule.Item>) {
        val sectionViewItems = items.map {
            DerivationSettingsModule.SectionItem(it.coinType.title, viewItems(it.type, it.coinType))
        }

        sections.postValue(sectionViewItems)
    }

    private fun viewItems(itemType: DerivationSettingsModule.ItemType, coinType: CoinType): List<DerivationSettingsModule.ViewItem> {
        return when (itemType) {
            is DerivationSettingsModule.ItemType.Derivation -> {
                itemType.derivations.map { derivation ->
                    val title = "${derivation.addressType()} - ${derivation.title()}"
                    val subtitle = stringProvider.string(derivation.description(), (derivation.addressPrefix(coinType)
                            ?: ""))
                    DerivationSettingsModule.ViewItem(title, subtitle, derivation == itemType.current)
                }
            }
            is DerivationSettingsModule.ItemType.BitcoinCashType -> {
                itemType.types.map { type ->
                    DerivationSettingsModule.ViewItem(stringProvider.string(type.title), stringProvider.string(type.description), type == itemType.current)
                }
            }
        }
    }

}
