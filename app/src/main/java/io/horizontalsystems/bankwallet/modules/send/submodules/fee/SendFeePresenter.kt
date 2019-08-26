package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal


class SendFeePresenter(
        private val interactor: SendFeeModule.IInteractor,
        private val helper: SendFeePresenterHelper,
        private val baseCoin: Coin,
        private val baseCurrency: Currency,
        private val feeCoinData: Pair<Coin, String>?)
    : SendFeeModule.IViewDelegate, SendFeeModule.IInteractorDelegate, SendFeeModule.IFeeModule {

    var view: SendFeeModule.IView? = null
    var moduleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    private var xRate: Rate? = null

    private var fee: BigDecimal = BigDecimal.ZERO
    private var availableFeeBalance: BigDecimal? = null

    private var feeRates: List<FeeRateInfo>? = null
    private var feeRateInfo = FeeRateInfo(FeeRatePriority.MEDIUM, 0, 0)

    private val coin: Coin
        get() = feeCoinData?.first ?: baseCoin

    private fun syncError() {
        try {
            validate()
            view?.setInsufficientFeeBalanceError(null)
        } catch (e: SendFeeModule.InsufficientFeeBalance) {
            view?.setInsufficientFeeBalanceError(e)
        }
    }

    private fun syncFeeLabels() {
        view?.setPrimaryFee(helper.feeAmount(fee, SendModule.InputType.COIN, xRate))
        view?.setSecondaryFee(helper.feeAmount(fee, SendModule.InputType.CURRENCY, xRate))
    }

    private fun syncFeeRateLabels() {
        view?.setDuration(helper.duration(feeRateInfo.duration))
        view?.setFeePriority(helper.priority(feeRateInfo.priority))
    }

    private fun validate() {
        val (feeCoin, coinProtocol) = feeCoinData ?: return
        val availableFeeBalance = availableFeeBalance ?: return

        if (availableFeeBalance < fee) {
            throw SendFeeModule.InsufficientFeeBalance(baseCoin, coinProtocol, feeCoin, CoinValue(feeCoin.code, fee))
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

    override val feeRate
        get() = feeRateInfo.feeRate

    override val coinValue: CoinValue
        get() = CoinValue(coin.code, fee)

    override val currencyValue: CurrencyValue?
        get() = xRate?.let { CurrencyValue(baseCurrency, fee.multiply(it.value)) }

    override val duration: String?
        get() = helper.duration(feeRateInfo.duration)

    override fun setFee(fee: BigDecimal) {
        this.fee = fee
        syncFeeLabels()
        syncError()
    }

    override fun setAvailableFeeBalance(availableFeeBalance: BigDecimal) {
        this.availableFeeBalance = availableFeeBalance
        syncError()
    }

    override fun setInputType(inputType: SendModule.InputType) {}

    // SendFeeModule.IViewDelegate

    override fun onViewDidLoad() {
        interactor.getRate(coin.code)

        feeRates = interactor.getFeeRates()

        feeRates?.find { it.priority == FeeRatePriority.MEDIUM }?.let {
            feeRateInfo = it
        }

        syncFeeRateLabels()
        syncFeeLabels()
        syncError()
    }

    override fun onClickFeeRatePriority() {
        feeRates?.let {
            view?.showFeeRatePrioritySelector(it.map { rateInfo ->
                feeRateInfoViewItem(rateInfo)
            })
        }
    }

    private fun feeRateInfoViewItem(rateInfo: FeeRateInfo): SendFeeModule.FeeRateInfoViewItem {
        return SendFeeModule.FeeRateInfoViewItem(
                title = "${helper.priority(rateInfo.priority)} (~${helper.duration(rateInfo.duration)})",
                feeRateInfo = rateInfo,
                selected = rateInfo.priority == feeRateInfo.priority)
    }

    override fun onChangeFeeRate(feeRateInfo: FeeRateInfo) {
        this.feeRateInfo = feeRateInfo

        syncFeeRateLabels()

        moduleDelegate?.onUpdateFeeRate(feeRate)
    }

    override fun onClear() {
        interactor.clear()
    }

    // SendFeeModule.IInteractorDelegate

    override fun onRateFetched(latestRate: Rate?) {
        xRate = latestRate
        syncFeeLabels()
        syncError()
    }

}
