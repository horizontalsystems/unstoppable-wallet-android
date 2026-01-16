package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.NftIcon
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType

@Composable
fun SendEvmTransactionView(
    navController: NavController,
    items: List<SectionViewItem>,
    cautions: List<CautionViewItem>,
    transactionFields: List<DataField>,
    networkFee: SendModule.AmountData?,
    statPage: StatPage
) {
    Column {
        items.forEach { sectionViewItem ->
            SectionView(sectionViewItem.viewItems, navController, statPage)
            VSpacer(16.dp)
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding(vertical = 8.dp)
        ) {
            if (transactionFields.isNotEmpty()) {
                transactionFields.forEachIndexed { index, field ->
                    field.GetContent(navController)
                }
            }
            DataFieldFee(
                navController,
                networkFee?.primary?.getFormattedPlain() ?: "---",
                networkFee?.secondary?.getFormattedPlain()
            )
        }

        if (cautions.isNotEmpty()) {
            Cautions(cautions)
        }
    }
}

@Composable
fun SectionView(viewItems: List<ViewItem>, navController: NavController, statPage: StatPage) {
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
                    navController,
                    item.networkFee.primary.getFormattedPlain() ?: "---",
                    item.networkFee.secondary?.getFormattedPlain() ?: "---"
                )
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
private fun Subhead(item: ViewItem.Subhead) {
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
private fun Amount(item: ViewItem.Amount) {
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
private fun AmountWithTitle(item: ViewItem.AmountWithTitle) {
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
                eyebrow = item.coinAmount.hs,
                subtitle = item.fiatAmount?.hs
            )
        }
    )
}

@Composable
private fun NftAmount(item: ViewItem.NftAmount) {
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
private fun Token(item: ViewItem.TokenItem) {
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
private fun TitleValueHex(
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
private fun setColorByType(type: ValueType) =
    when (type) {
        ValueType.Regular -> ComposeAppTheme.colors.leah
        ValueType.Disabled -> ComposeAppTheme.colors.grey
        ValueType.Outgoing -> ComposeAppTheme.colors.leah
        ValueType.Incoming -> ComposeAppTheme.colors.remus
        ValueType.Warning -> ComposeAppTheme.colors.jacob
        ValueType.Forbidden -> ComposeAppTheme.colors.lucian
    }

@Preview
@Composable
private fun Preview_Subhead() {
    val item = ViewItem.Subhead("Title", "Value", R.drawable.ic_arrow_down_left_24)
    ComposeAppTheme {
        Subhead(item)
    }
}

@Preview
@Composable
private fun Preview_TitleValue() {
    val item = ViewItem.Value("Title", "Value", ValueType.Incoming)
    ComposeAppTheme {
        TitleValue(item)
    }
}

@Preview
@Composable
private fun Preview_Amount() {
    val token = Token(
        coin = Coin("uid", "KuCoin", "KCS"),
        blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
        type = TokenType.Eip20("eef"),
        decimals = 18
    )
    val item = ViewItem.Amount(
        "$0.99",
        "0.104 KCS (est)",
        ValueType.Outgoing,
        token
    )
    ComposeAppTheme {
        Amount(item)
    }
}

@Preview
@Composable
private fun Preview_TitleValueHex() {
    ComposeAppTheme {
        TitleValueHex("Title", "ValueShort", "ValueLong")
    }
}
