package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.captionSB_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey

@Composable
fun CellMiddleInfo(
    eyebrow: String? = null,
    title: String,
    badge: String? = null,
    subtitleBadge: String? = null,
    subtitle: String? = null,
    subtitle2: String? = null,
    description: String? = null,
) {
    Column {
        eyebrow?.let {
            subhead_grey(it)
        }

        Row(horizontalArrangement = spacedBy(8.dp)) {
            headline2_leah(title)
            badge?.let {
                Badge(text = it)
            }
        }

        Row(horizontalArrangement = spacedBy(8.dp)) {
            subtitleBadge?.let {
                Badge(text = it)
            }
            Row(horizontalArrangement = spacedBy(4.dp)) {
                subtitle?.let {
                    subhead_grey(it)
                }
                subtitle2?.let {
                    subhead_grey(it)
                }
            }
        }

        description?.let {
            captionSB_grey(it)
        }
    }
}
