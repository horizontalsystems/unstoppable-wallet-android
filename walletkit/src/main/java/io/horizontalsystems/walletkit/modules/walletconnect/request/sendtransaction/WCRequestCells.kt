package io.horizontalsystems.walletkit.modules.walletconnect.request.sendtransaction

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.walletkit.modules.walletconnect.session.TitleValueCell
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellSecondary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

@Composable
fun DataBlock(
    sections: List<SectionViewItem>,
    onInfoClick: () -> Unit,
    onCopy: (String) -> Unit
) {
    sections.forEach { section ->
        section.viewItems.forEach { item ->
            when (item) {
                is ViewItem.Value -> TitleValueCell(item.title, item.value)

                is ViewItem.Amount -> TitleValueCell(
                    stringResource(R.string.WalletConnect_Value),
                    item.coinAmount
                )

                is ViewItem.AmountWithTitle -> TitleValueCell(
                    item.title,
                    item.coinAmount
                )

                is ViewItem.Address -> CopiableValueCell(item.title, item.address, onCopy)
                is ViewItem.Input -> CopiableValueCell(item.title, item.value, onCopy)
                is ViewItem.Fee -> FeeCell(
                    primaryValue = item.networkFee.primary.getFormatted(),
                    secondaryValue = item.networkFee.secondary?.getFormatted(),
                    onInfoClick = onInfoClick
                )
                else -> {
                    // do nothing
                }
            }
        }
    }
}

@Composable
fun CautionCell(caution: CautionViewItem) {
    val alertType = when (caution.type) {
        CautionViewItem.Type.Error -> AlertType.Critical
        CautionViewItem.Type.Warning -> AlertType.Caution
    }

    AlertCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        format = AlertFormat.Structured,
        type = alertType,
        text = caution.text,
        titleCustom = caution.title
    )
}

@Composable
fun CopiableValueCell(
    title: String,
    value: String,
    onCopy: ((String) -> Unit)? = null
) {
    val shortedValue = value.shorten()
    val copiedMessage = stringResource(R.string.Hud_Text_Copied)

    CellSecondary(
        middle = {
            CellMiddleInfo(
                subtitle = title.hs,
            )
        },
        right = {
            CellRightControlsButtonText(
                subtitle = shortedValue.hs,
                icon = painterResource(id = R.drawable.copy_filled_24),
                iconTint = ComposeAppTheme.colors.leah
            ) {
                TextHelper.copyText(value)
                onCopy?.invoke(copiedMessage)
            }
        },
    )
}

@Composable
fun FeeCell(
    primaryValue: String?,
    secondaryValue: String?,
    onInfoClick: () -> Unit
) {
    CellSecondary(
        middle = {
            CellMiddleInfoTextIcon(
                text = stringResource(R.string.Send_Fee).hs,
                icon = painterResource(R.drawable.info_filled_24),
                iconTint = ComposeAppTheme.colors.grey,
                onIconClick = onInfoClick,
            )
        },
        right = {
            CellRightInfo(
                title = primaryValue?.hs ?: "".hs,
                subtitle = secondaryValue?.hs ?: "".hs
            )
        },
    )
}
