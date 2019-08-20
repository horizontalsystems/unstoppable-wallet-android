package io.horizontalsystems.bankwallet.modules.send.submodules.address

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import java.math.BigDecimal

object SendAddressModule {

    interface IView {
        fun setAddress(address: String?)
        fun setAddressError(error: Exception?)
        fun setPasteButtonState(enabled: Boolean)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onAddressPasteClicked()
        fun onAddressDeleteClicked()
        fun onAddressScanClicked()
    }

    interface IInteractor {
        val addressFromClipboard: String?
        val clipboardHasPrimaryClip: Boolean

        fun parseAddress(address: String): Pair<String, BigDecimal?>
    }

    interface IInteractorDelegate

    interface IAddressModule {
        val address: String?

        fun didScanQrCode(address: String)
    }

    interface IAddressModuleDelegate {
        fun validate(address: String)

        fun onUpdateAddress()
        fun onUpdateAmount(amount: BigDecimal)

        fun scanQrCode()
    }

    fun init(view: SendAddressViewModel, coin: Coin, moduleDelegate: IAddressModuleDelegate?): SendAddressPresenter {
        val addressParser = App.addressParserFactory.parser(coin)
        val interactor = SendAddressInteractor(TextHelper, addressParser)
        val presenter = SendAddressPresenter(interactor)

        view.delegate = presenter

        presenter.view = view
        presenter.moduleDelegate = moduleDelegate

        interactor.delegate = presenter

        return presenter
    }

}
