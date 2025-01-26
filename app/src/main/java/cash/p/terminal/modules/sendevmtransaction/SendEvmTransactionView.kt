package cash.p.terminal.modules.sendevmtransaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.stats.StatEntity
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.modules.evmfee.Cautions
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.multiswap.ui.DataFieldFee
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.strings.helpers.shorten
import cash.p.terminal.ui.compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui_compose.components.NftIcon
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.TransactionInfoAddressCell
import cash.p.terminal.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.chartview.cell.SectionUniversalLawrence
import cash.p.terminal.ui.helpers.TextHelper
import cash.p.terminal.ui_compose.components.HFillSpacer
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.caption_grey
import cash.p.terminal.ui_compose.components.headline2_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.Blockchain
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.helpers.HudHelper

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
        }

        if (transactionFields.isNotEmpty()) {
            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                transactionFields.forEachIndexed { index, field ->
                    field.GetContent(navController, index != 0)
                }
            }
        }

        VSpacer(height = 16.dp)
        SectionUniversalLawrence {
            DataFieldFee(
                navController,
                networkFee?.primary?.getFormattedPlain() ?: "---",
                networkFee?.secondary?.getFormattedPlain() ?: "---"
            )
        }

        if (cautions.isNotEmpty()) {
            Cautions(cautions)
        }
    }
}

@Composable
private fun NonceView(nonceViewModel: SendEvmNonceViewModel) {
    val uiState = nonceViewModel.uiState
    if (!uiState.showInConfirmation) return
    val nonce = uiState.nonce ?: return

    Spacer(Modifier.height(16.dp))
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                subhead2_grey(
                    text = stringResource(id = R.string.Send_Confirmation_Nonce)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = nonce.toString(),
                    maxLines = 1,
                    style = ComposeAppTheme.typography.subhead1,
                    color = setColorByType(ValueType.Regular)
                )
            }
        }
    )
}

@Composable
private fun SectionView(viewItems: List<ViewItem>, navController: NavController, statPage: StatPage) {
    Spacer(Modifier.height(16.dp))
    CellUniversalLawrenceSection(viewItems) { item ->
        when (item) {
            is ViewItem.Subhead -> Subhead(item)
            is ViewItem.Value -> TitleValue(item)
            is ViewItem.ValueMulti -> TitleValueMulti(item)
            is ViewItem.AmountMulti -> AmountMulti(item)
            is ViewItem.Amount -> Amount(item)
            is ViewItem.AmountWithTitle -> AmountWithTitle(item)
            is ViewItem.NftAmount -> NftAmount(item)
            is ViewItem.Address -> {
                TransactionInfoAddressCell(
                    title = item.title,
                    value = item.value,
                    showAdd = item.showAdd,
                    blockchainType = item.blockchainType,
                    navController = navController,
                    onCopy = {
                        stat(page = statPage, section = item.statSection, event = StatEvent.Copy(StatEntity.Address))
                    },
                    onAddToExisting = {
                        stat(page = statPage, section = item.statSection, event = StatEvent.Open(StatPage.ContactAddToExisting))
                    },
                    onAddToNew = {
                        stat(page = statPage, section = item.statSection, event = StatEvent.Open(StatPage.ContactNew))
                    }
                )
            }
            is ViewItem.ContactItem -> TransactionInfoContactCell(item.contact.name)
            is ViewItem.Input -> TitleValueHex("CoinFragmentInput", item.value.shorten(), item.value)
            is ViewItem.TokenItem -> Token(item)
        }
    }
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
            style = ComposeAppTheme.typography.subhead1,
            color = setColorByType(item.type)
        )
    }
}

@Composable
private fun TitleValueMulti(item: ViewItem.ValueMulti) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        subhead2_grey(
            text = item.title
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.primaryValue,
                maxLines = 1,
                style = ComposeAppTheme.typography.subhead1,
                color = setColorByType(item.type)
            )
            Text(
                text = item.secondaryValue,
                maxLines = 1,
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey
            )
        }
    }
}

@Composable
private fun AmountMulti(item: ViewItem.AmountMulti) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CoinImage(
            token = item.token,
            modifier = Modifier.size(32.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.amounts[0].coinAmount,
                    maxLines = 1,
                    style = ComposeAppTheme.typography.subhead1,
                    color = setColorByType(item.type)
                )
                Spacer(Modifier.weight(1f))
                subhead2_grey(
                    text = item.amounts[0].fiatAmount ?: ""
                )
            }
            if (item.amounts.size > 1) {
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    caption_grey(
                        text = item.amounts[1].coinAmount
                    )
                    Spacer(Modifier.weight(1f))
                    caption_grey(
                        text = item.amounts[1].fiatAmount ?: ""
                    )
                }
            }
        }
    }
}

@Composable
private fun Amount(item: ViewItem.Amount) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CoinImage(
            token = item.token,
            modifier = Modifier.padding(end = 16.dp).size(32.dp)
        )
        Text(
            text = item.coinAmount,
            maxLines = 1,
            style = ComposeAppTheme.typography.subhead1,
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
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        CoinImage(
            token = item.token,
            modifier = Modifier.size(32.dp)
        )
        HSpacer(16.dp)
        Column {
            subhead2_leah(text = item.title)
            VSpacer(height = 1.dp)
            caption_grey(text = item.badge ?: stringResource(id =R.string.CoinPlatforms_Native))
        }
        HFillSpacer(minWidth = 8.dp)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.coinAmount,
                maxLines = 1,
                style = ComposeAppTheme.typography.subhead1,
                color = setColorByType(item.type)
            )
            item.fiatAmount?.let {
                VSpacer(height = 1.dp)
                subhead2_grey(text = it)
            }
        }
    }
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
            style = ComposeAppTheme.typography.subhead2,
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
        ValueType.Regular -> cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.bran
        ValueType.Disabled -> cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.grey
        ValueType.Outgoing -> cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.leah
        ValueType.Incoming -> cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.remus
        ValueType.Warning -> cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.jacob
        ValueType.Forbidden -> cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.lucian
    }

@Preview
@Composable
private fun Preview_Subhead() {
    val item = ViewItem.Subhead("Title", "Value", R.drawable.ic_arrow_down_left_24)
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        Subhead(item)
    }
}

@Preview
@Composable
private fun Preview_TitleValue() {
    val item = ViewItem.Value("Title", "Value", ValueType.Incoming)
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        TitleValue(item)
    }
}

@Preview
@Composable
private fun Preview_AmountMulti() {
    val token = cash.p.terminal.wallet.Token(
        coin = Coin("uid", "KuCoin", "KCS"),
        blockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null),
        type = TokenType.Eip20("eef"),
        decimals = 18
    )
    val item = ViewItem.AmountMulti(
        listOf(
            AmountValues("0.104 KCS (est)", "$0.99"),
            AmountValues("0.103 KCS (min)", "$0.95"),
        ),
        ValueType.Incoming,
        token
    )
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        AmountMulti(item)
    }
}

@Preview
@Composable
private fun Preview_Amount() {
    val token = cash.p.terminal.wallet.Token(
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
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        Amount(item)
    }
}

@Preview
@Composable
private fun Preview_TitleValueHex() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        TitleValueHex("Title", "ValueShort", "ValueLong")
    }
}
