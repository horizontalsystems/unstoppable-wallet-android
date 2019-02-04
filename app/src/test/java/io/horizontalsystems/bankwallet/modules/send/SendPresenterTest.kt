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
    private val view = mock(SendModule.IView::class.java)
    private val factory = mock(StateViewItemFactory::class.java)
    private val userInput = mock(SendModule.UserInput::class.java)
    private val prAddress = mock(PaymentRequestAddress::class.java)

    private val state = SendModule.State(2, SendModule.InputType.COIN)
    private val viewItem = SendModule.StateViewItem(8)
    private val viewItemConfirm = mock(SendModule.SendConfirmationViewItem::class.java)

    private lateinit var presenter: SendPresenter

    @Before
    fun setup() {

        whenever(interactor.parsePaymentAddress(any())).thenReturn(prAddress)
        whenever(interactor.stateForUserInput(any(), any())).thenReturn(state)
        whenever(factory.viewItemForState(any())).thenReturn(viewItem)
        whenever(factory.confirmationViewItemForState(any())).thenReturn(viewItemConfirm)

        presenter = SendPresenter(interactor, factory, userInput)
        presenter.view = view
    }

    // ViewDelegate

    @Test
    fun onViewDidLoad() {
        val inputType = mock(SendModule.InputType::class.java)

        whenever(interactor.defaultInputType).thenReturn(inputType)
        whenever(interactor.clipboardHasPrimaryClip).thenReturn(true)
        whenever(interactor.coinTitle).thenReturn("Bitcoin")
        whenever(interactor.coinCode).thenReturn("BTC")

        presenter.onViewDidLoad()

        verify(userInput).inputType = inputType

        verify(view).setCoinCode("BTC")
        verify(view).setCoinTitle("Bitcoin")
        verify(view).setDecimal(viewItem.decimal)
        verify(view).setAmountInfo(viewItem.amountInfo)
        verify(view).setSwitchButtonEnabled(viewItem.switchButtonEnabled)
        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setAddressInfo(viewItem.addressInfo)
        verify(view).setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        verify(view).setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
        verify(view).setSendButtonEnabled(viewItem.sendButtonEnabled)
        verify(view).setPasteButtonState(true)

        verify(interactor).retrieveRate()
    }

    @Test
    fun onAmountChanged() {
        presenter.onAmountChanged(0.5.toBigDecimal())

        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        verify(view).setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
        verify(view).setSendButtonEnabled(viewItem.sendButtonEnabled)
    }

    @Test
    fun onSwitchClicked() {
        whenever(userInput.inputType).thenReturn(SendModule.InputType.COIN)
        whenever(interactor.convertedAmountForInputType(userInput.inputType, userInput.amount)).thenReturn(0.toBigDecimal())

        presenter.onSwitchClicked()

        verify(userInput).inputType = SendModule.InputType.CURRENCY

        verify(view).setDecimal(viewItem.decimal)
        verify(view).setAmountInfo(viewItem.amountInfo)
        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setPrimaryFeeInfo(viewItem.primaryFeeInfo)
        verify(view).setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
        verify(interactor).defaultInputType = SendModule.InputType.CURRENCY
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

    @Test
    fun onDeleteClicked() {
        whenever(interactor.clipboardHasPrimaryClip).thenReturn(true)
        presenter.onDeleteClicked()

        verify(view).setPasteButtonState(true)
    }

    // InteractorDelegate

    @Test
    fun didRateRetrieve() {
        presenter.didRateRetrieve()

        verify(view).setSwitchButtonEnabled(viewItem.switchButtonEnabled)
        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setSecondaryFeeInfo(viewItem.secondaryFeeInfo)
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

    @Test
    fun onMaxClicked() {
        presenter.onMaxClicked()
        verify(interactor).getTotalBalanceMinusFee(userInput.inputType, userInput.address)
        verify(view).setAmountInfo(viewItem.amountInfo)
    }

}
