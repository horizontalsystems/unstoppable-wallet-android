package cash.p.terminal.modules.send.bitcoin.utxoexpert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.strings.helpers.shorten
import cash.p.terminal.ui.compose.components.ButtonSecondaryTransparent
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsCheckbox
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.SectionItemBorderedRowUniversalClear
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.components.subhead2_lucian
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo

@Composable
fun UtxoExpertModeScreen(
    adapter: ISendBitcoinAdapter,
    token: Token,
    customUnspentOutputs: List<UnspentOutputInfo>?,
    updateUnspentOutputs: (List<UnspentOutputInfo>) -> Unit,
    onBackClick: () -> Unit
) {

    val viewModel: UtxoExpertModeViewModel = viewModel(
        factory = UtxoExpertModeModule.Factory(
            adapter,
            token,
            customUnspentOutputs
        )
    )
    val uiState = viewModel.uiState

    ComposeAppTheme {
        Scaffold(
            containerColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.Send_Utxos),
                    navigationIcon = {
                        HsBackButton(onClick = onBackClick)
                    },
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                CellUniversalLawrenceSection {
                    UtxoInfoCell(
                        title = stringResource(R.string.Send_Utxo_AvailableBalance),
                        value = uiState.availableBalanceInfo.value,
                        subValue = uiState.availableBalanceInfo.subValue
                    )
                }
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    UtxoList(
                        utxos = uiState.utxoItems,
                        onItemClicked = {
                            viewModel.onUnspentOutputClicked(it)
                            updateUnspentOutputs(viewModel.customOutputs)
                        }
                    )
                }
                Box(
                    modifier = Modifier
                        .height(62.dp)
                        .fillMaxWidth()
                ) {
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10
                    )
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ButtonSecondaryTransparent(
                            title = stringResource(id = R.string.Send_Utxo_UnselectAll),
                            enabled = uiState.unselectAllIsEnabled,
                            onClick = {
                                viewModel.unselectAll()
                                updateUnspentOutputs(viewModel.customOutputs)
                            }
                        )
                        ButtonSecondaryTransparent(
                            title = stringResource(id = R.string.Send_Utxo_SelectAll),
                            onClick = {
                                viewModel.selectAll()
                                updateUnspentOutputs(viewModel.customOutputs)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UtxoList(
    utxos: List<UtxoExpertModeModule.UnspentOutputViewItem>,
    onItemClicked: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        item {
            VSpacer(16.dp)
        }
        itemsIndexed(utxos) { index, item ->
            UtxoCell(
                id = item.id,
                selected = item.selected,
                title = item.date,
                subtitle = item.address.shorten(),
                value = item.amountToken,
                subValue = item.amountFiat,
                onItemClicked = onItemClicked,
                showTopBorder = index != 0,
                topRoundedCorners = index == 0,
                bottomRoundedCorners = index == utxos.size - 1
            )
        }
        item {
            VSpacer(16.dp)
        }
    }
}

@Composable
private fun UtxoInfoCell(
    title: String,
    value: String?,
    subValue: String?
) {
    RowUniversal(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            subhead2_leah(text = title)
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            if (value == null) {
                subhead2_lucian(text = "N/A")
            } else {
                subhead2_leah(text = value)
                subhead2_grey(text = subValue ?: "---")
            }
        }
    }
}

@Composable
private fun UtxoCell(
    id: String,
    selected: Boolean,
    showTopBorder: Boolean,
    title: String,
    subtitle: String,
    value: String,
    subValue: String?,
    onItemClicked: (String) -> Unit,
    topRoundedCorners: Boolean,
    bottomRoundedCorners: Boolean
) {
    val shape = when {
        topRoundedCorners && bottomRoundedCorners -> RoundedCornerShape(12.dp)
        topRoundedCorners -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        bottomRoundedCorners -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> RoundedCornerShape(0.dp)
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        SectionItemBorderedRowUniversalClear(
            onClick = {
                onItemClicked.invoke(id)
            },
            borderTop = showTopBorder
        ) {
            HsCheckbox(
                checked = selected,
                onCheckedChange = { onItemClicked.invoke(id) }
            )
            HSpacer(16.dp)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                subhead2_leah(text = title)
                subhead2_grey(text = subtitle)
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                subhead2_leah(text = value)
                subValue?.let {
                    subhead2_grey(text = it)
                }
            }
        }
    }
}