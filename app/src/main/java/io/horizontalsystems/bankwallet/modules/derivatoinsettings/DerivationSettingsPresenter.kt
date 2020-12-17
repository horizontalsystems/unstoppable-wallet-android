package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.*

class DerivationSettingsPresenter(
        val view: DerivationSettingsModule.IView,
        private val interactor: DerivationSettingsModule.IInteractor,
        private val stringProvider: StringProvider)
    : ViewModel(), DerivationSettingsModule.IViewDelegate {

    private var items = listOf<DerivationSettingsItem>()

    override fun onViewLoad() {
        updateUi()
    }

    override fun onSelect(sectionIndex: Int, settingIndex: Int) {
        val item = items[sectionIndex]
        val derivation = AccountType.Derivation.values()[settingIndex]

        if (item.setting.derivation != derivation){
            val newSetting = DerivationSetting(item.setting.coinType, derivation)
            view.showDerivationChangeAlert(newSetting, item.coin.title)
        }
    }

    override fun onConfirm(derivationSetting: DerivationSetting) {
        interactor.saveDerivation(derivationSetting)
        updateUi()
    }

    private fun updateUi() {
        items = interactor.allActiveSettings.map { (setting, coin) ->
            DerivationSettingsItem(coin, setting)
        }

        val viewItems = items.map { sectionViewItem(it) }
        view.setViewItems(viewItems)
    }

    //region factory

    private fun sectionViewItem(item: DerivationSettingsItem): DerivationSettingSectionViewItem {
        return DerivationSettingSectionViewItem(item.coin.title, AccountType.Derivation.values().map {
            DerivationSettingViewItem(
                    it.longTitle(),
                    stringProvider.string(it.description(), (it.addressPrefix(item.coin.type) ?: "")),
                    it == item.setting.derivation)
        }
        )
    }

    //endregion

}
