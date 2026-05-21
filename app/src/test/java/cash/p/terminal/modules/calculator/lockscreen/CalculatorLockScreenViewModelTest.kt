package cash.p.terminal.modules.calculator.lockscreen

import cash.p.terminal.modules.calculator.domain.CalculatorExpressionEvaluator
import cash.p.terminal.modules.calculator.domain.CalculatorPinAttemptThrottle
import cash.p.terminal.modules.pin.unlock.AttemptPinUnlockUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorLockScreenViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var throttle: CalculatorPinAttemptThrottle
    private lateinit var attemptPinUnlock: AttemptPinUnlockUseCase
    private val evaluator = CalculatorExpressionEvaluator()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        throttle = mockk(relaxed = true)
        attemptPinUnlock = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        divideByZeroText: String = "can't divide by 0",
        locale: Locale = Locale.US,
    ): CalculatorLockScreenViewModel = CalculatorLockScreenViewModel(
        throttle = throttle,
        evaluator = evaluator,
        attemptPinUnlock = attemptPinUnlock,
        locale = locale,
        divideByZeroText = divideByZeroText,
    )

    @Test
    fun onDigitClick_appendsDigitToExpression() {
        val viewModel = createViewModel()

        viewModel.onDigitClick('5')
        viewModel.onDigitClick('7')

        assertEquals("57", viewModel.uiState.expression)
        assertEquals("57", viewModel.uiState.displayedResult)
    }

    @Test
    fun onOperatorClick_emptyExpressionWithMinus_acceptsAsUnary() {
        val viewModel = createViewModel()

        viewModel.onOperatorClick('-')

        assertEquals("-", viewModel.uiState.expression)
    }

    @Test
    fun onOperatorClick_emptyExpressionWithPlus_ignored() {
        val viewModel = createViewModel()

        viewModel.onOperatorClick('+')

        assertEquals("", viewModel.uiState.expression)
    }

    @Test
    fun onOperatorClick_emptyExpressionWithPercent_ignored() {
        val viewModel = createViewModel()

        viewModel.onOperatorClick('%')

        assertEquals("", viewModel.uiState.expression)
        assertEquals("0", viewModel.uiState.displayedResult)
    }

    @Test
    fun onOperatorClick_consecutiveOperators_replacesLast() {
        val viewModel = createViewModel()

        viewModel.onDigitClick('5')
        viewModel.onOperatorClick('+')
        viewModel.onOperatorClick('-')

        assertEquals("5-", viewModel.uiState.expression)
    }

    @Test
    fun onOperatorClick_percentAfterNumber_appendsAndUpdatesLiveResult() {
        val viewModel = createViewModel()

        viewModel.onDigitClick('1')
        viewModel.onDigitClick('0')
        viewModel.onOperatorClick('%')

        assertEquals("10%", viewModel.uiState.expression)
        assertEquals("0.1", viewModel.uiState.displayedResult)
    }

    @Test
    fun onOperatorClick_percentAfterBinaryOperator_ignored() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "10+")
        viewModel.onOperatorClick('%')

        assertEquals("10+", viewModel.uiState.expression)
    }

    @Test
    fun onOperatorClick_repeatedPercent_ignored() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "10%")
        viewModel.onOperatorClick('%')

        assertEquals("10%", viewModel.uiState.expression)
        assertEquals("0.1", viewModel.uiState.displayedResult)
    }

    @Test
    fun onDecimalClick_emptyExpression_insertsZeroDot() {
        val viewModel = createViewModel()

        viewModel.onDecimalClick()

        assertEquals("0.", viewModel.uiState.expression)
    }

    @Test
    fun onDecimalClick_alreadyHasDecimalInCurrentNumber_ignored() {
        val viewModel = createViewModel()

        viewModel.onDigitClick('5')
        viewModel.onDecimalClick()
        viewModel.onDecimalClick()

        assertEquals("5.", viewModel.uiState.expression)
    }

    @Test
    fun onDigitClick_afterPercent_ignored() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "10%")
        viewModel.onDigitClick('5')

        assertEquals("10%", viewModel.uiState.expression)
    }

    @Test
    fun onDecimalClick_afterPercent_ignored() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "10%")
        viewModel.onDecimalClick()

        assertEquals("10%", viewModel.uiState.expression)
    }

    @Test
    fun onDecimalClick_commaLocale_usesLocaleSeparator() {
        val viewModel = createViewModel(locale = Locale.GERMANY)

        viewModel.onDigitClick('1')
        viewModel.onDecimalClick()
        viewModel.onDigitClick('5')
        viewModel.onOperatorClick('+')
        viewModel.onDigitClick('2')

        assertEquals("1,5+2", viewModel.uiState.expression)
        assertEquals("3,5", viewModel.uiState.displayedResult)
    }

    @Test
    fun onToggleSignClick_number_togglesSign() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "12")
        viewModel.onToggleSignClick()

        assertEquals("-12", viewModel.uiState.expression)
        assertEquals("-12", viewModel.uiState.displayedResult)

        viewModel.onToggleSignClick()

        assertEquals("12", viewModel.uiState.expression)
        assertEquals("12", viewModel.uiState.displayedResult)
    }

    @Test
    fun onToggleSignClick_afterPlusOperator_convertsToSubtraction() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "5+2")
        viewModel.onToggleSignClick()

        assertEquals("5-2", viewModel.uiState.expression)
        assertEquals("3", viewModel.uiState.displayedResult)
    }

    @Test
    fun onToggleSignClick_afterMinusOperator_convertsToAddition() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "5-2")
        viewModel.onToggleSignClick()

        assertEquals("5+2", viewModel.uiState.expression)
        assertEquals("7", viewModel.uiState.displayedResult)
    }

    @Test
    fun onDeleteClick_dropsLastCharacter() {
        val viewModel = createViewModel()

        viewModel.onDigitClick('5')
        viewModel.onDigitClick('7')
        viewModel.onDeleteClick()

        assertEquals("5", viewModel.uiState.expression)
    }

    @Test
    fun onClearClick_resetsExpression() {
        val viewModel = createViewModel()

        viewModel.onDigitClick('5')
        viewModel.onOperatorClick('+')
        viewModel.onDigitClick('3')
        viewModel.onClearClick()

        assertEquals("", viewModel.uiState.expression)
        assertEquals("0", viewModel.uiState.displayedResult)
    }

    @Test
    fun onCloseParenClick_unbalanced_ignored() {
        val viewModel = createViewModel()

        viewModel.onDigitClick('5')
        viewModel.onCloseParenClick()

        assertEquals("5", viewModel.uiState.expression)
    }

    @Test
    fun onCloseParenClick_afterOpenAndDigit_appendsClose() {
        val viewModel = createViewModel()

        viewModel.onOpenParenClick()
        viewModel.onDigitClick('2')
        viewModel.onCloseParenClick()

        assertEquals("(2)", viewModel.uiState.expression)
    }

    @Test
    fun onCloseParenClick_afterPercent_appendsClose() {
        val viewModel = createViewModel()

        viewModel.onOpenParenClick()
        typeExpression(viewModel, "10%")
        viewModel.onCloseParenClick()

        assertEquals("(10%)", viewModel.uiState.expression)
        assertEquals("0.1", viewModel.uiState.displayedResult)
    }

    @Test
    fun onParenClick_withOpenParenAndValue_closesParen() {
        val viewModel = createViewModel()

        viewModel.onParenClick()
        viewModel.onDigitClick('2')
        viewModel.onParenClick()

        assertEquals("(2)", viewModel.uiState.expression)
        assertEquals("2", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_resultMatchesPin_unlocksAndConsumesToken() = runTest(dispatcher) {
        every { throttle.tryConsume() } returns true
        coEvery { attemptPinUnlock("000123") } returns true

        val viewModel = createViewModel()
        typeExpression(viewModel, "100+23")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.unlocked)
        verify { throttle.tryConsume() }
        verify { throttle.reset() }
    }

    @Test
    fun onEqualsClick_subtractPercent_returnsPercentOfLeftSubtracted() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "10-10%")
        viewModel.onEqualsClick()

        assertEquals("9", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_addPercent_returnsPercentOfLeftAdded() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "10+10%")
        viewModel.onEqualsClick()

        assertEquals("11", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_multiplyByPercent_returnsFractionMultiplier() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "10×10%")
        viewModel.onEqualsClick()

        assertEquals("1", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_divideByPercent_returnsFractionDivisor() {
        val viewModel = createViewModel()

        typeExpression(viewModel, "10÷10%")
        viewModel.onEqualsClick()

        assertEquals("100", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_divideByZero_showsErrorAndSkipsPinLookup() {
        val viewModel = createViewModel(divideByZeroText = "can't divide by 0")

        typeExpression(viewModel, "10÷0")
        viewModel.onEqualsClick()

        assertEquals("can't divide by 0", viewModel.uiState.displayedResult)
        verify(exactly = 0) { throttle.tryConsume() }
        coVerify(exactly = 0) { attemptPinUnlock(any()) }
    }

    @Test
    fun onEqualsClick_shortResult_padsWithLeadingZerosForPinLookup() = runTest(dispatcher) {
        every { throttle.tryConsume() } returns true
        coEvery { attemptPinUnlock("000010") } returns true

        val viewModel = createViewModel()
        typeExpression(viewModel, "5+5")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.unlocked)
        coVerify { attemptPinUnlock("000010") }
    }

    @Test
    fun onEqualsClick_singleDigitResult_padsWithLeadingZerosForPinLookup() = runTest(dispatcher) {
        every { throttle.tryConsume() } returns true
        coEvery { attemptPinUnlock("000007") } returns true

        val viewModel = createViewModel()
        typeExpression(viewModel, "3+4")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.unlocked)
        coVerify { attemptPinUnlock("000007") }
    }

    @Test
    fun onEqualsClick_zeroResult_padsToAllZerosForPinLookup() = runTest(dispatcher) {
        every { throttle.tryConsume() } returns true
        coEvery { attemptPinUnlock("000000") } returns true

        val viewModel = createViewModel()
        typeExpression(viewModel, "5-5")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.unlocked)
        coVerify { attemptPinUnlock("000000") }
    }

    @Test
    fun onEqualsClick_resultDoesNotMatchPin_keepsDisplayAndStaysLocked() = runTest(dispatcher) {
        every { throttle.tryConsume() } returns true
        coEvery { attemptPinUnlock(any()) } returns false

        val viewModel = createViewModel()
        typeExpression(viewModel, "100+23")
        viewModel.onEqualsClick()
        runCurrent()

        assertFalse(viewModel.uiState.unlocked)
        assertEquals("123", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_throttleDenies_doesNotInvokeUnlockOrExposePin() = runTest(dispatcher) {
        every { throttle.tryConsume() } returns false

        val viewModel = createViewModel()
        typeExpression(viewModel, "100+23")
        viewModel.onEqualsClick()
        runCurrent()

        coVerify(exactly = 0) { attemptPinUnlock(any()) }
        assertEquals("123", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_fractionalResultEvenWhenWholePartMatchesPin_skipsPinLookup() = runTest(dispatcher) {
        val viewModel = createViewModel()
        // 100+23.5 = 123.5 — whole part matches PIN "000123" but result is fractional
        typeExpression(viewModel, "100+23.5")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        verify(exactly = 0) { throttle.tryConsume() }
        coVerify(exactly = 0) { attemptPinUnlock(any()) }
    }

    @Test
    fun onEqualsClick_fractionalResultThatRoundsToInteger_skipsPinLookup() = runTest(dispatcher) {
        val viewModel = createViewModel()
        // exact arithmetic: 0.1 + 0.2 = 0.3, not 0
        typeExpression(viewModel, "0.1+0.2")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        verify(exactly = 0) { throttle.tryConsume() }
    }

    @Test
    fun onEqualsClick_negativeResult_skipsPinLookup() = runTest(dispatcher) {
        val viewModel = createViewModel()
        typeExpression(viewModel, "5-10")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        verify(exactly = 0) { throttle.tryConsume() }
        assertEquals("-5", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_resultExceedsPinLength_skipsPinLookup() = runTest(dispatcher) {
        val viewModel = createViewModel()
        typeExpression(viewModel, "1234567")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        verify(exactly = 0) { throttle.tryConsume() }
    }

    @Test
    fun onEqualsClick_invalidExpression_doesNotConsumeThrottleAndKeepsDisplay() = runTest(dispatcher) {
        val viewModel = createViewModel()
        typeExpression(viewModel, "(2+")
        viewModel.onEqualsClick()

        verify(exactly = 0) { throttle.tryConsume() }
        coVerify(exactly = 0) { attemptPinUnlock(any()) }
        assertFalse(viewModel.uiState.unlocked)
    }

    @Test
    fun onEqualsClick_rapidRepeatedTaps_consumeOnlyOneTokenAndOneUnlock() {
        val standardDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(standardDispatcher)
        try {
            runTest(standardDispatcher) {
                every { throttle.tryConsume() } returns true
                val gate = CompletableDeferred<Boolean>()
                coEvery { attemptPinUnlock(any()) } coAnswers { gate.await() }

                val viewModel = createViewModel()
                typeExpression(viewModel, "100+23")

                repeat(5) { viewModel.onEqualsClick() }
                runCurrent()

                verify(exactly = 1) { throttle.tryConsume() }
                coVerify(exactly = 1) { attemptPinUnlock(any()) }

                gate.complete(false)
                advanceUntilIdle()
            }
        } finally {
            Dispatchers.setMain(dispatcher)
        }
    }

    @Test
    fun onDigitClick_currentNumberHas15Digits_ignoresFurtherDigits() {
        val viewModel = createViewModel()

        repeat(20) { viewModel.onDigitClick('9') }

        assertEquals("999999999999999", viewModel.uiState.expression)
    }

    @Test
    fun onDigitClick_perOperandLimitIsIndependent_allowsNewNumberAfterOperator() {
        val viewModel = createViewModel()

        repeat(20) { viewModel.onDigitClick('1') }
        viewModel.onOperatorClick('+')
        repeat(20) { viewModel.onDigitClick('2') }

        assertEquals("111111111111111+222222222222222", viewModel.uiState.expression)
    }

    @Test
    fun onDigitClick_perOperandLimitCountsDigitsOnly_decimalPointDoesNotConsumeLimit() {
        val viewModel = createViewModel()

        viewModel.onDigitClick('1')
        viewModel.onDecimalClick()
        repeat(20) { viewModel.onDigitClick('2') }

        assertEquals("1.22222222222222", viewModel.uiState.expression)
    }

    @Test
    fun appendToExpression_exceedsMaxExpressionLength_ignoresFurtherInput() {
        val viewModel = createViewModel()

        repeat(80) {
            viewModel.onDigitClick('1')
            viewModel.onOperatorClick('+')
        }

        assertTrue(viewModel.uiState.expression.length <= 64)
    }

    @Test
    fun onToggleSignClick_expressionAtMaxLength_ignoresGrowth() {
        val viewModel = createViewModel()
        val expression = "1.11111111111111×222222222222222×333333333333333×444444444444444"
        assertEquals(64, expression.length)
        typeExpression(viewModel, expression)

        viewModel.onToggleSignClick()

        assertEquals(expression, viewModel.uiState.expression)
    }

    @Test
    fun onEqualsClick_resultExceeds15Digits_usesScientificNotation() {
        val viewModel = createViewModel()
        typeExpression(viewModel, "999999999999999×999999999999999")

        viewModel.onEqualsClick()

        assertEquals("9.99999999999998e+29", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_resultIsExtremelySmall_usesScientificNotation() {
        val viewModel = createViewModel()
        typeExpression(viewModel, "1÷999999999999999")

        viewModel.onEqualsClick()

        assertEquals("1e-15", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_resultIsRoundToFifteenSignificantDigits() {
        val viewModel = createViewModel()
        typeExpression(viewModel, "1÷3")

        viewModel.onEqualsClick()

        assertEquals("0.333333333333333", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_fractionalResultRoundedToIntegerForDisplay_skipsPinLookup() = runTest(dispatcher) {
        val viewModel = createViewModel()
        // 1/3*3 = 0.999...99 exact — visually rounds to "1", but must not unlock PIN 000001.
        typeExpression(viewModel, "1÷3×3")
        viewModel.onEqualsClick()
        advanceUntilIdle()

        verify(exactly = 0) { throttle.tryConsume() }
        coVerify(exactly = 0) { attemptPinUnlock(any()) }
        assertEquals("1", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_resultIsSixteenDigitInteger_usesScientificNotation() {
        val viewModel = createViewModel()
        typeExpression(viewModel, "999999999999999+1")

        viewModel.onEqualsClick()

        assertEquals("1e+15", viewModel.uiState.displayedResult)
    }

    @Test
    fun onEqualsClick_resultIsFifteenNines_staysPlain() {
        val viewModel = createViewModel()
        typeExpression(viewModel, "999999999999998+1")

        viewModel.onEqualsClick()

        assertEquals("999999999999999", viewModel.uiState.displayedResult)
    }

    @Test
    fun onUnlockedConsumed_clearsExpressionAndUnlockedFlag() {
        val viewModel = createViewModel()
        viewModel.onDigitClick('1')
        viewModel.onUnlockedConsumed()

        assertEquals("", viewModel.uiState.expression)
        assertFalse(viewModel.uiState.unlocked)
    }

    private fun typeExpression(
        viewModel: CalculatorLockScreenViewModel,
        expression: String,
    ) {
        for (ch in expression) {
            when (ch) {
                in '0'..'9' -> viewModel.onDigitClick(ch)
                '+', '-', '×', '÷', '%' -> viewModel.onOperatorClick(ch)
                '*' -> viewModel.onOperatorClick('×')
                '/' -> viewModel.onOperatorClick('÷')
                '.' -> viewModel.onDecimalClick()
                '(' -> viewModel.onOpenParenClick()
                ')' -> viewModel.onCloseParenClick()
            }
        }
    }
}
