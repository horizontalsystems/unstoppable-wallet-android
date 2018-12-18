package io.horizontalsystems.bankwallet.modules.send

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SendPresenterTest {

    private val interactor = mock(SendModule.IInteractor::class.java)
    private val router = mock(SendModule.IRouter::class.java)
    private val view = mock(SendModule.IView::class.java)
    private val factory = mock(StateViewItemFactory::class.java)
    private val userInput = mock(SendModule.UserInput::class.java)
    private val prAddress = mock(PaymentRequestAddress::class.java)

    private val state = SendModule.State(SendModule.InputType.COIN)
    private val viewItem = SendModule.StateViewItem()
    private val viewItemConfirm = mock(SendModule.SendConfirmationViewItem::class.java)

    private lateinit var presenter: SendPresenter

    @Before
    fun setup() {

        whenever(interactor.parsePaymentAddress(any())).thenReturn(prAddress)
        whenever(interactor.stateForUserInput(any())).thenReturn(state)
        whenever(factory.viewItemForState(any())).thenReturn(viewItem)
        whenever(factory.confirmationViewItemForState(any())).thenReturn(viewItemConfirm)

        presenter = SendPresenter(interactor, router, factory, userInput)
        presenter.view = view
    }

    // ViewDelegate

    @Test
    fun onViewDidLoad() {
        presenter.onViewDidLoad()

        verify(interactor).retrieveRate()
    }

    @Test
    fun onAmountChanged() {
        presenter.onAmountChanged(0.5)

        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        verify(view).setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
        verify(view).setSendButtonEnabled(viewItem.sendButtonEnabled)
    }

    @Test
    fun onSwitchClicked() {
        whenever(userInput.inputType).thenReturn(SendModule.InputType.COIN)

        presenter.onSwitchClicked()

        verify(userInput).inputType = SendModule.InputType.CURRENCY

        verify(view).setAmountInfo(viewItem.amountInfo)
        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        verify(view).setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
    }

    @Test
    fun onPasteClicked() {
        whenever(interactor.addressFromClipboard).thenReturn("abc")

        presenter.onPasteClicked()
        verify(interactor).parsePaymentAddress("abc")
    }

    @Test
    fun onScanAddress() {
        presenter.onScanAddress("abc")

        verify(interactor).parsePaymentAddress("abc")
    }

    @Test
    fun onSendClicked() {
        presenter.onSendClicked()

        verify(view).showConfirmation(viewItemConfirm)
    }

    // InteractorDelegate

    @Test
    fun didRateRetrieve() {
        presenter.didRateRetrieve()

        verify(view).setCoin(interactor.coinCode)
        verify(view).setAmountInfo(viewItem.amountInfo)
        verify(view).setSwitchButtonEnabled(viewItem.switchButtonEnabled)
        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setAddressInfo(viewItem.addressInfo)
        verify(view).setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        verify(view).setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
        verify(view).setSendButtonEnabled(viewItem.sendButtonEnabled)
    }

    @Test
    fun didSend() {
        presenter.didSend()

        verify(view).dismissWithSuccess()
    }

    @Test
    fun didFailToSend() {
        val exception = Throwable()

        presenter.didFailToSend(exception)
        verify(view).showError(exception)
    }
}
