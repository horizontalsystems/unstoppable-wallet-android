package io.horizontalsystems.walletkit.modules.send.bitcoin.utxoexpert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsCheckbox
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_lucian
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Token

@Composable
fun UtxoExpertModeScreen(
    adapter: ISendBitcoinAdapter,
    token: Token,
    customUnspentOutputs: List<UnspentOutputInfo>?,
    updateUnspentOutputs: (List<UnspentOutputInfo>) -> Unit,
    onBackClick: () -> Unit
) {

    val viewModel: UtxoExpertModeViewModel = viewModel(
        factory = UtxoExpertModeFactory(
            adapter,
            token,
            customUnspentOutputs
        )
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Send_Utxos),
        onBack = onBackClick,
        bottomBar = {
            Box(
                modifier = Modifier
                    .height(62.dp)
                    .systemBarsPadding()
                    .fillMaxWidth()
            ) {
                HsDivider(modifier = Modifier.fillMaxWidth())
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
        }
    }
}



