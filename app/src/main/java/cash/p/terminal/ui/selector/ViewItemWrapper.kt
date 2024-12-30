package cash.p.terminal.ui.selector

import cash.p.terminal.R
import cash.p.terminal.strings.helpers.Translator

class ViewItemWrapper<T>(val title: String, val item: T, val color: Int? = null, val subtitle: String? = null) {
    override fun equals(other: Any?) = when {
        other !is ViewItemWrapper<*> -> false
        else -> item == other.item
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (item?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun <T>getAny(): ViewItemWrapper<T?> {
            return ViewItemWrapper(cash.p.terminal.strings.helpers.Translator.getString(R.string.Any), null, R.color.grey)
        }
    }
}
