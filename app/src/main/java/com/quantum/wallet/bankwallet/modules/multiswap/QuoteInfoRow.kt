package com.quantum.wallet.bankwallet.modules.multiswap

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellSecondary
import com.quantum.wallet.bankwallet.uiv3.components.cell.HSString
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs

@Composable
fun QuoteInfoRow(
    title: String,
    value: HSString,
    valueSecondary: HSString? = null,
    onInfoClick: (() -> Unit)? = null,
    onCellClick: (() -> Unit)? = null,
) {
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
                eyebrow = value,
                subtitle = valueSecondary
            )
        },
        onClick = onCellClick
    )
}

@Preview
@Composable
fun QuoteInfoRowPreview() {
    ComposeAppTheme {
        QuoteInfoRow(
            title = stringResource(R.string.Swap_Recipient),
            value = "0x7A04536a50d12952f69E071e4c92693939db86b5".hs
        )
    }
}