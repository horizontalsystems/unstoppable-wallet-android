package bitcoin.wallet.core.managers

import bitcoin.wallet.core.App
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.UnspentOutput

class DatabaseManager : IDatabaseManager {

    private val unspentOutputDao by lazy {
        App.db.unspentOutputDao()
    }

    private val exchangeRateDao by lazy {
        App.db.exchangeRateDao()
    }

    override fun getUnspentOutputs(): List<UnspentOutput> = unspentOutputDao.all

    override fun insertUnspentOutputs(values: List<UnspentOutput>) {
        unspentOutputDao.insertAll(*values.toTypedArray())
    }

    override fun truncateUnspentOutputs() {
        unspentOutputDao.truncate()
    }

    override fun getExchangeRates(): List<ExchangeRate> = exchangeRateDao.all

    override fun insertExchangeRates(values: List<ExchangeRate>) {
        exchangeRateDao.insertAll(*values.toTypedArray())
    }

    override fun truncateExchangeRates() {
        exchangeRateDao.truncate()
    }

}
