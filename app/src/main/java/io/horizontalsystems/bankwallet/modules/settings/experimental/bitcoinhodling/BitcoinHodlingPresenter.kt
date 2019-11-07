package io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling

import androidx.lifecycle.ViewModel

class BitcoinHodlingPresenter(
        val view: BitcoinHodlingModule.IView,
        private val interactor: BitcoinHodlingModule.IInteractor
) : ViewModel(), BitcoinHodlingModule.IViewDelegate {

    // IViewDelegate

    override fun onLoad() {
        view.setLockTime(interactor.isLockTimeEnabled)
    }

    override fun onSwitchLockTime(enabled: Boolean) {
        interactor.isLockTimeEnabled = enabled
    }

}
