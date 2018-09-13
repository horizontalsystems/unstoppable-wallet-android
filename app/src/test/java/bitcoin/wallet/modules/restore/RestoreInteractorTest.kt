package bitcoin.wallet.modules.restore

class RestoreInteractorTest {

//    private val mnemonic = mock(IMnemonic::class.java)
//    private var delegate = mock(RestoreModule.IInteractorDelegate::class.java)
//    private val blockchainManager = mock(BlockchainManager::class.java)
//
//    private var interactor = RestoreInteractor(mnemonic, blockchainManager)
//
//    @Before
//    fun before() {
//        RxBaseTest.setup()
//
//        interactor.delegate = delegate
//    }
//
//    @Test
//    fun restoreWallet_validate() {
//        val words = listOf("first", "second", "etc")
//
//        interactor.restore(words)
//
//        verify(mnemonic).validateWords(words)
//    }
//
//    @Test
//    fun restoreWallet_success() {
//        val words = listOf("first", "second", "etc")
//
//        whenever(mnemonic.validateWords(words)).thenReturn(true)
//
//        interactor.restore(words)
//
//        verify(blockchainManager).initNewWallet(words)
//        verify(delegate).didRestore()
//        verifyNoMoreInteractions(delegate)
//    }
//
//    @Test
//    fun restoreWallet_failureWordsError() {
//        val words = listOf("first", "second", "etc")
//
//        whenever(mnemonic.validateWords(words)).thenReturn(false)
//
//        interactor.restore(words)
//
//        verify(delegate).didFailToRestore(any())
//        verifyNoMoreInteractions(delegate)
//    }
//
//    @Test
//    fun restoreWallet_failureInitError() {
//        val words = listOf("first", "second", "etc")
//        val exception = UserNotAuthenticatedException()
//
//        whenever(mnemonic.validateWords(words)).thenReturn(true)
//        whenever(blockchainManager.initNewWallet(words)).thenThrow(exception)
//
//        interactor.restore(words)
//
//        verify(delegate).didFailToRestore(exception)
//        verifyNoMoreInteractions(delegate)
//    }
}