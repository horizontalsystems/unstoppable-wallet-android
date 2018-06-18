package bitcoin.wallet.core.managers

object Factory {

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

    val unspentOutputProvider by lazy {
        UnspentOutputProvider()
    }

    val exchangeRateProvider by lazy {
        ExchangeRateProvider()
    }

}
