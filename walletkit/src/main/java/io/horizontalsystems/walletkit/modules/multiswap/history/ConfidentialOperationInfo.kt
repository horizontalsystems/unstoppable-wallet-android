package io.horizontalsystems.walletkit.modules.multiswap.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.sendevmtransaction.AddressCell
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsImageCircle
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellSecondary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

/**
 * Private send: the same token goes in and out, so the top card mirrors the send
 * confirmation — what the recipient receives, then the recipient. The deposit address is
 * never shown; it would leak the rail.
 */
@Composable
internal fun PrivateSendInfoContent(uiState: SwapInfoUiState, navigation: HSNavigation) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence),
    ) {
        TokenAmountRow(
            imageUrl = uiState.tokenOutImageUrl,
            alternativeImageUrl = uiState.tokenOutAlternativeImageUrl,
            code = uiState.tokenOutCode,
            badge = uiState.tokenOutBadge,
            amount = uiState.amountOut ?: uiState.amountIn,
            fiatAmount = uiState.fiatAmountOut ?: uiState.fiatAmountIn,
        )
        uiState.recipientAddress?.let { address ->
            DividerWithArrow()
            AddressCell(
                address = address,
                contact = uiState.recipientContactName,
            )
        }
    }

    VSpacer(16.dp)

    DetailsCard {
        DateRow(uiState.formattedDate)
        uiState.estimatedArrival?.let { arrival ->
            EstimatedArrivalRow(arrival, navigation)
        }
    }
}

/**
 * CrossPay: a token pair like a swap, and always a recipient — the payment lands in the
 * receiver's wallet in the token they asked for.
 */
@Composable
internal fun CrossPayInfoContent(uiState: SwapInfoUiState, navigation: HSNavigation) {
    val view = LocalView.current

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence),
    ) {
        TokenAmountRow(
            imageUrl = uiState.tokenInImageUrl,
            alternativeImageUrl = uiState.tokenInAlternativeImageUrl,
            code = uiState.tokenInCode,
            badge = uiState.tokenInBadge,
            amount = uiState.amountIn,
            fiatAmount = uiState.fiatAmountIn,
        )
        DividerWithArrow()
        TokenAmountRow(
            imageUrl = uiState.tokenOutImageUrl,
            alternativeImageUrl = uiState.tokenOutAlternativeImageUrl,
            code = uiState.tokenOutCode,
            badge = uiState.tokenOutBadge,
            amount = uiState.amountOut ?: "---",
            fiatAmount = uiState.fiatAmountOut,
        )
    }

    VSpacer(16.dp)

    DetailsCard {
        DateRow(uiState.formattedDate)
        uiState.estimatedArrival?.let { arrival ->
            EstimatedArrivalRow(arrival, navigation)
        }
        uiState.recipientAddress?.let { address ->
            RecipientRow(address = address, view = view)
        }
    }
}

@Composable
private fun TokenAmountRow(
    imageUrl: String,
    alternativeImageUrl: String?,
    code: String,
    badge: String?,
    amount: String,
    fiatAmount: String?,
) {
    val leah = ComposeAppTheme.colors.leah
    CellPrimary(
        left = {
            HsImageCircle(
                modifier = Modifier.size(32.dp),
                url = imageUrl,
                alternativeUrl = alternativeImageUrl,
                placeholder = R.drawable.coin_placeholder,
            )
        },
        middle = {
            CellMiddleInfo(
                eyebrow = code.hs(color = leah),
                subtitle = (badge ?: stringResource(R.string.CoinPlatforms_Native)).hs,
            )
        },
        right = {
            CellRightInfo(
                eyebrow = amount.hs(color = leah),
                subtitle = fiatAmount?.hs,
            )
        },
    )
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 8.dp),
    ) {
        content()
    }
}

@Composable
private fun DateRow(formattedDate: String) {
    CellSecondary(
        middle = {
            CellMiddleInfoTextIcon(text = stringResource(R.string.TransactionInfo_Date).hs)
        },
        right = {
            CellRightInfoTextIcon(text = formattedDate.hs(color = ComposeAppTheme.colors.leah))
        },
    )
}

@Composable
private fun EstimatedArrivalRow(arrival: String, navigation: HSNavigation) {
    val title = stringResource(R.string.PrivateSend_EstimatedTime)
    val description = stringResource(R.string.Swap_EstimatedTimeDescription)
    CellSecondary(
        middle = {
            CellMiddleInfoTextIcon(
                text = title.hs,
                icon = painterResource(R.drawable.ic_info_24),
                iconTint = ComposeAppTheme.colors.grey,
                onIconClick = {
                    navigation.slideFromBottom(
                        SwapInfoSheet(SwapInfoSheet.Input(title, description, R.drawable.ic_circle_clock_24))
                    )
                },
            )
        },
        right = {
            CellRightInfoTextIcon(text = arrival.hs(color = ComposeAppTheme.colors.leah))
        },
    )
}
