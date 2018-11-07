package bitcoin.wallet.modules.settings.basecurrency

import bitcoin.wallet.core.ICurrencyManager
import bitcoin.wallet.entities.Currency

class BaseCurrencySettingsInteractor(
        private val currencyManager: ICurrencyManager): BaseCurrencySettingsModule.IBaseCurrencySettingsInteractor {

    var delegate: BaseCurrencySettingsModule.IBaseCurrencySettingsInteractorDelegate? = null

    override val currencies: List<Currency>
        get() = currencyManager.currencies

    override val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    override fun setBaseCurrency(code: String) {
        currencyManager.setBaseCurrency(code= code)
        delegate?.didSetBaseCurrency()
    }
}
