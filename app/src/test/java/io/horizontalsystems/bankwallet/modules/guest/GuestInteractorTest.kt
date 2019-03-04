package io.horizontalsystems.bankwallet.modules.guest

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.core.managers.AuthManager
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.Mockito.*

class GuestInteractorTest {

    private val authManager = mock(AuthManager::class.java)
    private val wordsManager = mock(WordsManager::class.java)
    private val delegate = mock(GuestModule.IInteractorDelegate::class.java)
    private val keystoreSafeExecute = Mockito.mock(IKeyStoreSafeExecute::class.java)
    private lateinit var sysInfoManager: ISystemInfoManager
    private lateinit var interactor: GuestInteractor
    private val appVersion = "1,01"

    @Captor
    private val actionRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Captor
    private val successRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Captor
    private val failureRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Before
    fun before() {
        RxBaseTest.setup()

        sysInfoManager = com.nhaarman.mockito_kotlin.mock {
            on { appVersion } doReturn appVersion
        }

        interactor = GuestInteractor(authManager, wordsManager, keystoreSafeExecute, sysInfoManager)
        interactor.delegate = delegate
    }

    @Test
    fun getAppVersion() {
        Assert.assertEquals(interactor.appVersion, appVersion)
    }

    @Test
    fun createWallet_success() {

        interactor.createWallet()

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val successRunnable = successRunnableCaptor.firstValue

        actionRunnable.run()
        successRunnable.run()

        verify(delegate).didCreateWallet()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun createWallet_error() {

        interactor.createWallet()

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val failureRunnable = failureRunnableCaptor.firstValue

        actionRunnable.run()
        failureRunnable.run()

        verify(delegate).didFailToCreateWallet()
        verifyNoMoreInteractions(delegate)
    }

}