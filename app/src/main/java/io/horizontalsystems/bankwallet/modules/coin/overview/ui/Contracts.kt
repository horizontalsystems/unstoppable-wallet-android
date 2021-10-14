package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection

@Preview
@Composable
fun ContractsPreview() {
    ComposeAppTheme(darkTheme = true) {
        val contracts = listOf(
            ContractInfo.Erc20("0xda123as34290098asd0098asdasd9098asd90123asd"),
            ContractInfo.Bep20("0x34290098asd8asdasd98asd8asdasd9098asd098as9"),
            ContractInfo.Bep2("BNB"),
        )
        Contracts(contracts = contracts)
    }
}
@Composable
fun Contracts(contracts: List<ContractInfo>) {
    Column {
        CellSingleLineClear(borderTop = true) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.CoinPage_Contracts),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.oz,
                )
            }
        }

        CellSingleLineLawrenceSection(contracts) { contractInfo ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(painter = painterResource(id = contractInfo.logoResId), contentDescription = "platform")
                Text(
                    text = contractInfo.uid,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    style = ComposeAppTheme.typography.subhead2,
                    color = ComposeAppTheme.colors.grey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_copy_20,
                    onClick = { }
                )
                ButtonSecondaryCircle(
                    modifier = Modifier.padding(start = 8.dp),
                    icon = R.drawable.ic_add_to_wallet2_20,
                    onClick = { }
                )
            }
        }
    }
}