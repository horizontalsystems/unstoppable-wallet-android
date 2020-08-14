package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.FeeState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.Single
import java.math.BigDecimal

class SendBitcoinHandler(
        private val interactor: SendModule.ISendBitcoinInteractor,
        private val router: SendModule.IRouter,
        private val coinType: CoinType)
    : SendModule.ISendHandler, SendModule.ISendBitcoinInteractorDelegate, SendAmountModule.IAmountModuleDelegate,
      SendAddressModule.IAddressModuleDelegate, SendFeeModule.IFeeModuleDelegate,
      SendHodlerModule.IHodlerModuleDelegate {

    private fun syncValidation() {
        try {
            amountModule.validAmount()
            addressModule.validAddress()

            delegate.onChange(true)

        } catch (e: Exception) {
            delegate.onChange(false)
        }
    }

    private fun syncMinimumAmount() {
        amountModule.setMinimumAmount(interactor.fetchMinimumAmount(addressModule.currentAddress))
        syncValidation()
    }

    private fun syncMaximumAmount() {
        hodlerModule?.let {
            amountModule.setMaximumAmount(interactor.fetchMaximumAmount(it.pluginData()))
            syncValidation()
        }
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

        if (feeModule.feeRateState is FeeState.Error) {

            feeModule.setFee(BigDecimal.ZERO)
            feeModule.setError((feeModule.feeRateState as FeeState.Error).error)

        } else if (feeModule.feeRateState is FeeState.Value) {

            val feeRateValue = (feeModule.feeRateState as FeeState.Value).value
            feeModule.setError(null)
            interactor.fetchAvailableBalance(feeRateValue, addressModule.currentAddress, hodlerModule?.pluginData())
            interactor.fetchFee(amountModule.currentAmount, feeRateValue, addressModule.currentAddress,
                                hodlerModule?.pluginData())
        }
    }

    override val inputItems: List<SendModule.Input> =
            mutableListOf<SendModule.Input>().apply {
                add(SendModule.Input.Amount)
                add(SendModule.Input.Address())
                add(SendModule.Input.Fee(true))
                if (coinType is CoinType.Bitcoin && interactor.isLockTimeEnabled)
                    add(SendModule.Input.Hodler)
                add(SendModule.Input.ProceedButton)
            }

    override fun onModulesDidLoad() {
        feeModule.fetchFeeRate()

        syncState()
        syncMinimumAmount()
        syncMaximumAmount()
    }

    override fun onAddressScan(address: String) {
        addressModule.didScanQrCode(address)
    }

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        val hodlerData = hodlerModule?.pluginData()?.get(HodlerPlugin.id) as? HodlerData
        val lockTimeInterval = hodlerData?.lockTimeInterval

        return mutableListOf<SendModule.SendConfirmationViewItem>().apply {
            add(SendModule.SendConfirmationAmountViewItem(
                    amountModule.primaryAmountInfo(),
                    amountModule.secondaryAmountInfo(),
                    addressModule.validAddress(),
                    lockTimeInterval != null))

            add(SendModule.SendConfirmationFeeViewItem(feeModule.primaryAmountInfo, feeModule.secondaryAmountInfo))

            add(SendModule.SendConfirmationDurationViewItem(feeModule.duration))

            lockTimeInterval?.let {
                add(SendModule.SendConfirmationLockTimeViewItem(it))
            }
        }

    }

    override fun sendSingle(logger: AppLogger): Single<Unit> {
        return interactor.send(amountModule.validAmount(), addressModule.validAddress(), feeModule.feeRate,
                               hodlerModule?.pluginData(), logger)
    }

    // SendModule.ISendBitcoinInteractorDelegate

    override fun didFetchAvailableBalance(availableBalance: BigDecimal) {
        amountModule.setAvailableBalance(availableBalance)
        syncValidation()
    }

    override fun didFetchFee(fee: BigDecimal) {
        feeModule.setFee(fee)
    }

    // SendAmountModule.ModuleDelegate

    override fun onChangeAmount() {
        syncState()
        syncValidation()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    // SendAddressModule.ModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address, hodlerModule?.pluginData())
    }

    override fun onUpdateAddress() {
        syncMinimumAmount()
        syncState()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    override fun scanQrCode() {
        router.scanQrCode()
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
