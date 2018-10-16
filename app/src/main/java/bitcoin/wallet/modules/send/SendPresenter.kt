package bitcoin.wallet.modules.send

import bitcoin.wallet.R
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import java.text.NumberFormat
import java.text.ParseException

class SendPresenter(
        private val interactor: SendModule.IInteractor,
        private val router: SendModule.IRouter,
        private val baseCurrency: Currency) : SendModule.IViewDelegate, SendModule.IInteractorDelegate {

    var view: SendModule.IView? = null

    private var enteredAmount: Double = 0.0
    private var fiatAmount: Double? = null
    private var cryptoAmount: Double? = null
    private var exchangeRate = 0.0

    lateinit var coinCode: String

    override fun onViewDidLoad() {
        coinCode = interactor.getCoinCode()
        updateAmounts()

        interactor.fetchExchangeRate()
    }

    override fun didFetchExchangeRate(exchangeRate: Double) {
        this.exchangeRate = exchangeRate
        refreshAmountHint()
    }

    override fun didFailToSend(exception: Exception) {
        view?.showError(getError(exception))
    }

    override fun didSend() {
        view?.showSuccess()
    }

    override fun onScanClick() {
        router.startScan()
    }

    override fun onPasteClick() {
        val copiedText = interactor.getCopiedText()
        view?.setAddress(copiedText)
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

    override fun onAddressEntered(address: String?) {
        view?.showAddressWarning(address?.let { !interactor.isValid(it) } ?: false)
    }

    override fun onSendClick(address: String) {
        cryptoAmount?.let { interactor.send(coinCode, address, it) }
    }

    private fun updateAmounts() {
        updateAmountView()
        updateAmountHintView()
    }

    private fun refreshAmountHint() {
        cryptoAmount = enteredAmount
        fiatAmount = enteredAmount * exchangeRate
        updateAmountHintView()
    }

    private fun updateAmountView() {
        val amount = cryptoAmount ?: 0.0
        val amountStr = formatCryptoAmount(amount)

        view?.setAmount(if (amount > 0.0) amountStr else null)
    }

    private fun updateAmountHintView() {
        val amountStr = formatFiatAmount(fiatAmount ?: 0.0)

        view?.setAmountHint("${baseCurrency.getSymbolChar()} $amountStr")
    }

    private fun getError(exception: Exception) = when (exception) {
//        is UnsupportedBlockchain -> R.string.error_unsupported_blockchain
//        is InvalidAddress -> R.string.send_bottom_sheet_error_invalid_address
//        is NotEnoughFundsException -> R.string.send_insufficient_funds
        else -> R.string.error
    }

    private fun formatCryptoAmount(amount: Double) = NumberFormatHelper.cryptoAmountFormat.format(amount)

    private fun formatFiatAmount(amount: Double) = NumberFormatHelper.fiatAmountFormat.format(amount)

}
