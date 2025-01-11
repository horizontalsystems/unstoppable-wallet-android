package cash.p.terminal.modules.coin.indicators

import cash.p.terminal.R
import cash.p.terminal.strings.helpers.Translator

class NotIntegerException : Exception() {
    override fun getLocalizedMessage(): String {
        return cash.p.terminal.strings.helpers.Translator.getString(R.string.Error_NotInteger)
    }
}

class OutOfRangeException(val lower: Int, val upper: Int) : Exception() {
    override fun getLocalizedMessage(): String {
        return cash.p.terminal.strings.helpers.Translator.getString(R.string.Error_OutOfRange, lower, upper)
    }
}