package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import io.horizontalsystems.bankwallet.R

@Composable
fun HsRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val painterResource = if (selected) {
        painterResource(id = R.drawable.ic_radion)
    } else {
        painterResource(id = R.drawable.ic_radioff)
    }

    HsIconButton(
        onClick = onClick
    ) {
        Image(
            painter = painterResource,
            contentDescription = null
        )
    }

}
