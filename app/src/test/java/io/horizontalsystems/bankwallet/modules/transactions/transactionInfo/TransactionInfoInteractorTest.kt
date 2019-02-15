package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.core.IClipboardManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionInfoInteractorTest {

    private val delegate = mock(TransactionInfoModule.InteractorDelegate::class.java)
    private val clipboardManager = mock(IClipboardManager::class.java)

    private lateinit var interactor: TransactionInfoInteractor

    @Before
    fun setup() {
        interactor = TransactionInfoInteractor(clipboardManager)
        interactor.delegate = delegate
    }

    @Test
    fun onCopy() {
        val value = "value"

        interactor.onCopy(value)

        verify(clipboardManager).copyText(value)
    }

}
