package io.horizontalsystems.bankwallet.uiv3.components.section

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_andy
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_leah

@Composable
fun SectionHeaderAndy(
    title: String,
    icon: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Image(
                painter = painterResource(icon),
                modifier = Modifier.size(20.dp),
                contentDescription = null
            )
            HSpacer(8.dp)
        }
        subheadSB_andy(text = title)
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Image(
                painter = painterResource(icon),
                modifier = Modifier.size(20.dp),
                contentDescription = null
            )
            HSpacer(8.dp)
        }
        subheadSB_leah(text = title)
    }
}