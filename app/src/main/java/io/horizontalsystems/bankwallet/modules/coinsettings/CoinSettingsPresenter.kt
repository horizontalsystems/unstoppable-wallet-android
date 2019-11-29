package io.horizontalsystems.bankwallet.modules.coinsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*

class CoinSettingsPresenter(
        private val coin: Coin,
        private var coinSettings: CoinSettings,
        val view: CoinSettingsModule.IView,
        val router: CoinSettingsModule.IRouter
) : ViewModel(), CoinSettingsModule.IViewDelegate {

    override fun viewDidLoad() {
        view.setTitle(coin.title)

        for ((key, value) in coinSettings) {
            when (key) {
                CoinSetting.Derivation -> {
                    val derivation = AccountType.Derivation.valueOf(value)
                    view.update(derivation)
                }
                CoinSetting.SyncMode -> {
                    val syncMode = SyncMode.valueOf(value)
                    view.update(syncMode, coin.title)
                }
            }
        }
    }

    override fun onSelect(syncMode: SyncMode) {
        coinSettings[CoinSetting.SyncMode] = syncMode.value
        view.update(syncMode, coin.title)
    }

    override fun onSelect(derivation: AccountType.Derivation) {
        coinSettings[CoinSetting.Derivation] = derivation.value
        view.update(derivation)
    }

    override fun onDone() {
        router.notifyOptions(coinSettings, coin)
    }

    override fun onCancel() {
        router.onCancelClick()
    }
}
