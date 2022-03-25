package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.FeeRateState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountInfo
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.BigInteger

class SendFeePresenter(
    private val helper: SendFeePresenterHelper,
    private val baseCoin: PlatformCoin,
    private val feeCoinData: Pair<PlatformCoin, String>?,
    private val customPriorityUnit: CustomPriorityUnit?,
    private val feeRateAdjustmentHelper: FeeRateAdjustmentHelper,

    private val baseCurrency: Currency,
    private val marketKit: MarketKit,
    private val feeRateProvider: IFeeRateProvider?,
    private val interactorPlatformCoin: PlatformCoin
) : ViewModel(), SendFeeModule.IFeeModule {

    val showAdjustableFeeMenu = MutableLiveData<Boolean>()
    val primaryFee = MutableLiveData<String?>()
    val secondaryFee = MutableLiveData<String?>()
    val feePriority = MutableLiveData<FeeRatePriority>()
    val showFeePriorityOptions = MutableLiveData<List<SendFeeModule.FeeRateInfoViewItem>>()
    val showCustomFeePriority = SingleLiveEvent<Boolean>()
    val setCustomFeeParams = SingleLiveEvent<Triple<Int, IntRange, String?>>()
    val insufficientFeeBalanceError = SingleLiveEvent<SendFeeModule.InsufficientFeeBalance?>()
    val setLoading = MutableLiveData<Boolean>()
    val setError = MutableLiveData<Exception>()
    val showLowFeeWarningLiveData = MutableLiveData<Boolean>()

    var moduleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    private var xRate: BigDecimal? = null
    private var inputType = SendModule.InputType.COIN

    private var fee: BigDecimal = BigDecimal.ZERO
    private var availableFeeBalance: BigDecimal? = null

    private var error: Exception? = null

    private var customFeeRate: BigInteger? = null
        set(value) {
            field = value
            value?.let {
                showLowFeeWarningLiveData.postValue(value < recommendedFeeRate ?: BigInteger.ZERO)
            }
        }

    private var fetchedFeeRate: BigInteger? = null
    private var feeRatePriority: FeeRatePriority? = feeRateProvider?.defaultFeeRatePriority
    private var feeRateAdjustmentInfo: FeeRateAdjustmentInfo = FeeRateAdjustmentInfo(SendAmountInfo.NotEntered, null, baseCurrency, null)
    private var recommendedFeeRate: BigInteger? = null

    private val platformCoin: PlatformCoin
        get() = feeCoinData?.first ?: baseCoin

    private val feeRatePriorityList: List<FeeRatePriority> = feeRateProvider?.feeRatePriorityList ?: listOf()
    private val disposables = CompositeDisposable()

    init {
        if (!interactorPlatformCoin.coin.isCustom) {
            marketKit.coinPriceObservable(interactorPlatformCoin.coin.uid, baseCurrency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { marketInfo ->
                    didUpdateExchangeRate(marketInfo.value)
                }
                .let {
                    disposables.add(it)
                }
        }
    }

    private fun syncError() {

        if (error != null) {
            this.setError.postValue(error)
            return
        }

        try {
            validate()
            insufficientFeeBalanceError.postValue(null)
        } catch (e: SendFeeModule.InsufficientFeeBalance) {
            insufficientFeeBalanceError.postValue(e)
        }
    }

    private fun syncFees() {
        primaryFee.postValue(helper.feeAmount(fee, inputType, xRate))
        secondaryFee.postValue(helper.feeAmount(fee, inputType.reversed(), xRate))
    }

    private fun syncFeeRateLabels() {
        showCustomFeePriority.postValue(feeRatePriority is FeeRatePriority.Custom)

        feeRatePriority?.let {
            feePriority.postValue(it)
        }
    }

    private fun validate() {
        val (feeCoin, coinProtocol) = feeCoinData ?: return
        val availableFeeBalance = availableFeeBalance ?: return

        if (availableFeeBalance < fee) {
            throw SendFeeModule.InsufficientFeeBalance(baseCoin, coinProtocol, feeCoin, CoinValue(CoinValue.Kind.PlatformCoin(platformCoin), fee))
        }
    }

    private fun updateCustomFeeParams(priority: FeeRatePriority.Custom) {
        customPriorityUnit ?: return

        val range = IntRange(customPriorityUnit.fromBaseUnit(priority.range.first).toInt(), customPriorityUnit.fromBaseUnit(priority.range.last).toInt())
        val feeRateValue = (feeRate ?: priority.value).let { customPriorityUnit.fromBaseUnit(it) }
        val adjustedFeeRateValue = feeRateValue.coerceAtMost(customPriorityUnit.fromBaseUnit(priority.range.last)).toInt()   // value can't be more than slider upper range
        this.customFeeRate = adjustedFeeRateValue.toBigInteger()

        setCustomFeeParams.postValue(
            Triple<Int, IntRange, String?>(
                adjustedFeeRateValue,
                range,
                customPriorityUnit.getLabel()
            )
        )
    }

    private fun getSmartFee(): Long? {
        return fetchedFeeRate?.let {
            feeRateAdjustmentHelper.applyRule(platformCoin.coinType, feeRateAdjustmentInfo, it.toLong())
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


    override val coinValue: CoinValue
        get() = CoinValue(CoinValue.Kind.PlatformCoin(platformCoin), fee)

    override val currencyValue: CurrencyValue?
        get() = this.xRate?.let { xRate ->
            CurrencyValue(baseCurrency, fee * xRate)
        }

    override val feeRate: Long?
        get() = customFeeRate?.toLong() ?: getSmartFee()

    override fun setLoading(loading: Boolean) {
        this.setLoading.postValue(loading)
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
            this.syncFeeRate(it)
        }
    }

    private fun syncFeeRate(feeRatePriority: FeeRatePriority) {
        if (feeRateProvider == null)
            return

        feeRateProvider.feeRate(feeRatePriority)
            .subscribeOn(Schedulers.io())
            .subscribe({
                didUpdate(it, feeRatePriority)
            }, {
                didReceiveError(it as Exception)
            })
            .let { disposables.add(it) }
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

    fun onViewDidLoad() {
        xRate = marketKit.coinPrice(platformCoin.coin.uid, baseCurrency.code)?.value

        syncFeeRateLabels()
        syncFees()
        syncError()

        showAdjustableFeeMenu.postValue(feeRatePriorityList.isNotEmpty())
    }

    fun onClickFeeRatePriority() {
        val items = feeRatePriorityList.map { priority ->
            SendFeeModule.FeeRateInfoViewItem(priority, priority == feeRatePriority)
        }
        showFeePriorityOptions.value = items
    }

    fun onChangeFeeRate(feeRatePriority: FeeRatePriority) {
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

    fun onChangeFeeRateValue(value: Int) {
        customPriorityUnit ?: return

        val converted = customPriorityUnit.convertToBaseUnit(value.toLong())
        this.customFeeRate = converted.toBigInteger()
        moduleDelegate?.onUpdateFeeRate()
    }

    // IInteractorDelegate

    private fun didUpdate(feeRate: BigInteger, feeRatePriority: FeeRatePriority) {
        when (feeRatePriority) {
            FeeRatePriority.HIGH -> {
                showLowFeeWarningLiveData.postValue(false)
            }
            FeeRatePriority.RECOMMENDED -> {
                recommendedFeeRate = feeRate
                showLowFeeWarningLiveData.postValue(false)
            }
            is FeeRatePriority.Custom -> {
                // handled in customFeeRate setter
            }
            FeeRatePriority.LOW -> {
                showLowFeeWarningLiveData.postValue(true)
            }
        }

        this.fetchedFeeRate = feeRate
        moduleDelegate?.onUpdateFeeRate()
    }

    private fun didReceiveError(error: Exception) {
        this.error = error
        moduleDelegate?.onUpdateFeeRate()
    }

    private fun didUpdateExchangeRate(rate: BigDecimal) {
        xRate = rate
        syncFees()
    }

    // ViewModel

    override fun onCleared() {
        disposables.dispose()
    }
}
