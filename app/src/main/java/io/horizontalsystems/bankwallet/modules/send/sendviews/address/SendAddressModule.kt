package io.horizontalsystems.bankwallet.modules.send.sendviews.address

import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.modules.send.SendModule
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
        fun onViewDidLoad()
        fun onAddressScan(address: String)
        fun onPasteButtonClick()
        fun onAddressDeleteClick()
        fun onParsedAddress(parsedAddress: PaymentRequestAddress)
        fun getAddress(): String?
    }

    interface IInteractor {
//        fun parseAddress(address: String): PaymentRequestAddress
//        fun validate(address: String)
        val addressFromClipboard: String?
        val clipboardHasPrimaryClip: Boolean
    }

    interface IInteractorDelegate {

    }

    class AddressError : Exception() {
        class InvalidAddress : SendModule.AddressError()
    }

    fun init(view: SendAddressViewModel) {
//        val adapter = App.adapterManager.adapters.first { it.wallet.coin.code == coinCode }
        val interactor = SendAddressInteractor(TextHelper)
        val presenter = SendAddressPresenter(interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
