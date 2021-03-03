package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CoinValueInfo
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CurrencyValueInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountInfo
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import java.math.BigInteger

class SendFeePresenter(
        val view: SendFeeModule.IView,
        private val interactor: SendFeeModule.IInteractor,
        private val helper: SendFeePresenterHelper,
        private val baseCoin: Coin,
        private val baseCurrency: Currency,
        private val feeCoinData: Pair<Coin, String>?,
        private val customPriorityUnit: CustomPriorityUnit?,
        private val feeRateAdjustmentHelper: FeeRateAdjustmentHelper)
    : ViewModel(), SendFeeModule.IViewDelegate, SendFeeModule.IFeeModule, SendFeeModule.IInteractorDelegate {

    var moduleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    private var xRate: BigDecimal? = null
    private var inputType = SendModule.InputType.COIN

    private var fee: BigDecimal = BigDecimal.ZERO
    private var availableFeeBalance: BigDecimal? = null

    private var error: Exception? = null

    private var customFeeRate: BigInteger? = null
    private var fetchedFeeRate: BigInteger? = null
    private var feeRatePriority: FeeRatePriority? = interactor.defaultFeeRatePriority
    private var feeRateAdjustmentInfo: FeeRateAdjustmentInfo = FeeRateAdjustmentInfo(SendAmountInfo.NotEntered, null, baseCurrency, null)

    private val coin: Coin
        get() = feeCoinData?.first ?: baseCoin

    private fun syncError() {

        if (error != null) {
            view.setError(error)
            return
        }

        try {
            validate()
            view.setInsufficientFeeBalanceError(null)
        } catch (e: SendFeeModule.InsufficientFeeBalance) {
            view.setInsufficientFeeBalanceError(e)
        }
    }

    private fun syncFees() {
        view.setPrimaryFee(helper.feeAmount(fee, inputType, xRate))
        view.setSecondaryFee(helper.feeAmount(fee, inputType.reversed(), xRate))
    }

    private fun syncFeeRateLabels() {
        view.showCustomFeePriority(feeRatePriority is FeeRatePriority.Custom)

        feeRatePriority?.let {
            view.setFeePriority(it)
        }
    }

    private fun validate() {
        val (feeCoin, coinProtocol) = feeCoinData ?: return
        val availableFeeBalance = availableFeeBalance ?: return

        if (availableFeeBalance < fee) {
            throw SendFeeModule.InsufficientFeeBalance(baseCoin, coinProtocol, feeCoin, CoinValue(feeCoin, fee))
        }
    }

    private fun updateCustomFeeParams(priority: FeeRatePriority.Custom) {
        customPriorityUnit ?: return

        val units = feeRate?.let { customPriorityUnit.getUnits(it).toInt() } ?: priority.value
        val minValue = units.coerceAtMost(priority.range.last)   // value can't be more than slider upper range
        val converted = customPriorityUnit.getConvertedValue(minValue.toLong())
        this.customFeeRate = converted.toBigInteger()

        view.setCustomFeeParams(units, priority.range, customPriorityUnit.getLabel())
    }

    private fun getSmartFee(): Long? {
        return fetchedFeeRate?.let {
            feeRateAdjustmentHelper.applyRule(coin.type, feeRateAdjustmentInfo, it.toLong())
        }
    }

    // SendFeeModule.IFeeModule

    override val isValid: Boolean
        get() = try {
            validate()
            true
        } catch (e: Exception) {
            false
        }

    override val feeRateState: FeeRateState
        get() {
            if (error != null) {
                return FeeRateState.Error(error as Exception)
            }
            feeRate?.let {
                return FeeRateState.Value(it)
            }

            return FeeRateState.Loading
        }


    override val primaryAmountInfo: AmountInfo
        get() {
            return when (inputType) {
                SendModule.InputType.COIN -> CoinValueInfo(CoinValue(coin, fee))
                SendModule.InputType.CURRENCY -> {
                    this.xRate?.let { xRate ->
                        CurrencyValueInfo(CurrencyValue(baseCurrency, fee * xRate))
                    } ?: throw Exception("Invalid state")
                }
            }
        }

    override val secondaryAmountInfo: AmountInfo?
        get() {
            return when (inputType.reversed()) {
                SendModule.InputType.COIN -> CoinValueInfo(CoinValue(coin, fee))
                SendModule.InputType.CURRENCY -> {
                    this.xRate?.let { xRate ->
                        CurrencyValueInfo(CurrencyValue(baseCurrency, fee * xRate))
                    }
                }
            }
        }

    override val feeRate: Long?
        get() = customFeeRate?.toLong() ?: getSmartFee()

    override fun setLoading(loading: Boolean) {
        view.setLoading(loading)
    }

    override fun setFee(fee: BigDecimal) {
        this.fee = fee
        syncFees()
        syncError()
    }

    override fun setError(externalError: Exception?) {
        this.error = externalError
        syncError()
    }

    override fun fetchFeeRate() {
        fetchedFeeRate = null
        error = null

        feeRatePriority?.let {
            interactor.syncFeeRate(it)
        }
    }

    override fun setAvailableFeeBalance(availableFeeBalance: BigDecimal) {
        this.availableFeeBalance = availableFeeBalance
        syncError()
    }

    override fun setInputType(inputType: SendModule.InputType) {
        this.inputType = inputType
        syncFees()
    }

    override fun setBalance(balance: BigDecimal) {
        feeRateAdjustmentInfo.balance = balance
    }

    override fun setRate(rate: BigDecimal?) {
        feeRateAdjustmentInfo.xRate = rate
    }

    override fun setAmountInfo(sendAmountInfo: SendAmountInfo) {
        feeRateAdjustmentInfo.amountInfo = sendAmountInfo
    }

    // SendFeeModule.IViewDelegate

    override fun onViewDidLoad() {
        xRate = interactor.getRate(coin.type)

        syncFeeRateLabels()
        syncFees()
        syncError()

        view.setAdjustableFeeVisible(interactor.feeRatePriorityList.isNotEmpty())
    }

    override fun onClickFeeRatePriority() {
        val items = interactor.feeRatePriorityList.map { priority ->
            SendFeeModule.FeeRateInfoViewItem(priority, priority == feeRatePriority)
        }
        view.showFeeRatePrioritySelector(items)
    }

    override fun onChangeFeeRate(feeRatePriority: FeeRatePriority) {
        if (feeRatePriority is FeeRatePriority.Custom) {
            updateCustomFeeParams(feeRatePriority)
        } else {
            customFeeRate = null
        }

        this.feeRatePriority = feeRatePriority

        syncFeeRateLabels()

        moduleDelegate?.onUpdateFeeRate()

        fetchFeeRate()
    }

    override fun onChangeFeeRateValue(value: Int) {
        customPriorityUnit ?: return

        val converted = customPriorityUnit.getConvertedValue(value.toLong())
        this.customFeeRate = converted.toBigInteger()
        moduleDelegate?.onUpdateFeeRate()
    }

    // IInteractorDelegate

    override fun didUpdate(feeRate: BigInteger) {
        this.fetchedFeeRate = feeRate
        moduleDelegate?.onUpdateFeeRate()
    }

    override fun didReceiveError(error: Exception) {
        this.error = error
        moduleDelegate?.onUpdateFeeRate()
    }

    override fun didUpdateExchangeRate(rate: BigDecimal) {
        xRate = rate
        syncFees()
    }

    // ViewModel

    override fun onCleared() {
        interactor.onClear()
    }
}
