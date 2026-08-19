package io.horizontalsystems.walletkit.modules.sendevmtransaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.CoinImage
import io.horizontalsystems.walletkit.ui.compose.components.NftIcon
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.headline2_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.hs


@Composable
fun SectionView(viewItems: List<ViewItem>, navigation: HSNavigation, statPage: StatPage) {
    Box {
        CellUniversalLawrenceSection(viewItems) { item ->
            when (item) {
                is ViewItem.Subhead -> Subhead(item)
                is ViewItem.Value -> TitleValue(item)
                is ViewItem.Amount -> Amount(item)
                is ViewItem.AmountWithTitle -> AmountWithTitle(item)
                is ViewItem.NftAmount -> NftAmount(item)
                is ViewItem.Address -> AddressCell(address = item.address, contact = item.contact)
                is ViewItem.Input -> TitleValueHex(item.title, item.value.shorten(), item.value)
                is ViewItem.TokenItem -> Token(item)
                is ViewItem.Fee -> DataFieldFee(
                    navigation,
                    item.networkFee.primary.getFormattedPlain() ?: "---",
                    item.networkFee.secondary?.getFormattedPlain() ?: "---"
                )
                // Alert is only produced in the WalletConnect request flow (rendered by DataBlock);
                // it does not appear in these EVM section views.
                is ViewItem.Alert -> {}
            }
        }
        if (viewItems.size == 2) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 57.dp) //top cell is 67.dp - iconWidth/2(which is equal 10.dp)
                    .clip(CircleShape)
                    .background(ComposeAppTheme.colors.lawrence)
            )
        }
    }
}

@Composable
fun AddressCell(
    address: String,
    contact: String?,
) {
    val image = if (contact != null) R.drawable.user_wrapped_32 else R.drawable.wallet_wrapped_32
    val description = if (contact != null) address else null
    CellPrimary(
        left = {
            Image(
                painter = painterResource(image),
                modifier = Modifier.size(32.dp),
                contentDescription = null
            )
        },
        middle = {
            CellMiddleInfo(
                eyebrow = (contact ?: address).hs(color = ComposeAppTheme.colors.leah),
                subtitle = description?.hs
            )
        },
    )
}

@Composable
fun Subhead(item: ViewItem.Subhead) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        item.iconRes?.let {
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                painter = painterResource(id = it),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        headline2_leah(
            text = item.title
        )
        Spacer(Modifier.weight(1f))
        subhead1_grey(
            text = item.value
        )
    }
}

@Composable
fun TitleValue(item: ViewItem.Value) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        subhead2_grey(
            text = item.title
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = item.value,
            maxLines = 1,
            style = ComposeAppTheme.typography.subhead,
            color = setColorByType(item.type)
        )
    }
}

@Composable
fun Amount(item: ViewItem.Amount) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CoinImage(
            token = item.token,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
        )
        Text(
            text = item.coinAmount,
            maxLines = 1,
            style = ComposeAppTheme.typography.subhead,
            color = setColorByType(item.type)
        )
        Spacer(Modifier.weight(1f))
        subhead2_grey(
            text = item.fiatAmount ?: ""
        )
    }
}

@Composable
fun AmountWithTitle(item: ViewItem.AmountWithTitle) {
    CellPrimary(
        left = {
            CoinImage(
                token = item.token,
                modifier = Modifier.size(32.dp)
            )
        },
        middle = {
            CellMiddleInfo(
                eyebrow = item.title.hs(color = ComposeAppTheme.colors.leah),
                subtitle = (item.badge ?: stringResource(id =R.string.CoinPlatforms_Native)).hs
            )
        },
        right = {
            CellRightInfo(
                eyebrow = item.coinAmount.hs(color = ComposeAppTheme.colors.leah),
                subtitle = item.fiatAmount?.hs
            )
        }
    )
}

@Composable
fun NftAmount(item: ViewItem.NftAmount) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        NftIcon(
            modifier = Modifier.padding(end = 16.dp),
            iconUrl = item.iconUrl,
        )
        Text(
            text = item.amount,
            maxLines = 1,
            style = ComposeAppTheme.typography.subheadR,
            color = setColorByType(item.type)
        )
    }
}

@Composable
fun Token(item: ViewItem.TokenItem) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CoinImage(
            token = item.token,
            modifier = Modifier.padding(end = 16.dp).size(32.dp)
        )
        subhead1_leah(item.token.coin.code)
    }
}

@Composable
fun TitleValueHex(
    title: String,
    valueTitle: String,
    value: String,
) {
    val localView = LocalView.current
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        subhead2_grey(
            text = title
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            modifier = Modifier.height(28.dp),
            title = valueTitle,
            onClick = {
                TextHelper.copyText(value)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
            }
        )
    }
}

@Composable
fun setColorByType(type: ValueType) =
    when (type) {
        ValueType.Regular -> ComposeAppTheme.colors.leah
        ValueType.Disabled -> ComposeAppTheme.colors.grey
        ValueType.Outgoing -> ComposeAppTheme.colors.leah
        ValueType.Incoming -> ComposeAppTheme.colors.remus
        ValueType.Warning -> ComposeAppTheme.colors.jacob
        ValueType.Forbidden -> ComposeAppTheme.colors.lucian
    }