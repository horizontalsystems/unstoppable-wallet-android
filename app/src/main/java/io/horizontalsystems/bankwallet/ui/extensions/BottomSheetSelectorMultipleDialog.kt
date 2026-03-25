package io.horizontalsystems.bankwallet.ui.extensions

import java.util.UUID

class BottomSheetSelectorMultipleDialog {
    data class Config(
        val title: String,
        val selectedIndexes: List<Int>,
        val viewItems: List<BottomSheetSelectorViewItem>,
        val descriptionTitle: String? = null,
        val allowEmpty: Boolean = false
    ) {
        val uuid = UUID.randomUUID().toString()
    }
}

data class BottomSheetSelectorViewItem(
    val title: String,
    val subtitle: String,
    val copyableString: String? = null,
    val icon: String? = null
)
