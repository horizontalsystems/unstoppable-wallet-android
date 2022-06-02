package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CoinListHeaderWithInfoButton(
    titleTextRes: Int,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead1_grey(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp).weight(1f),
            text = stringResource(titleTextRes).uppercase(),
        )
        //IconButton has own padding, that's pushes 16.dp from end
        HsIconButton(
            onClick = onInfoClick
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info_20),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        }
    }
}
