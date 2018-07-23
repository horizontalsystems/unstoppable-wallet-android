package bitcoin.wallet.modules.send

import bitcoin.wallet.R
import bitcoin.wallet.blockchain.InvalidAddress
import bitcoin.wallet.blockchain.NotEnoughFundsException
import bitcoin.wallet.blockchain.UnsupportedBlockchain
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import java.text.NumberFormat
import java.text.ParseException

class SendPresenter(private val interactor: SendModule.IInteractor, private val router: SendModule.IRouter, private val coinCode: String) : SendModule.IViewDelegate, SendModule.IInteractorDelegate {

    var view: SendModule.IView? = null

    private var enteredAmount: Double = 0.0

    private var fiatAmount: Double? = null
    private var cryptoAmount: Double? = null

    private var exchangeRate = 0.0

    private var isEnteringInCrypto = false

    private lateinit var baseCurrencyCode: String

    override fun onViewDidLoad() {
        baseCurrencyCode = interactor.getBaseCurrency()

        updateAmounts()

        interactor.fetchExchangeRate()
    }

    override fun onScanClick() {
        router.startScan()
    }

    override fun onPasteClick() {
        val copiedText = interactor.getCopiedText()
        view?.setAddress(copiedText)
    }

    override fun onCurrencyButtonClick() {
        isEnteringInCrypto = !isEnteringInCrypto

        updateAmounts()
    }

    override fun didFetchExchangeRate(exchangeRate: Double) {
        this.exchangeRate = exchangeRate
        refreshAmountHint()
    }

    override fun onAmountEntered(amount: String?) {
        val numberFormat = NumberFormat.getInstance()
        val number = try {
            numberFormat.parse(amount)
        } catch (ex: ParseException) {
            null
        }
        enteredAmount = number?.toDouble() ?: 0.0
        refreshAmountHint()
    }

    override fun onCancelClick() {
        view?.closeView()
    }

    override fun onSendClick(address: String) {
        cryptoAmount?.let { interactor.send(coinCode, address, it) }
    }

    override fun didFailToSend(exception: Exception) {
        view?.showError(getError(exception))
    }

    override fun didSend() {
        view?.showSuccess()
    }

    private fun getError(exception: Exception) = when (exception) {
        is UnsupportedBlockchain -> R.string.error_unsupported_blockchain
        is InvalidAddress -> R.string.send_bottom_sheet_error_invalid_address
        is NotEnoughFundsException -> R.string.send_bottom_sheet_error_insufficient_balance
        else -> R.string.error
    }

    private fun updateAmounts() {
        updateAmountView()
        updateAmountHintView()
    }

    private fun refreshAmountHint() {
        if (isEnteringInCrypto) {
            cryptoAmount = enteredAmount
            fiatAmount = enteredAmount * exchangeRate
        } else {
            fiatAmount = enteredAmount
            cryptoAmount = enteredAmount / exchangeRate
        }

        updateAmountHintView()
    }

    private fun updateAmountView() {
        val amount = (if (isEnteringInCrypto) cryptoAmount else fiatAmount) ?: 0.0
        val amountStr = if (isEnteringInCrypto) formatCryptoAmount(amount) else formatFiatAmount(amount)
        val currency = if (isEnteringInCrypto) coinCode else baseCurrencyCode

        view?.setCurrency(currency)
        view?.setAmount(if (amount ?: 0.0 > 0.0) amountStr else null)
    }

    private fun updateAmountHintView() {
        val amount = (if (isEnteringInCrypto) fiatAmount else cryptoAmount) ?: 0.0
        val amountStr = if (isEnteringInCrypto) formatFiatAmount(amount) else formatCryptoAmount(amount)
        val currency = if (isEnteringInCrypto) baseCurrencyCode else coinCode

        view?.setAmountHint("$amountStr $currency")
    }

    private fun formatCryptoAmount(amount: Double) = NumberFormatHelper.cryptoAmountFormat.format(amount)

    private fun formatFiatAmount(amount: Double) = NumberFormatHelper.fiatAmountFormat.format(amount)

}
