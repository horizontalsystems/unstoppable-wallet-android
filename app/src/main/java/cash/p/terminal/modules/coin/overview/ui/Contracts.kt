package cash.p.terminal.modules.coin.overview.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.imageUrl
import cash.p.terminal.modules.coin.ContractInfo
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.CellSingleLineClear
import io.horizontalsystems.core.CellUniversalLawrenceSection
import io.horizontalsystems.core.RowUniversal
import io.horizontalsystems.core.imageUrl

@Preview
@Composable
fun ContractsPreview() {
    ComposeAppTheme(darkTheme = true) {
        val contracts = listOf(
            ContractInfo(
                "0xda123as34290098asd0098asdasd9098asd90123asd",
                BlockchainType.Ethereum.imageUrl,
                "https://etherscan.io/token/0xda123as34290098asd0098asdasd9098asd90123asd"
            ),
            ContractInfo(
                "0x34290098asd8asdasd98asd8asdasd9098asd098as9",
                BlockchainType.BinanceChain.imageUrl,
                "https://bscscan.com/token/0x34290098asd8asdasd98asd8asdasd9098asd098as9"
            ),
            ContractInfo(
                "BNB",
                BlockchainType.BinanceSmartChain.imageUrl,
                "https://explorer.binance.org/asset/BNB"
            ),
        )
        Contracts(contracts = contracts, {}, {})
    }
}
@Composable
fun Contracts(
    contracts: List<ContractInfo>,
    onClickCopy: (ContractInfo) -> Unit,
    onClickExplorer: (String) -> Unit,
) {
    Column {
        CellSingleLineClear(borderTop = true) {
            body_leah(text = stringResource(id = R.string.CoinPage_Contracts))
        }

        CellUniversalLawrenceSection(contracts) { contractInfo ->
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = rememberAsyncImagePainter(
                        model = contractInfo.imgUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_32)
                    ),
                    contentDescription = "platform"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    contractInfo.name?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            body_leah(
                                modifier = Modifier.weight(1f, fill = false),
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            contractInfo.schema?.let { labelText ->
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ComposeAppTheme.colors.jeremy)
                                ) {
                                    Text(
                                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                                        text = labelText,
                                        color = ComposeAppTheme.colors.bran,
                                        style = ComposeAppTheme.typography.microSB,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(1.dp))
                    }
                    subhead2_grey(
                        text = contractInfo.shortened,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_copy_20,
                    contentDescription = stringResource(R.string.Button_Copy),
                    onClick = {
                        onClickCopy.invoke(contractInfo)
                    }
                )
                contractInfo.explorerUrl?.let { explorerUrl ->
                    ButtonSecondaryCircle(
                        modifier = Modifier.padding(start = 16.dp),
                        icon = R.drawable.ic_globe_20,
                        contentDescription = stringResource(R.string.Button_Browser),
                        onClick = {
                            onClickExplorer.invoke(explorerUrl)
                        }
                    )
                }
            }
        }
    }
}