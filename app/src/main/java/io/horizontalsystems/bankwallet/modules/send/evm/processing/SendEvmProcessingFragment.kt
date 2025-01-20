package io.horizontalsystems.bankwallet.modules.send.evm.processing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSCircularProgressIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.PremiumHeader
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoCancelCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoSpeedUpCell
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.BlockchainType

class SendEvmProcessingFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        SendEvmProcessingScreen(navController)
    }

}

@Composable
private fun SendEvmProcessingScreen(
    navController: NavController
) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )
        },
    ) { innerPaddings ->
        Column(
            modifier = Modifier.padding(innerPaddings)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                VSpacer(12.dp)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(
                            color = ComposeAppTheme.colors.raina,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(R.drawable.ic_checkmark_48),
                        contentDescription = "checkmark",
                        tint = ComposeAppTheme.colors.remus
                    )
                }
                VSpacer(32.dp)
                headline1_leah(
                    stringResource(R.string.Send_Processing),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    textAlign = TextAlign.Center
                )
                VSpacer(12.dp)
                subhead2_grey(
                    text = stringResource(R.string.Send_Processing_Description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    textAlign = TextAlign.Center
                )
                VSpacer(32.dp)
                TransactionDataView(
                    title = "Ethereum",
                    subtitle = "to 0x7a9f...77aa",
                    coinIconUrl = "eth.png",
                    coinIconPlaceholder = R.drawable.logo_ethereum_24,
                    coinAmount = "0.0001 ETH",
                    fiatAmount = "$0.0001",
                    progress = 0.5f,
                )

                if (true) {
                    VSpacer(height = 24.dp)
                    PremiumHeader()
                    SectionPremiumUniversalLawrence {
                        TransactionInfoSpeedUpCell(
                            transactionHash = "txhash",
                            blockchainType = BlockchainType.fromUid("ethereum"),
                            navController = navController
                        )
                        Divider(
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                        )
                        TransactionInfoCancelCell(
                            transactionHash = "txhash",
                            blockchainType = BlockchainType.fromUid("ethereum"),
                            navController = navController
                        )
                    }
                    InfoText(
                        text = stringResource(R.string.TransactionInfo_SpeedUpDescription),
                    )
                }
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Done),
                    onClick = {

                    },
                )
            }
        }
    }
}

@Composable
fun TransactionDataView(
    title: String,
    subtitle: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    coinAmount: String,
    fiatAmount: String? = null,
    progress: Float? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.tyler)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            progress?.let { progress ->
                HSCircularProgressIndicator(progress)
            }
            HsImage(
                url = coinIconUrl,
                placeholder = coinIconPlaceholder,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        }
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                body_leah(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                body_lucian(coinAmount)
            }
            VSpacer(3.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subhead2_grey(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                fiatAmount?.let {
                    subhead2_grey(it)
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewTransactionDataView() {
    ComposeAppTheme {
        TransactionDataView(
            title = "Ethereum",
            subtitle = "to 0x7a9f...77aa",
            coinIconUrl = "eth.png",
            coinAmount = "0.0001 ETH",
            fiatAmount = "$0.0001",
            coinIconPlaceholder = R.drawable.logo_ethereum_24,
        )
    }
}
