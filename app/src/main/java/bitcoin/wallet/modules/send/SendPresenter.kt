package bitcoin.wallet.modules.send

import bitcoin.wallet.R
import bitcoin.wallet.blockchain.InvalidAddress
import bitcoin.wallet.blockchain.NotEnoughFundsException
import bitcoin.wallet.blockchain.UnsupportedBlockchain

class SendPresenter(private val interactor: SendModule.IInteractor, private val router: SendModule.IRouter, private val coinCode: String) : SendModule.IViewDelegate, SendModule.IInteractorDelegate {

    var view: SendModule.IView? = null

    private var enteredAmount: Double = 0.0

    private var fiatAmount: Double? = null
    private var cryptoAmount: Double? = null

    private var exchangeRate = 0.0

    private var showCryptoAmountFirst = false

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
        showCryptoAmountFirst = !showCryptoAmountFirst

        updateAmounts()
    }

    override fun didFetchExchangeRate(exchangeRate: Double) {
        this.exchangeRate = exchangeRate
        recalculateSecondaryAmountHint()
    }

    override fun onAmountEntered(amount: Double?) {
        enteredAmount = amount ?: 0.0
        recalculateSecondaryAmountHint()
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
        updatePrimaryAmount()
        updateSecondaryAmountHint()
    }

    private fun recalculateSecondaryAmountHint() {
        if (showCryptoAmountFirst) {
            cryptoAmount = enteredAmount
            fiatAmount = enteredAmount * exchangeRate
        } else {
            fiatAmount = enteredAmount
            cryptoAmount = enteredAmount / exchangeRate
        }

        updateSecondaryAmountHint()
    }

    private fun updatePrimaryAmount() {
        if (showCryptoAmountFirst) {
            view?.setPrimaryCurrency(coinCode)
            view?.setPrimaryAmount(cryptoAmount)
        } else {
            view?.setPrimaryCurrency(baseCurrencyCode)
            view?.setPrimaryAmount(fiatAmount)
        }
    }

    private fun updateSecondaryAmountHint() {
        val amount = (if (showCryptoAmountFirst) fiatAmount else cryptoAmount) ?: 0.0
        val currency = if (showCryptoAmountFirst) baseCurrencyCode else coinCode
        view?.setSecondaryAmountHint("$amount $currency")
    }

}
