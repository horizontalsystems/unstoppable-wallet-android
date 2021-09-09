package io.horizontalsystems.bankwallet.ui.selector

class ViewItemWithIconWrapper<T>(val title: String, val item: T, val iconName: String, val subtitle: String? = null) {
    override fun equals(other: Any?) = when (other) {
        !is ViewItemWithIconWrapper<*> -> false
        else -> item == other.item
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (item?.hashCode() ?: 0)
        return result
    }
}
