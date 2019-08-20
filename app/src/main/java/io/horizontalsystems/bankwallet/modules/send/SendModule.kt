package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.SendConfirmationInfo
import io.horizontalsystems.bankwallet.modules.send.sendviews.fee.SendFeeModule
import java.math.BigDecimal

object SendModule {

    interface IView {
        fun loadInputItems(inputs: List<Input>)
        fun setSendButtonEnabled(enabled: Boolean)
        fun showConfirmation(viewItem: SendConfirmationInfo)
        fun dismissWithSuccess()
        fun showError(error: Throwable)
    }

    interface IViewDelegate {
        var amountModule: SendAmountModule.IAmountModule
        var addressModule: SendAddressModule.IAddressModule
        var feeModule: SendFeeModule.IFeeModule

        fun onViewDidLoad()
        fun onModulesDidLoad()
        fun onAddressScan(address: String)
        fun onSendClicked()
        fun onSendConfirmed(memo: String?)
        fun onClear()
    }

    interface ISendBitcoinInteractor {
        fun fetchAvailableBalance(feeRate: Long, address: String?)
        fun fetchFee(amount: BigDecimal, feeRate: Long, address: String?)
        fun validate(address: String)
        fun send(amount: BigDecimal, address: String, feeRate: Long)
        fun clear()
    }

    interface ISendBitcoinInteractorDelegate {
        fun didFetchAvailableBalance(availableBalance: BigDecimal)
        fun didFetchFee(fee: BigDecimal)
        fun didSend()
        fun didFailToSend(error: Throwable)
    }

    interface ISendEthereumInteractor {
        val ethereumBalance: BigDecimal

        fun availableBalance(gasPrice: Long): BigDecimal
        fun validate(address: String)
        fun fee(gasPrice: Long): BigDecimal
        fun send(amount: BigDecimal, address: String, gasPrice: Long)
        fun clear()
    }

    interface ISendEthereumInteractorDelegate {
        fun didSend()
        fun didFailToSend(error: Throwable)
    }

    interface IRouter {
        fun scanQrCode()
    }

    fun init(view: SendViewModel, wallet: Wallet): IViewDelegate {
        return when (val adapter = App.adapterManager.getAdapterForWallet(wallet)) {
            is ISendBitcoinAdapter -> {
                val interactor = SendBitcoinInteractor(adapter)
                val presenter = SendBitcoinPresenter(interactor, view, SendConfirmationViewItemFactory())

                presenter.view = view
                interactor.delegate = presenter

                view.amountModuleDelegate = presenter
                view.addressModuleDelegate = presenter
                view.feeModuleDelegate = presenter

                view.delegate = presenter

                presenter
            }
            is ISendEthereumAdapter -> {
                val interactor = SendEthereumInteractor(adapter)
                val presenter = SendEthereumPresenter(interactor, view, SendConfirmationViewItemFactory())

                presenter.view = view
                interactor.delegate = presenter

                view.amountModuleDelegate = presenter
                view.addressModuleDelegate = presenter
                view.feeModuleDelegate = presenter

                view.delegate = presenter

                presenter
            }
            else -> {
                throw Exception("No adapter found!")
            }
        }
    }

    enum class InputType {
        COIN, CURRENCY
    }

    enum class AdapterFields {
        CoinAmountInBigDecimal, CoinValue, CurrencyValue, Address, FeeRate, InputType, FeeCoinValue, FeeCurrencyValue, Memo
    }

    sealed class Input {
        object Amount : Input()
        object Address : Input()
        class Fee(val isAdjustable: Boolean) : Input()
        object SendButton : Input()
    }

    sealed class AmountInfo {
        data class CoinValueInfo(val coinValue: CoinValue) : AmountInfo()
        data class CurrencyValueInfo(val currencyValue: CurrencyValue) : AmountInfo()

        fun getFormatted(): String? = when (this) {
            is CoinValueInfo -> {
                App.numberFormatter.format(this.coinValue)
            }
            is CurrencyValueInfo -> {
                App.numberFormatter.format(this.currencyValue, trimmable = true)
            }
        }
    }

}
