package io.horizontalsystems.bankwallet.modules.send

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.FeeRates
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal

class SendPresenterTest {

    private val interactor = mock(SendModule.IInteractor::class.java)
    private val view = mock(SendModule.IView::class.java)
    private val factory = mock(StateViewItemFactory::class.java)
    private val userInput = mock(SendModule.UserInput::class.java)
    private val prAddress = mock(PaymentRequestAddress::class.java)
    private val feeRateConverter = mock(FeeRateSliderConverter::class.java)
    private val coin = mock(Coin::class.java)

    private val state = SendModule.State(2, SendModule.InputType.COIN)
    private val viewItem = SendModule.StateViewItem(8)
    private val viewItemConfirm = mock(SendModule.SendConfirmationViewItem::class.java)
    private val feeRate = 13

    private lateinit var presenter: SendPresenter

    @Before
    fun setup() {

        whenever(interactor.coin).thenReturn(coin)
        whenever(interactor.parsePaymentAddress(any())).thenReturn(prAddress)
        whenever(interactor.stateForUserInput(any())).thenReturn(state)
        whenever(factory.viewItemForState(any(), any())).thenReturn(viewItem)
        whenever(factory.confirmationViewItemForState(any())).thenReturn(viewItemConfirm)

        presenter = SendPresenter(interactor, factory, userInput, feeRateConverter)
        presenter.view = view
    }

    // ViewDelegate

    @Test
    fun onViewDidLoad() {
        val medium = 5
        val feeRate = FeeRates(1, medium, 10)

        val inputType = mock(SendModule.InputType::class.java)

        whenever(interactor.defaultInputType).thenReturn(inputType)
        whenever(interactor.clipboardHasPrimaryClip).thenReturn(true)
        whenever(interactor.feeRates).thenReturn(feeRate)

        presenter.onViewDidLoad()

        verify(userInput).feeRate = medium

        verify(view).setCoin(interactor.coin)
        verify(view).setDecimal(viewItem.decimal)
        verify(view).setAmountInfo(viewItem.amountInfo)
        verify(view).setSwitchButtonEnabled(viewItem.switchButtonEnabled)
        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setAddressInfo(viewItem.addressInfo)
        verify(view).setFeeInfo(viewItem.feeInfo)
        verify(view).setSendButtonEnabled(viewItem.sendButtonEnabled)
        verify(view).setPasteButtonState(true)

        verify(interactor).retrieveRate()
    }

    @Test
    fun onViewDidLoad_setFeeRate() {
        val medium = 5
        val feeRate = FeeRates(1, medium, 10)
        val sliderPosition = 30

        whenever(interactor.feeRates).thenReturn(feeRate)
        whenever(feeRateConverter.percent(medium)).thenReturn(sliderPosition)

        presenter.onViewDidLoad()

        verify(view).setFeeSliderPosition(sliderPosition)
    }

    @Test
    fun onAmountChanged() {
        presenter.onAmountChanged(0.5.toBigDecimal())

        verify(view).setHintInfo(viewItem.hintInfo)
        verify(view).setFeeInfo(viewItem.feeInfo)
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
        verify(view).setFeeInfo(viewItem.feeInfo)
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
        verify(view).setFeeInfo(viewItem.feeInfo)
    }

    @Test
    fun didRateRetrieve_withDefaultInputTypeAsCurrency() {
        val inputType = SendModule.InputType.CURRENCY

        whenever(userInput.amount).thenReturn(BigDecimal.ZERO)
        whenever(interactor.defaultInputType).thenReturn(inputType)

        presenter.didRateRetrieve()
        verify(userInput).inputType = SendModule.InputType.CURRENCY
    }

    @Test
    fun didSend() {
        presenter.didSend()

        verify(view).dismissWithSuccess()
    }

    @Test
    fun didFailToSend() {
        val exception = Throwable()
        val errorTextId = R.string.Hud_Network_Issue

        presenter.didFailToSend(exception)
        verify(view).showError(errorTextId)
    }

    @Test
    fun onMaxClicked() {
        whenever(userInput.feeRate).thenReturn(feeRate)
        presenter.onMaxClicked()
        verify(interactor).getTotalBalanceMinusFee(userInput.inputType, userInput.address, feeRate)
        verify(view).setAmountInfo(viewItem.amountInfo)
    }

    @Test
    fun onClear() {
        presenter.onClear()

        verify(interactor).clear()
    }

    @Test
    fun onFeeMultiplierChange_converterIsNull() {
        val medium = 5
        val feeRate = FeeRates(1, medium, 10)
        val percent = 35

        whenever(interactor.feeRates).thenReturn(feeRate)

        presenter = SendPresenter(interactor, factory, userInput, null)

        presenter.onFeeMultiplierChange(percent)

        verify(userInput).feeRate = medium
    }

    @Test
    fun onFeeMultiplierChange() {
        val feeRate = FeeRates(1, 5, 10)
        val percent = 35
        val unit = 3

        whenever(interactor.feeRates).thenReturn(feeRate)
        whenever(feeRateConverter.percent(percent)).thenReturn(unit)

        presenter.onFeeMultiplierChange(percent)

        verify(userInput).feeRate = unit
    }

    @Test
    fun test_FeeAdjustable() {
        val feeAdjustable = presenter.feeAdjustable
        Assert.assertEquals(true, feeAdjustable)
    }

    @Test
    fun test_FeeAdjustable_false() {
        presenter = SendPresenter(interactor, factory, userInput, null)
        val feeAdjustable = presenter.feeAdjustable
        Assert.assertEquals(false, feeAdjustable)
    }

}
