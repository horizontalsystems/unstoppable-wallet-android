package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.NoFeeSendTransactionError
import io.horizontalsystems.bankwallet.entities.FeeRateState
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single
import java.math.BigDecimal

class SendBitcoinHandler(
    private val adapter: ISendBitcoinAdapter,
    private val storage: ILocalStorage,
    private val coinType: CoinType
)
    : SendModule.ISendHandler, SendAmountModule.IAmountModuleDelegate,
      SendAddressModule.IAddressModuleDelegate, SendFeeModule.IFeeModuleDelegate,
      SendHodlerModule.IHodlerModuleDelegate {

    private fun syncValidation() {
        var amountError: Throwable? = null
        var addressError: Throwable? = null

        try {
            amountModule.validAmount()
        } catch (e: Exception) {
            amountError = e
        }

        try {
            addressModule.validateAddress()
        } catch (e: Exception) {
            addressError = e
        }

        delegate.onChange(amountError == null && addressError == null && feeModule.isValid, amountError, addressError)
    }

    private fun syncMinimumAmount() {
        amountModule.setMinimumAmount(adapter.minimumSendAmount(addressModule.currentAddress?.hex) ?: BigDecimal.ZERO)
        syncValidation()
    }

    private fun syncMaximumAmount() {
        hodlerModule?.let {
            amountModule.setMaximumAmount(adapter.maximumSendAmount(it.pluginData()))
            syncValidation()
        }
    }

    private fun syncCurrencyAmount() {
        feeModule.setAmountInfo(amountModule.sendAmountInfo)
        syncState()
    }

    // SendModule.ISendHandler

    override lateinit var amountModule: SendAmountModule.IAmountModule
    override lateinit var addressModule: SendAddressModule.IAddressModule
    override lateinit var feeModule: SendFeeModule.IFeeModule
    override lateinit var memoModule: SendMemoModule.IMemoModule
    override var hodlerModule: SendHodlerModule.IHodlerModule? = null

    override lateinit var delegate: SendModule.ISendHandlerDelegate

    override fun sync() {
        if (feeModule.feeRateState.isError) {

            feeModule.fetchFeeRate()
            syncState()
            syncValidation()
        }
    }

    private fun syncState() {
        val loading = feeModule.feeRateState.isLoading

        amountModule.setLoading(loading)
        feeModule.setLoading(loading)

        if (loading)
            return

        if (feeModule.feeRateState is FeeRateState.Error) {

            feeModule.setFee(BigDecimal.ZERO)
            feeModule.setError((feeModule.feeRateState as FeeRateState.Error).error)

        } else if (feeModule.feeRateState is FeeRateState.Value) {

            val feeRateValue = (feeModule.feeRateState as FeeRateState.Value).value
            feeModule.setError(null)
            amountModule.setAvailableBalance(
                adapter.availableBalance(
                    feeRateValue,
                    addressModule.currentAddress?.hex,
                    hodlerModule?.pluginData()
                )
            )
            feeModule.setFee(
                adapter.fee(
                    amountModule.currentAmount,
                    feeRateValue,
                    addressModule.currentAddress?.hex,
                    hodlerModule?.pluginData()
                ) ?: BigDecimal.ZERO
            )
            syncValidation()
        }
    }

    override val inputItems: List<SendModule.Input> =
        mutableListOf<SendModule.Input>().apply {
            add(SendModule.Input.Amount)
            add(SendModule.Input.Address())
            add(SendModule.Input.Fee)
            if (coinType is CoinType.Bitcoin && storage.isLockTimeEnabled)
                add(SendModule.Input.Hodler)
            add(SendModule.Input.ProceedButton)
        }

    override fun onModulesDidLoad() {
        feeModule.setBalance(adapter.balanceData.available)
        feeModule.fetchFeeRate()

        syncState()
        syncMinimumAmount()
        syncMaximumAmount()
    }

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        val hodlerData = hodlerModule?.pluginData()?.get(HodlerPlugin.id) as? HodlerData
        val lockTimeInterval = hodlerData?.lockTimeInterval

        return mutableListOf<SendModule.SendConfirmationViewItem>().apply {
            add(SendModule.SendConfirmationAmountViewItem(
                    amountModule.coinValue(),
                    amountModule.currencyValue(),
                    addressModule.validAddress(),
                    lockTimeInterval != null))

            add(SendModule.SendConfirmationFeeViewItem(feeModule.coinValue, feeModule.currencyValue))

            lockTimeInterval?.let {
                add(SendModule.SendConfirmationLockTimeViewItem(it))
            }
        }

    }

    override fun sendSingle(logger: AppLogger): Single<Unit> {
        return when (val feeRate = feeModule.feeRate) {
            null -> Single.error(NoFeeSendTransactionError())
            else -> adapter.send(
                amountModule.validAmount(),
                addressModule.validAddress().hex,
                feeRate,
                hodlerModule?.pluginData(),
                storage.transactionSortingType,
                logger
            )
        }
    }

    override fun onChangeAmount() {
        syncState()
        syncValidation()
        syncCurrencyAmount()
    }

    override fun onChangeInputType(inputType: AmountInputType) {
        feeModule.setInputType(inputType)
    }

    override fun onRateUpdated(rate: BigDecimal?) {
        feeModule.setRate(rate)
    }

    // SendAddressModule.ModuleDelegate

    override fun validate(address: String) {
        adapter.validate(address, hodlerModule?.pluginData())
    }

    override fun onUpdateAddress() {
        syncMinimumAmount()
        syncState()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    // SendFeeModule.IFeeModuleDelegate

    override fun onUpdateFeeRate() {
        syncState()
    }

    override fun onUpdateLockTimeInterval(timeInterval: LockTimeInterval?) {
        syncValidation()
        syncMaximumAmount()

        syncState()
    }
}
