package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.entities.Bitcoin
import bitcoin.wallet.entities.UnspentOutput
import io.reactivex.subjects.PublishSubject

object Factory {

    val unspentOutputUpdateSubject = PublishSubject.create<List<UnspentOutput>>()
    val exchangeRateUpdateSubject = PublishSubject.create<HashMap<String, Double>>()

    val mnemonicManager by lazy {
        MnemonicManager()
    }

    val preferencesManager by lazy {
        PreferencesManager()
    }

    val walletDataProvider by lazy {
        StubWalletDataProvider()
    }

    val randomProvider by lazy {
        RandomProvider()
    }

    val unspentOutputManager by lazy {
        UnspentOutputManager(databaseManager, networkManager, unspentOutputUpdateSubject)
    }

    val exchangeRateManager by lazy {
        ExchangeRateManager(databaseManager, networkManager, exchangeRateUpdateSubject)
    }

    val databaseManager by lazy {
        DatabaseManager()
    }

    val networkManager by lazy {
        NetworkManager()
    }

}

class DatabaseManager : IDatabaseManager {

    override fun getUnspentOutputs(): List<UnspentOutput> {
        return listOf(
                UnspentOutput(32500000, 0, 0, "", ""),
                UnspentOutput(16250000, 0, 0, "", "")
        )

    }

    override fun insertUnspentOutputs(values: List<UnspentOutput>) {
    }

    override fun truncateUnspentOutputs() {
    }

    override fun getExchangeRates(): HashMap<String, Double> {
        return hashMapOf(Bitcoin().code to 14_400.0)
    }

    override fun insertExchangeRates(values: HashMap<String, Double>) {
    }

    override fun truncateExchangeRates() {
    }

}
