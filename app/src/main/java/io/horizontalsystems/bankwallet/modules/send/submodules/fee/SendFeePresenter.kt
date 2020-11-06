package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CoinValueInfo
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CurrencyValueInfo
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import kotlin.math.min

class SendFeePresenter(
        val view: SendFeeModule.IView,
        private val interactor: SendFeeModule.IInteractor,
        private val helper: SendFeePresenterHelper,
        private val baseCoin: Coin,
        private val baseCurrency: Currency,
        private val feeCoinData: Pair<Coin, String>?,
        private val customPriorityUnit: CustomPriorityUnit?)
    : ViewModel(), SendFeeModule.IViewDelegate, SendFeeModule.IFeeModule, SendFeeModule.IInteractorDelegate {

    var moduleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    private var xRate: BigDecimal? = null
    private var inputType = SendModule.InputType.COIN

    private var fee: BigDecimal = BigDecimal.ZERO
    private var availableFeeBalance: BigDecimal? = null
    private var feeRateInfo = FeeRateInfo(FeeRatePriority.MEDIUM, 1)
    private var error: Exception? = null

    private var feeRates: List<FeeRateInfo>? = null
        set(value) {
            field = value
            value?.let {
                getFeeRateInfoByPriority(it, FeeRatePriority.MEDIUM, FeeRatePriority.RECOMMENDED)?.let { feeInfo ->
                    feeRateInfo = feeInfo
                    syncFeeRateLabels()
                }
            }
        }

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
        if (feeRateInfo.priority is FeeRatePriority.Custom) {
            view.showCustomFeePriority(true)
        } else {
            view.showCustomFeePriority(false)
            view.setDuration(feeRateInfo.duration)
        }

        view.setFeePriority(feeRateInfo.priority)
    }

    private fun updateCustomFeeParams(priority: FeeRatePriority.Custom) {
        val selectedValue = customPriorityUnit?.getUnits(feeRateInfo.feeRate)?.toInt() ?: priority.range.last
        val value = min(selectedValue, priority.range.last)
        view.setCustomFeeParams(value, priority.range, customPriorityUnit?.getLabel())
    }

    private fun validate() {
        val (feeCoin, coinProtocol) = feeCoinData ?: return
        val availableFeeBalance = availableFeeBalance ?: return

        if (availableFeeBalance < fee) {
            throw SendFeeModule.InsufficientFeeBalance(baseCoin, coinProtocol, feeCoin, CoinValue(feeCoin, fee))
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

    override val feeRateState: FeeState
        get() {
            if (error != null) {
                return FeeState.Error(error as Exception)
            }
            if (feeRates != null) {
                return FeeState.Value(feeRateInfo.feeRate)
            }

            return FeeState.Loading
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

    override val feeRate: Long
        get() = feeRateInfo.feeRate

    override val duration: Long?
        get() = feeRateInfo.duration

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
        feeRates = null
        error = null

        interactor.syncFeeRate()
    }

    override fun setAvailableFeeBalance(availableFeeBalance: BigDecimal) {
        this.availableFeeBalance = availableFeeBalance
        syncError()
    }

    override fun setInputType(inputType: SendModule.InputType) {
        this.inputType = inputType
        syncFees()
    }

    // SendFeeModule.IViewDelegate

    override fun onViewDidLoad() {
        xRate = interactor.getRate(coin.code)

        syncFeeRateLabels()
        syncFees()
        syncError()
    }

    override fun onClickFeeRatePriority() {
        feeRates?.let {
            view.showFeeRatePrioritySelector(it.map { rateInfo ->
                feeRateInfoViewItem(rateInfo)
            })
        }
    }

    private fun getFeeRateInfoByPriority(searchList: List<FeeRateInfo>, vararg priorities: FeeRatePriority): FeeRateInfo? {
        priorities.forEach { feeRatePriority ->
            searchList.find { it.priority == feeRatePriority }?.let {
                return it
            }
        }
        return null
    }

    private fun feeRateInfoViewItem(rateInfo: FeeRateInfo): SendFeeModule.FeeRateInfoViewItem {
        return SendFeeModule.FeeRateInfoViewItem(feeRateInfo = rateInfo,
                selected = rateInfo.priority == feeRateInfo.priority)
    }

    override fun onChangeFeeRate(feeRateInfo: FeeRateInfo) {
        if (feeRateInfo.priority is FeeRatePriority.Custom) {
            updateCustomFeeParams(feeRateInfo.priority)
        }

        this.feeRateInfo = feeRateInfo

        syncFeeRateLabels()

        moduleDelegate?.onUpdateFeeRate()
    }

    override fun onChangeFeeRateValue(value: Long) {
        if (feeRateInfo.priority is FeeRatePriority.Custom) {
            feeRateInfo.feeRate = customPriorityUnit?.getConvertedValue(value) ?: value

            moduleDelegate?.onUpdateFeeRate()
        }
    }

    // IInteractorDelegate

    override fun didUpdate(feeRates: List<FeeRateInfo>) {
        this.feeRates = feeRates
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
