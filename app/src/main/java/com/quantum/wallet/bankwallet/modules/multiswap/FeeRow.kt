package com.quantum.wallet.bankwallet.modules.multiswap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellSecondary
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs

@Composable
fun FeeRow(
    title: String,
    valueFiat: String? = null,
    valueToken: String,
    onInfoClick: (() -> Unit)? = null,
) {
    var showFiat by remember(valueFiat) { mutableStateOf(false) }

    val displayedValue = when {
        showFiat && valueFiat != null -> valueFiat
        else -> valueToken
    }

    val middleContent: @Composable () -> Unit = {
        if (onInfoClick != null) {
            CellMiddleInfoTextIcon(
                text = title.hs(color = ComposeAppTheme.colors.grey),
                icon = painterResource(R.drawable.ic_info_filled_20),
                iconTint = ComposeAppTheme.colors.grey,
                onIconClick = onInfoClick
            )
        } else {
            CellMiddleInfo(eyebrow = title.hs)
        }
    }

    CellSecondary(
        middle = middleContent,
        right = {
            CellRightInfo(
                eyebrow = displayedValue.hs(ComposeAppTheme.colors.leah),
                onClick = if (valueFiat != null) {
                    { showFiat = !showFiat }
                } else {
                    null
                },
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