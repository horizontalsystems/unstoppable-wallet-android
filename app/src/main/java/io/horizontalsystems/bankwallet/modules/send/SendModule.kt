package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceHandler
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceInteractor
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinHandler
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinInteractor
import io.horizontalsystems.bankwallet.modules.send.dash.SendDashHandler
import io.horizontalsystems.bankwallet.modules.send.dash.SendDashInteractor
import io.horizontalsystems.bankwallet.modules.send.ethereum.SendEthereumHandler
import io.horizontalsystems.bankwallet.modules.send.ethereum.SendEthereumInteractor
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.CustomPriorityUnit
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZcashHandler
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZcashInteractor
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.Single
import java.math.BigDecimal

object SendModule {

    interface IView {
        var delegate: IViewDelegate

        fun loadInputItems(inputs: List<Input>)
        fun setSendButtonEnabled(actionState: SendPresenter.ActionState)
        fun showConfirmation(confirmationViewItems: List<SendConfirmationViewItem>)
        fun showErrorInToast(error: Throwable)
    }

    interface IViewDelegate {
        var view: IView
        val handler: ISendHandler

        fun onViewDidLoad()
        fun onModulesDidLoad()
        fun onProceedClicked()
        fun onSendConfirmed(logger: AppLogger)
        fun onClear()
    }

    interface ISendBitcoinInteractor {
        val balance: BigDecimal
        val isLockTimeEnabled: Boolean

        fun fetchAvailableBalance(feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?)
        fun fetchMinimumAmount(address: String?): BigDecimal
        fun fetchMaximumAmount(pluginData: Map<Byte, IPluginData>): BigDecimal?
        fun fetchFee(amount: BigDecimal, feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?)
        fun validate(address: String, pluginData: Map<Byte, IPluginData>?)
        fun send(amount: BigDecimal, address: String, feeRate: Long, pluginData: Map<Byte, IPluginData>?, logger: AppLogger): Single<Unit>
        fun clear()
    }

    interface ISendBitcoinInteractorDelegate {
        fun didFetchAvailableBalance(availableBalance: BigDecimal)
        fun didFetchFee(fee: BigDecimal)
    }

    interface ISendDashInteractor {
        fun fetchAvailableBalance(address: String?)
        fun fetchMinimumAmount(address: String?): BigDecimal
        fun fetchFee(amount: BigDecimal, address: String?)
        fun validate(address: String)
        fun send(amount: BigDecimal, address: String, logger: AppLogger): Single<Unit>
        fun clear()
    }

    interface ISendDashInteractorDelegate {
        fun didFetchAvailableBalance(availableBalance: BigDecimal)
        fun didFetchFee(fee: BigDecimal)
    }

    interface ISendEthereumInteractor {
        val balance: BigDecimal
        val ethereumBalance: BigDecimal
        val minimumRequiredBalance: BigDecimal
        val minimumAmount: BigDecimal

        fun availableBalance(gasPrice: Long, gasLimit: Long): BigDecimal
        fun validate(address: String)
        fun fee(gasPrice: Long, gasLimit: Long): BigDecimal
        fun send(amount: BigDecimal, address: String, gasPrice: Long, gasLimit: Long, logger: AppLogger): Single<Unit>
        fun estimateGasLimit(toAddress: String?, value: BigDecimal, gasPrice: Long?): Single<Long>

    }

    interface ISendBinanceInteractor {
        val availableBalance: BigDecimal
        val availableBinanceBalance: BigDecimal
        val fee: BigDecimal

        fun validate(address: String)
        fun send(amount: BigDecimal, address: String, memo: String?, logger: AppLogger): Single<Unit>
    }

    interface ISendZcashInteractor {
        val availableBalance: BigDecimal
        val fee: BigDecimal

        fun validate(address: String)
        fun send(amount: BigDecimal, address: String, memo: String?, logger: AppLogger): Single<Unit>
    }

    interface IRouter {
        fun closeWithSuccess()
    }

    interface ISendInteractor {
        var delegate: ISendInteractorDelegate

        fun send(sendSingle: Single<Unit>, logger: AppLogger)
        fun clear()
    }

    interface ISendInteractorDelegate {
        fun sync()
        fun didSend()
        fun didFailToSend(error: Throwable)
    }

    interface ISendHandler {
        var amountModule: SendAmountModule.IAmountModule
        var addressModule: SendAddressModule.IAddressModule
        var feeModule: SendFeeModule.IFeeModule
        var memoModule: SendMemoModule.IMemoModule
        var hodlerModule: SendHodlerModule.IHodlerModule?

        val inputItems: List<Input>
        var delegate: ISendHandlerDelegate

        fun sync()
        fun onModulesDidLoad()
        fun onClear() {}

        @Throws
        fun confirmationViewItems(): List<SendConfirmationViewItem>
        fun sendSingle(logger: AppLogger): Single<Unit>
    }

    interface ISendHandlerDelegate {
        fun onChange(isValid: Boolean, amountError: Throwable?, addressError: Throwable?)
    }

    abstract class SendConfirmationViewItem

    data class SendConfirmationAmountViewItem(
            val primaryInfo: AmountInfo,
            val secondaryInfo: AmountInfo?,
            val receiver: Address,
            val locked: Boolean = false
    ) : SendConfirmationViewItem()

    data class SendConfirmationFeeViewItem(
            val primaryInfo: AmountInfo,
            val secondaryInfo: AmountInfo?
    ) : SendConfirmationViewItem()

    data class SendConfirmationTotalViewItem(
            val primaryInfo: AmountInfo,
            val secondaryInfo: AmountInfo?
    ) : SendConfirmationViewItem()

    data class SendConfirmationMemoViewItem(val memo: String?) : SendConfirmationViewItem()

    data class SendConfirmationLockTimeViewItem(val lockTimeInterval: LockTimeInterval) : SendConfirmationViewItem()

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendView()
            val interactor: ISendInteractor = SendInteractor()
            val router = SendRouter()
            val presenter = SendPresenter(interactor, router)

            val handler: ISendHandler = when (val adapter = App.adapterManager.getAdapterForWallet(wallet)) {
                is ISendBitcoinAdapter -> {
                    val bitcoinInteractor = SendBitcoinInteractor(adapter, App.localStorage)
                    val handler = SendBitcoinHandler(bitcoinInteractor, wallet.coin.type)

                    bitcoinInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler
                    presenter.hodlerModuleDelegate = handler
                    presenter.customPriorityUnit = CustomPriorityUnit.Satoshi

                    handler
                }
                is ISendDashAdapter -> {
                    val dashInteractor = SendDashInteractor(adapter)
                    val handler = SendDashHandler(dashInteractor)

                    dashInteractor.delegate = handler

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }
                is ISendEthereumAdapter -> {
                    val ethereumInteractor = SendEthereumInteractor(adapter)
                    val handler = SendEthereumHandler(ethereumInteractor)

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler
                    presenter.customPriorityUnit = CustomPriorityUnit.Gwei

                    handler
                }
                is ISendBinanceAdapter -> {
                    val binanceInteractor = SendBinanceInteractor(adapter)
                    val handler = SendBinanceHandler(binanceInteractor)

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }
                is ISendZcashAdapter -> {
                    val zcashInteractor = SendZcashInteractor(adapter)
                    val handler = SendZcashHandler(zcashInteractor)

                    presenter.amountModuleDelegate = handler
                    presenter.addressModuleDelegate = handler
                    presenter.feeModuleDelegate = handler

                    handler
                }
                else -> {
                    throw Exception("No adapter found!")
                }
            }

            presenter.view = view
            presenter.handler = handler

            view.delegate = presenter
            handler.delegate = presenter
            interactor.delegate = presenter

            return presenter as T
        }
    }

    enum class InputType {
        COIN, CURRENCY;

        fun reversed(): InputType {
            return if (this == COIN) CURRENCY else COIN
        }
    }

    sealed class Input {
        object Amount : Input()
        class Address(val editable: Boolean = false) : Input()
        object Fee : Input()
        class Memo(val maxLength: Int) : Input()
        object ProceedButton : Input()
        object Hodler : Input()
    }

    data class AmountData(val primary: AmountInfo, val secondary: AmountInfo?) {
        fun getFormatted(): String {
            var formatted = primary.getFormatted()

            secondary?.let {
                formatted += "  |  " + it.getFormatted()
            }

            return formatted
        }
    }

    sealed class AmountInfo {
        data class CoinValueInfo(val coinValue: CoinValue) : AmountInfo()
        data class CurrencyValueInfo(val currencyValue: CurrencyValue) : AmountInfo()

        val value: BigDecimal
            get() = when (this) {
                is CoinValueInfo -> coinValue.value
                is CurrencyValueInfo -> currencyValue.value
            }

        val decimal: Int
            get() = when (this) {
                is CoinValueInfo -> coinValue.coin.decimal
                is CurrencyValueInfo -> currencyValue.currency.decimal
            }

        fun getAmountName(): String = when (this) {
            is CoinValueInfo -> coinValue.coin.title
            is CurrencyValueInfo -> currencyValue.currency.code
        }

        fun getFormatted(): String = when (this) {
            is CoinValueInfo -> {
                coinValue.getFormatted()
            }
            is CurrencyValueInfo -> {
                App.numberFormatter.formatFiat(currencyValue.value, currencyValue.currency.symbol, 2, 2)
            }
        }

        fun getFormattedForTxInfo(): String = when (this) {
            is CoinValueInfo -> {
                coinValue.getFormatted()
            }
            is CurrencyValueInfo -> {
                val significantDecimal = App.numberFormatter.getSignificantDecimalFiat(currencyValue.value)

                App.numberFormatter.formatFiat(currencyValue.value, currencyValue.currency.symbol, 0, significantDecimal)
            }
        }


    }

}
