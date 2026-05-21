package cash.p.terminal.modules.calculator.lockscreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.modules.calculator.domain.CalculatorEvalResult
import cash.p.terminal.modules.calculator.domain.CalculatorExpressionEvaluator
import cash.p.terminal.modules.calculator.domain.CalculatorPinAttemptThrottle
import cash.p.terminal.modules.pin.PinModule
import cash.p.terminal.modules.pin.unlock.AttemptPinUnlockUseCase
import cash.p.terminal.strings.helpers.Translator
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

class CalculatorLockScreenViewModel(
    private val throttle: CalculatorPinAttemptThrottle,
    private val evaluator: CalculatorExpressionEvaluator,
    private val attemptPinUnlock: AttemptPinUnlockUseCase,
    locale: Locale = Locale.getDefault(),
    private val divideByZeroText: String = Translator.getString(R.string.calculator_error_divide_by_zero),
) : ViewModel() {

    private val decimalSeparator: Char =
        DecimalFormatSymbols.getInstance(locale).decimalSeparator

    private var attemptInFlight = false

    var uiState by mutableStateOf(
        CalculatorLockScreenUiState(
            expression = "",
            displayedResult = "0",
            decimalSeparator = decimalSeparator,
            unlocked = false,
        )
    )
        private set

    fun onDigitClick(digit: Char) {
        if (!digit.isDigit()) return
        if (uiState.expression.lastOrNull() == '%') return
        if (currentNumber().count { it.isDigit() } >= MAX_DIGITS_PER_NUMBER) return
        appendToExpression(digit.toString())
    }

    private fun currentNumber(): String =
        uiState.expression.takeLastWhile { it.isDigit() || it == decimalSeparator }

    fun onOperatorClick(operator: Char) {
        if (operator !in OPERATORS) return
        if (operator == '%') {
            appendPercent()
            return
        }

        val current = uiState.expression
        if (current.isEmpty()) {
            if (operator != '-') return
            updateExpression(operator.toString())
            return
        }
        val last = current.last()
        if (last in BINARY_OPERATORS) {
            updateExpression(current.dropLast(1) + operator)
        } else {
            appendToExpression(operator.toString())
        }
    }

    private fun appendPercent() {
        val current = uiState.expression
        val last = current.lastOrNull() ?: return
        if (last == '%') return
        if (!last.isDigit() && last != ')') return
        appendToExpression("%")
    }

    fun onDecimalClick() {
        if (uiState.expression.lastOrNull() == '%') return
        val lastNumber = currentNumber()
        if (lastNumber.contains(decimalSeparator)) return
        if (lastNumber.isEmpty()) {
            appendToExpression("0$decimalSeparator")
        } else {
            appendToExpression(decimalSeparator.toString())
        }
    }

    fun onOpenParenClick() {
        appendToExpression("(")
    }

    fun onCloseParenClick() {
        val current = uiState.expression
        val open = current.count { it == '(' }
        val close = current.count { it == ')' }
        if (open <= close) return
        if (current.isEmpty() || current.last() in BINARY_OPERATORS || current.last() == '(') return
        appendToExpression(")")
    }

    fun onParenClick() {
        if (canClose()) onCloseParenClick() else onOpenParenClick()
    }

    fun onToggleSignClick() {
        val current = uiState.expression
        if (current.isEmpty()) return

        val numberStart = lastNumberStart(current)
        if (numberStart == current.length) {
            stripTrailingUnaryMinus(current)
            return
        }
        toggleSignBeforeNumber(current, numberStart)
    }

    private fun canClose(): Boolean {
        val current = uiState.expression
        val open = current.count { it == '(' }
        val close = current.count { it == ')' }
        if (open <= close) return false
        val last = current.lastOrNull() ?: return false
        return last !in BINARY_OPERATORS && last != '('
    }

    private fun lastNumberStart(expression: String): Int =
        expression.length - expression.takeLastWhile {
            it.isDigit() || it == decimalSeparator
        }.length

    private fun stripTrailingUnaryMinus(expression: String) {
        if (expression.last() != '-') return
        val before = expression.getOrNull(expression.length - 2)
        if (before == null || before == '(' || before in BINARY_OPERATORS) {
            updateExpression(expression.dropLast(1))
        }
    }

    private fun toggleSignBeforeNumber(expression: String, numberStart: Int) {
        if (numberStart == 0) {
            updateExpression("-$expression")
            return
        }
        val prev = expression[numberStart - 1]
        val beforePrev = expression.getOrNull(numberStart - 2)
        val prevIsUnary = beforePrev == null || beforePrev == '(' || beforePrev in BINARY_OPERATORS
        val prefix = expression.substring(0, numberStart - 1)
        val suffix = expression.substring(numberStart)
        val updated = when {
            prev == '-' && prevIsUnary -> prefix + suffix
            prev == '-' -> "$prefix+$suffix"
            prev == '+' -> "$prefix-$suffix"
            prev == '(' || prev in BINARY_OPERATORS ->
                expression.substring(0, numberStart) + '-' + suffix
            else -> return
        }
        updateExpression(updated)
    }

    fun onDeleteClick() {
        val current = uiState.expression
        if (current.isEmpty()) return
        updateExpression(current.dropLast(1))
    }

    fun onClearClick() {
        uiState = uiState.copy(
            expression = "",
            displayedResult = "0",
        )
    }

    fun onEqualsClick() {
        if (attemptInFlight) return
        val expression = uiState.expression
        if (expression.isEmpty()) return

        when (val result = evaluator.evaluate(expression, decimalSeparator)) {
            is CalculatorEvalResult.DivideByZero -> showDivideByZero()
            is CalculatorEvalResult.Invalid -> return
            is CalculatorEvalResult.Success -> handleEvaluatedValue(result.value)
        }
    }

    fun onUnlockedConsumed() {
        uiState = uiState.copy(
            unlocked = false,
            expression = "",
            displayedResult = "0",
        )
    }

    private fun handleEvaluatedValue(value: BigDecimal) {
        uiState = uiState.copy(displayedResult = formatForDisplay(value))

        val candidatePin = value.toPinCandidate() ?: return
        if (!throttle.tryConsume()) return

        attemptInFlight = true
        viewModelScope.launch {
            try {
                if (attemptPinUnlock(candidatePin)) {
                    throttle.reset()
                    uiState = uiState.copy(unlocked = true)
                }
            } finally {
                attemptInFlight = false
            }
        }
    }

    private fun appendToExpression(text: String) {
        updateExpression(uiState.expression + text)
    }

    private fun updateExpression(newExpression: String) {
        if (newExpression.length > MAX_EXPRESSION_LENGTH) return
        val live = liveResultOrNull(newExpression)
        uiState = uiState.copy(
            expression = newExpression,
            displayedResult = live ?: uiState.displayedResult,
        )
    }

    private fun liveResultOrNull(expression: String): String? {
        if (expression.isEmpty()) return "0"
        if (expression.last() in BINARY_OPERATORS) return null
        val open = expression.count { it == '(' }
        val close = expression.count { it == ')' }
        if (open != close) return null
        return when (val result = evaluator.evaluate(expression, decimalSeparator)) {
            is CalculatorEvalResult.DivideByZero -> divideByZeroText
            is CalculatorEvalResult.Invalid -> null
            is CalculatorEvalResult.Success -> formatForDisplay(result.value)
        }
    }

    private fun showDivideByZero() {
        uiState = uiState.copy(displayedResult = divideByZeroText)
    }

    private fun formatForDisplay(value: BigDecimal): String {
        val formatted = formatNumber(value)
        return if (decimalSeparator == '.') formatted else formatted.replace('.', decimalSeparator)
    }

    private fun formatNumber(value: BigDecimal): String {
        if (value.signum() == 0) return "0"
        val rounded = value.round(DISPLAY_CONTEXT).stripTrailingZeros()
        val absRounded = rounded.abs()
        val needsScientific = absRounded >= SCIENTIFIC_THRESHOLD_LARGE ||
            absRounded < SCIENTIFIC_THRESHOLD_SMALL
        return if (needsScientific) scientificString(rounded) else plainString(rounded)
    }

    private fun plainString(value: BigDecimal): String =
        if (value.scale() <= 0) {
            value.toBigInteger().toString()
        } else {
            value.toPlainString()
        }

    private fun scientificString(value: BigDecimal): String {
        val formatted = String.format(
            Locale.US,
            "%." + (MAX_SIGNIFICANT_DIGITS - 1) + "e",
            value,
        )
        val (mantissa, exponent) = formatted.split('e')
        val cleanMantissa = mantissa.trimEnd('0').trimEnd('.')
        return cleanMantissa + "e" + exponent
    }

    private fun BigDecimal.toPinCandidate(): String? {
        if (signum() < 0) return null
        val stripped = stripTrailingZeros()
        if (stripped.scale() > 0) return null
        val digits = stripped.toBigInteger().toString()
        if (digits.length > PinModule.PIN_COUNT) return null
        return digits.padStart(PinModule.PIN_COUNT, '0')
    }

    companion object {
        private const val MAX_DIGITS_PER_NUMBER = 15
        private const val MAX_EXPRESSION_LENGTH = 64
        private const val MAX_SIGNIFICANT_DIGITS = 15
        private val DISPLAY_CONTEXT = MathContext(MAX_SIGNIFICANT_DIGITS, RoundingMode.HALF_UP)
        private val SCIENTIFIC_THRESHOLD_LARGE: BigDecimal = BigDecimal.TEN.pow(MAX_SIGNIFICANT_DIGITS)
        private val SCIENTIFIC_THRESHOLD_SMALL: BigDecimal = BigDecimal("1E-6")
        private val BINARY_OPERATORS = setOf('+', '-', '×', '÷')
        private val OPERATORS = BINARY_OPERATORS + '%'
    }
}

data class CalculatorLockScreenUiState(
    val expression: String,
    val displayedResult: String,
    val decimalSeparator: Char,
    val unlocked: Boolean,
)
