package io.horizontalsystems.bankwallet.modules.send.sendviews.address

import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import java.math.BigDecimal

object SendAddressModule {

    interface IView{
        fun onAmountChange(amount: BigDecimal)
        fun setAddress(address: String?)
        fun setAddressError(error: Exception?)
        fun setPasteButtonState(enabled: Boolean)
        fun notifyMainViewModelAddressUpdated()
        fun parseAddressInMainViewModel(address: String)
    }

    interface IViewDelegate {
        val validState: Boolean

        fun onViewDidLoad()
        fun onAddressScan(address: String)
        fun onPasteButtonClick()
        fun onAddressDeleteClick()
        fun onParsedAddress(parsedAddress: PaymentRequestAddress)
        fun getAddress(): String?
    }

    interface IInteractor {
        val addressFromClipboard: String?
        val clipboardHasPrimaryClip: Boolean
    }

    interface IInteractorDelegate {

    }

    fun init(view: SendAddressViewModel) {
        val interactor = SendAddressInteractor(TextHelper)
        val presenter = SendAddressPresenter(interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
