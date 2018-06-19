package bitcoin.wallet.core.managers

import bitcoin.wallet.core.NetworkManager
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

