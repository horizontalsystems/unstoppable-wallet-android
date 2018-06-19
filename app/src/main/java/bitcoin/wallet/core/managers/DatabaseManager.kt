package bitcoin.wallet.core.managers

import bitcoin.wallet.core.App
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.Bitcoin
import bitcoin.wallet.entities.UnspentOutput

class DatabaseManager : IDatabaseManager {

    private val unspentOutputDao by lazy {
        App.db.unspentOutputDao()
    }

    override fun getUnspentOutputs(): List<UnspentOutput> = unspentOutputDao.all

    override fun insertUnspentOutputs(values: List<UnspentOutput>) {
        unspentOutputDao.insertAll(*values.toTypedArray())
    }

    override fun truncateUnspentOutputs() {
        unspentOutputDao.truncate()
    }

    override fun getExchangeRates(): HashMap<String, Double> {
        return hashMapOf(Bitcoin().code to 14_400.0)
    }

    override fun insertExchangeRates(values: HashMap<String, Double>) {
    }

    override fun truncateExchangeRates() {
    }

}
