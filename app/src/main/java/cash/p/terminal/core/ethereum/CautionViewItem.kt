package cash.p.terminal.core.ethereum

import cash.p.terminal.core.HSCaution

data class CautionViewItem(val title: String, val text: String, val type: Type) {
    enum class Type {
        Error, Warning
    }
}

fun HSCaution.toCautionViewItem(): CautionViewItem {
    return CautionViewItem(
        title = s.toString(),
        text = description?.toString() ?: "",
        type = when (type) {
            HSCaution.Type.Error -> CautionViewItem.Type.Error
            HSCaution.Type.Warning -> CautionViewItem.Type.Warning
        }
    )
}
