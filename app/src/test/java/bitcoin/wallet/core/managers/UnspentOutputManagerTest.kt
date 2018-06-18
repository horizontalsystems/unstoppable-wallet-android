package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.INetworkManager
import bitcoin.wallet.entities.UnspentOutput
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class UnspentOutputManagerTest {

    private val databaseManager = mock(IDatabaseManager::class.java)
    private val networkManager = mock(INetworkManager::class.java)
    private val updateSubject = mock<PublishSubject<List<UnspentOutput>>>()

    private val unspentOutputManager = UnspentOutputManager(databaseManager, networkManager, updateSubject)

    @Before
    fun before() {
        RxBaseTest.setup()
    }

    @Test
    fun refresh() {

        val unspentOutputs = listOf(mock(UnspentOutput::class.java), mock(UnspentOutput::class.java))

        whenever(networkManager.getUnspentOutputs()).thenReturn(Flowable.just(unspentOutputs))

        unspentOutputManager.refresh()

        verify(databaseManager).truncateUnspentOutputs()
        verify(databaseManager).insertUnspentOutputs(unspentOutputs)

    }

    @Test
    fun refresh_subjectCall() {

        val unspentOutputs = listOf(mock(UnspentOutput::class.java), mock(UnspentOutput::class.java))

        whenever(networkManager.getUnspentOutputs()).thenReturn(Flowable.just(unspentOutputs))

        unspentOutputManager.refresh()

        verify(updateSubject).onNext(unspentOutputs)

    }

}
