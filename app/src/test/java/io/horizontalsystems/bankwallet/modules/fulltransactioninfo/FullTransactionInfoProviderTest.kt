package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.INetworkManager
import org.mockito.Mockito.mock

//todo fix this broken test
class FullTransactionInfoProviderTest {

    private var networkManager = mock(INetworkManager::class.java)
    private val adapter = mock(FullTransactionInfoModule.Adapter::class.java)
    private val provider = mock(FullTransactionInfoModule.Provider::class.java)
    private val transactionHash = "abc"
    private val transactionUri = "https://domain.com/tx/abc"
    private val jsonObject = JsonObject()

    private lateinit var fullTransactionInfoProvider: FullTransactionInfoProvider

//    @Before
//
//    fun setup() {
//        whenever(provider.apiUrl(transactionHash)).thenReturn(transactionUri)
//        whenever(networkManager.getTransaction(any(), any()))
//                .thenReturn(Flowable.just(jsonObject))
//
//        fullTransactionInfoProvider = FullTransactionInfoProvider(networkManager, adapter, provider)
//    }
//
//    @Test
//    fun url() {
//        fullTransactionInfoProvider.url(transactionHash)
//
//        verify(provider).url(transactionHash)
//    }
//
//    @Test
//    fun retrieveTransactionInfo() {
//        fullTransactionInfoProvider.retrieveTransactionInfo(transactionHash)
//                .test()
//                .assertOf {
//                    verify(provider).apiUrl(transactionHash)
//                    verify(adapter).convert(jsonObject)
//                }
//    }
}
