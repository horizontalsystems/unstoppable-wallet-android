package io.horizontalsystems.walletkit.modules.sendevmtransaction

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
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.evmfee.Cautions
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.CoinImage
import io.horizontalsystems.walletkit.ui.compose.components.NftIcon
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline2_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType

@Composable
fun SendEvmTransactionView(
    navigation: HSNavigation,
    items: List<SectionViewItem>,
    cautions: List<CautionViewItem>,
    transactionFields: List<DataField>,
    networkFee: SendModule.AmountData?,
    statPage: StatPage
) {
    Column {
        items.forEach { sectionViewItem ->
            SectionView(sectionViewItem.viewItems, navigation, statPage)
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
                    field.GetContent(navigation)
                }
            }
            DataFieldFee(
                navigation,
                networkFee?.primary?.getFormattedPlain() ?: "---",
                networkFee?.secondary?.getFormattedPlain()
            )
        }

        if (cautions.isNotEmpty()) {
            Cautions(cautions)
        }
    }
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
