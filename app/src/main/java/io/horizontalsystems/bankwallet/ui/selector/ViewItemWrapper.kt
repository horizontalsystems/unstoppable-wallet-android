package io.horizontalsystems.bankwallet.ui.selector

class ViewItemWrapper<T>(val title: String, val item: T, val color: Int) {
    override fun equals(other: Any?) = when {
        other !is ViewItemWrapper<*> -> false
        else -> item == other.item
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (item?.hashCode() ?: 0)
        return result
    }
}
