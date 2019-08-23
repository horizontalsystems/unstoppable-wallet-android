package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceInteractor
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinancePresenter
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinInteractor
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinPresenter
import io.horizontalsystems.bankwallet.modules.send.dash.SendDashInteractor
import io.horizontalsystems.bankwallet.modules.send.dash.SendDashPresenter
import io.horizontalsystems.bankwallet.modules.send.eos.SendEosInteractor
import io.horizontalsystems.bankwallet.modules.send.eos.SendEosPresenter
import io.horizontalsystems.bankwallet.modules.send.ethereum.SendEthereumInteractor
import io.horizontalsystems.bankwallet.modules.send.ethereum.SendEthereumPresenter
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.SendConfirmationInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import java.math.BigDecimal

object SendModule {

    interface IView {
        fun loadInputItems(inputs: List<Input>)
        fun setSendButtonEnabled(enabled: Boolean)
        fun showConfirmation(viewItem: SendConfirmationInfo)
        fun showErrorInToast(error: Throwable)
        fun showErrorInDialog(coinException: CoinException)
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

    interface ISendDashInteractor {
        fun fetchAvailableBalance( address: String?)
        fun fetchFee(amount: BigDecimal, address: String?)
        fun validate(address: String)
        fun send(amount: BigDecimal, address: String)
        fun clear()
    }

    interface ISendDashInteractorDelegate {
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

    interface ISendBinanceInteractor {
        val availableBalance: BigDecimal
        val availableBinanceBalance: BigDecimal
        val fee: BigDecimal

        fun validate(address: String)
        fun send(amount: BigDecimal, address: String, memo: String?)
        fun clear()
    }

    interface ISendBinanceInteractorDelegate {
        fun didSend()
        fun didFailToSend(error: Throwable)
    }

    interface ISendEosInteractor {
        val availableBalance: BigDecimal

        fun validate(account: String)
        fun send(amount: BigDecimal, account: String, memo: String?)
        fun clear()
    }

    interface ISendEosInteractorDelegate {
        fun didSend()
        fun didFailToSend(error: Throwable)
        fun didFailToSendWithEosBackendError(coinException: CoinException)
    }

    interface IRouter {
        fun scanQrCode()
        fun closeWithSuccess()
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
            is ISendDashAdapter -> {
                val interactor = SendDashInteractor(adapter)
                val presenter = SendDashPresenter(interactor, view, SendConfirmationViewItemFactory())

                presenter.view = view
                interactor.delegate = presenter

                view.amountModuleDelegate = presenter
                view.addressModuleDelegate = presenter

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
            is ISendBinanceAdapter -> {
                val interactor = SendBinanceInteractor(adapter)
                val presenter = SendBinancePresenter(interactor, view, SendConfirmationViewItemFactory())

                presenter.view = view
                interactor.delegate = presenter

                view.amountModuleDelegate = presenter
                view.addressModuleDelegate = presenter

                view.delegate = presenter

                presenter
            }
            is ISendEosAdapter -> {
                val interactor = SendEosInteractor(adapter)
                val presenter = SendEosPresenter(interactor, view, SendConfirmationViewItemFactory(), wallet.coin.decimal)

                presenter.view = view
                interactor.delegate = presenter

                view.amountModuleDelegate = presenter
                view.addressModuleDelegate = presenter

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
