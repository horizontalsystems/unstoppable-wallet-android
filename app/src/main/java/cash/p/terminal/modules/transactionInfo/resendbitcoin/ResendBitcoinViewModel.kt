package cash.p.terminal.modules.transactionInfo.resendbitcoin

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import io.horizontalsystems.core.logger.AppLogger
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.IFeeRateProvider
import cash.p.terminal.core.LocalizedException
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.core.adapters.BitcoinBaseAdapter
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.SendWarningRiskOfGettingStuck
import cash.p.terminal.modules.transactionInfo.options.SpeedUpCancelType
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.strings.helpers.TranslatableString
import io.horizontalsystems.bitcoincore.rbf.ReplacementTransaction
import io.horizontalsystems.bitcoincore.rbf.ReplacementTransactionBuilder.BuildError
import io.horizontalsystems.bitcoincore.rbf.ReplacementTransactionInfo
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.Coin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.UnknownHostException

class ResendBitcoinViewModel(
    private val type: SpeedUpCancelType,
    private val transactionRecord: BitcoinTransactionRecord,

    private val replacementInfo: ReplacementTransactionInfo?,

    private val adapter: BitcoinBaseAdapter,
    private val feeRateProvider: IFeeRateProvider,
    private val xRateService: XRateService,
    private val contactsRepo: ContactsRepository,
) : ViewModelUiState<ResendBitcoinUiState>() {

    private val titleResId: Int
    private val sendButtonTitleResId: Int
    private val addressTitleResId: Int

    private val token = adapter.wallet.token
    private val transactionHash = transactionRecord.transactionHash

    private val logger = AppLogger("Resend-${token.coin.code}")

    private val coinMaxAllowedDecimals: Int = token.decimals
    private val fiatMaxAllowedDecimals: Int = App.appConfigProvider.fiatDecimal
    private val blockchainType: BlockchainType = token.blockchainType
    private val coinRate: CurrencyValue? = xRateService.getRate(token.coin.uid)

    private var sendResult: SendResult? = null
    private var feeCaution: HSCaution? = null

    private var minFee: Long = 0
    private var recommendedFee: Long = 0

    private var replacementTransaction: ReplacementTransaction? = null
    private var record = transactionRecord

    init {
        when (type) {
            SpeedUpCancelType.SpeedUp -> {
                titleResId = R.string.TransactionInfoOptions_SpeedUp_Title
                addressTitleResId = R.string.Send_Confirmation_To
                sendButtonTitleResId = R.string.TransactionInfoOptions_SpeedUp_Button
            }

            SpeedUpCancelType.Cancel -> {
                titleResId = R.string.TransactionInfoOptions_Cancel_Title
                addressTitleResId = R.string.Send_Confirmation_Own
                sendButtonTitleResId = R.string.TransactionInfoOptions_Cancel_Button
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (replacementInfo != null) {
                val feeRates = feeRateProvider.getFeeRates()
                val feeRange = replacementInfo.feeRange
                recommendedFee = replacementInfo.replacementTxMinSize * feeRates.recommended
                val minFee = recommendedFee.coerceAtLeast(feeRange.first).coerceAtMost(feeRange.last)

                updateReplacementTransaction(minFee)
            } else {
                feeCaution = createCaution(BuildError.UnableToReplace)
                emitState()
            }
        }

        viewModelScope.launch {
            contactsRepo.contactsFlow.collect {
                emitState()
            }
        }
    }

    private fun updateReplacementTransaction(minFee: Long) {
        try {
            this.minFee = minFee

            val (replacementTransaction, bitcoinTransactionRecord) = when (type) {
                SpeedUpCancelType.SpeedUp -> adapter.speedUpTransaction(transactionHash, minFee)
                SpeedUpCancelType.Cancel -> adapter.cancelTransaction(transactionHash, minFee)
            }

            this.replacementTransaction = replacementTransaction
            this.record = bitcoinTransactionRecord

            feeCaution = if (minFee < recommendedFee) SendWarningRiskOfGettingStuck else null
        } catch (error: Throwable) {
            feeCaution = createCaution(error)
        }

        emitState()
    }

    private fun createCaution(error: Throwable) = when (error) {
        BuildError.FeeTooLow -> HSCaution(TranslatableString.ResString(R.string.TransactionInfoOptions_Rbf_FeeTooLow))
        BuildError.RbfNotEnabled -> HSCaution(TranslatableString.ResString(R.string.TransactionInfoOptions_Rbf_NotEnabled))
        is BuildError.InvalidTransaction,
        BuildError.UnableToReplace,
        BuildError.NoPreviousOutput -> HSCaution(TranslatableString.ResString(R.string.TransactionInfoOptions_Rbf_UnableToReplace))

        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    override fun createState(): ResendBitcoinUiState {
        val address = Address(hex = record.to!!)
        val contact = contactsRepo.getContactsFiltered(blockchainType = blockchainType, addressQuery = address.hex).firstOrNull()

        return ResendBitcoinUiState(
            titleResId = titleResId,
            sendButtonTitleResId = sendButtonTitleResId,
            type = type,

            coin = token.coin,
            feeCoin = token.coin,
            amount = record.mainValue.decimalValue!!.abs(),
            fee = record.fee!!.decimalValue!!,
            address = address,
            addressTitleResId = addressTitleResId,
            contact = contact,
            lockTimeInterval = record.lockInfo?.lockTimeInterval,

            coinMaxAllowedDecimals = coinMaxAllowedDecimals,
            fiatMaxAllowedDecimals = fiatMaxAllowedDecimals,
            blockchainType = blockchainType,
            coinRate = coinRate,
            sendResult = sendResult,
            feeCaution = feeCaution,

            minFee = minFee,
            replacedTransactionHashes = replacementTransaction?.replacedTransactionHashes ?: listOf(transactionRecord.transactionHash)
        )
    }

    fun setMinFee(minFee: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            updateReplacementTransaction(minFee)
        }
    }

    fun incrementMinFee() {
        viewModelScope.launch(Dispatchers.IO) {
            updateReplacementTransaction(minFee + 1)
        }
    }

    fun decrementMinFee() {
        viewModelScope.launch(Dispatchers.IO) {
            updateReplacementTransaction(minFee - 1)
        }
    }

    fun onClickSend() {
        viewModelScope.launch(Dispatchers.IO) {
            send()
        }
    }

    private fun send() {
        val replacementTransaction = replacementTransaction ?: return

        val logger = logger.getScopedUnique()
        logger.info("click")

        try {
            sendResult = SendResult.Sending
            emitState()

            adapter.send(replacementTransaction)

            logger.info("success")

            sendResult = SendResult.Sent()
            emitState()
        } catch (e: Throwable) {
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
            emitState()
        }
    }

}

data class ResendBitcoinUiState(
    @StringRes
    val titleResId: Int,
    val sendButtonTitleResId: Int,
    val type: SpeedUpCancelType,

    val coin: Coin,
    val feeCoin: Coin,
    val amount: BigDecimal,
    val fee: BigDecimal,
    val address: Address,
    val addressTitleResId: Int,
    val contact: Contact?,
    val lockTimeInterval: LockTimeInterval? = null,

    val coinMaxAllowedDecimals: Int,
    val fiatMaxAllowedDecimals: Int,
    val blockchainType: BlockchainType,
    val coinRate: CurrencyValue?,
    val feeCaution: HSCaution?,
    val sendResult: SendResult?,

    val minFee: Long,
    val replacedTransactionHashes: List<String>
)
