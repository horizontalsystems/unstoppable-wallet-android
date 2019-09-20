package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import java.math.BigDecimal

object SendAddressModule {

    interface View {
        fun setAddress(address: String?)
        fun setAddressError(error: Exception?)
        fun setPasteButtonState(enabled: Boolean)
    }

    interface ViewDelegate {
        fun onViewDidLoad()
        fun onAddressPasteClicked()
        fun onAddressDeleteClicked()
        fun onAddressScanClicked()
    }

    interface Interactor {
        val addressFromClipboard: String?
        val clipboardHasPrimaryClip: Boolean

        fun parseAddress(address: String): Pair<String, BigDecimal?>
    }

    interface InteractorDelegate

    interface IAddressModule {
        val currentAddress: String?

        @Throws
        fun validAddress(): String
        fun didScanQrCode(address: String)
    }

    interface IAddressModuleDelegate {
        fun validate(address: String)

        fun onUpdateAddress()
        fun onUpdateAmount(amount: BigDecimal)

        fun scanQrCode()
    }

    open class ValidationError : Exception() {
        class InvalidAddress : ValidationError()
    }


    class Factory(private val coin: Coin) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendAddressViewModel()
            val addressParser = App.addressParserFactory.parser(coin)
            val interactor = SendAddressInteractor(TextHelper, addressParser)
            val presenter = SendAddressPresenter(view, interactor)

            interactor.delegate = presenter

            return presenter as T
        }
    }

}
