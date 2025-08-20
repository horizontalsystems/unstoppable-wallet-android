package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey

@Composable
fun CellRightInfo(
    eyebrow: String? = null,
    title: String,
    subtitle: String? = null,
    description: String? = null,
) {
    Column(horizontalAlignment = Alignment.End) {
        eyebrow?.let {
            subhead_grey(it)
        }

        headline2_leah(title)

        subtitle?.let {
            subhead_grey(it)
        }

        description?.let {
            captionSB_grey(it)
        }
    }
}
