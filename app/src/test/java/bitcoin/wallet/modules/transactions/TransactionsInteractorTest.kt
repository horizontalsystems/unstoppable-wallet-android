package bitcoin.wallet.modules.transactions

class TransactionsInteractorTest {

//    private val delegate = mock(TransactionsModule.IInteractorDelegate::class.java)
//    private val databaseManager = mock(IDatabaseManager::class.java)
//    private val coinManager = mock(CoinManager::class.java)
//
//    private val interactor = TransactionsInteractor(databaseManager, coinManager)
//
//    @Before
//    fun before() {
//        RxBaseTest.setup()
//
//        interactor.delegate = delegate
//    }
//
//    @Test
//    fun retrieveTransactionRecords() {
//        whenever(databaseManager.getTransactionRecords()).thenReturn(Observable.empty())
//        whenever(databaseManager.getBlockchainInfos()).thenReturn(Observable.empty())
//
//        interactor.retrieveTransactionRecords()
//
//        verify(databaseManager).getTransactionRecords()
//    }
//
//    @Test
//    fun retrieveTransactionItems_success() {
//        val transactionRecords = listOf<TransactionRecord>()
//
//        whenever(databaseManager.getTransactionRecords()).thenReturn(Observable.just(DatabaseChangeset(transactionRecords)))
//        whenever(databaseManager.getBlockchainInfos()).thenReturn(Observable.empty())
//        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(DatabaseChangeset(arrayListOf())))
//
//        interactor.retrieveTransactionRecords()
//
//        verify(delegate).didRetrieveTransactionRecords(listOf())
//    }
//
//    @Test
//    fun retrieveTransactionItems_transactionOutConvert() {
//        val now = Date()
//        val bitcoin = Bitcoin()
//        val bitcoinCash = BitcoinCash()
//        val blockchainInfos = listOf(
//                BlockchainInfo().apply {
//                    coinCode = "BTC"
//                    latestBlockHeight = 130
//                },
//                BlockchainInfo().apply {
//                    coinCode = "BCH"
//                    latestBlockHeight = 140
//                })
//
//        val btcTxAmount = 1.0
//        val bchTxAmount = 1.0
//
//        val transactionRecordBTC = TransactionRecord().apply {
//            transactionHash = "transactionHash"
//            amount = 100000000
//            fee = 1000000
//            incoming = true
//            timestamp = now.time
//            from = RealmList("from-address")
//            to = RealmList("to-address")
//            blockHeight = 0
//            coinCode = "BTC"
//        }
//
//        val transactionRecordBCH = TransactionRecord().apply {
//            transactionHash = "transactionHash"
//            amount = 100000000
//            fee = 1000000
//            incoming = true
//            timestamp = now.time
//            from = RealmList("from-address")
//            to = RealmList("to-address")
//            blockHeight = 113
//            coinCode = "BCH"
//        }
//
//
//        val btcExchangeRate = ExchangeRate().apply {
//            code = "BTC"
//            value = 7349.4
//        }
//
//        val bchExchangeRate = ExchangeRate().apply {
//            code = "BCH"
//            value = 843.2
//        }
//
//        val exchangeRates = listOf(btcExchangeRate, bchExchangeRate)
//
//        val expectedItems = listOf(
//                TransactionRecordViewItem(
//                        "transactionHash",
//                        CoinValue(bitcoin, btcTxAmount),
//                        CoinValue(bitcoin, 0.01),
//                        "from-address",
//                        "to-address",
//                        true,
//                        0,
//                        now,
//                        TransactionRecordViewItem.Status.PENDING,
//                        0,
//                        btcTxAmount / 100_000_000.0 * btcExchangeRate.value
//                ),
//                TransactionRecordViewItem(
//                        "transactionHash",
//                        CoinValue(bitcoinCash, bchTxAmount),
//                        CoinValue(bitcoinCash, 0.01),
//                        "from-address",
//                        "to-address",
//                        true,
//                        113,
//                        now,
//                        TransactionRecordViewItem.Status.SUCCESS,
//                        28,
//                        bchTxAmount / 100_000_000.0 * bchExchangeRate.value
//                )
//        )
//
//        whenever(coinManager.getCoinByCode("BTC")).thenReturn(bitcoin)
//        whenever(coinManager.getCoinByCode("BCH")).thenReturn(bitcoinCash)
//        whenever(databaseManager.getTransactionRecords()).thenReturn(Observable.just(DatabaseChangeset(listOf(transactionRecordBTC, transactionRecordBCH))))
//        whenever(databaseManager.getBlockchainInfos()).thenReturn(Observable.just(DatabaseChangeset(blockchainInfos)))
//
//        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(DatabaseChangeset(exchangeRates)))
//
//        interactor.retrieveTransactionRecords()
//
//        verify(delegate).didRetrieveTransactionRecords(expectedItems)
//    }

}
