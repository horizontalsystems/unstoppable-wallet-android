package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

@Composable
fun FeeRow(
    title: String,
    valueFiat: String? = null,
    valueToken: String,
    onInfoClick: (() -> Unit)? = null,
) {
    var showFiat by remember { mutableStateOf(valueFiat != null) }
    val displayedValue = if (showFiat) valueFiat else valueToken
    val onClick = { showFiat = !showFiat }

    CellSecondary(
        middle = {
            if (onInfoClick != null) {
                CellMiddleInfoTextIcon(
                    text = title.hs(color = ComposeAppTheme.colors.grey),
                    icon = painterResource(R.drawable.ic_info_filled_20),
                    iconTint = ComposeAppTheme.colors.grey,
                    onIconClick = onInfoClick
                )
            } else {
                CellMiddleInfo(
                    eyebrow = title.hs
                )
            }
        },
        right = {
            CellRightInfo(
                eyebrow = displayedValue?.hs(ComposeAppTheme.colors.leah),
                onClick = if (valueFiat != null) onClick else null,
            )
        },
    )
}

@Preview
@Composable
fun FeeRowPreview() {
    ComposeAppTheme {
        FeeRow(
            title = stringResource(R.string.Swap_Recipient),
            valueFiat = "$3.23",
            valueToken = "0x7A04536a50d12952f69E071e4c92693939db86b5"
        )
    }
}