package io.horizontalsystems.bankwallet.modules.walletconnect.session.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.modules.walletconnect.session.Status
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCWhiteListState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.subscriptions.core.ScamProtection

@Composable
fun BlockchainCell(
    title: String,
    value: String?,
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = title)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        value?.let {
            subhead1_leah(text = it)
        }
    }
}

@Composable
fun ScamProtectionCell(
    activated: Boolean,
    whiteListState: WCWhiteListState,
    navController: NavController,
) {
    CellPrimary(
        middle = {
            CellMiddleInfo(
                title = "Scam Protection".hs,
            )
        },
        right = {
            if (!activated) {
                CellRightInfoTextIcon(
                    text = "Deactivated".hs(color = ComposeAppTheme.colors.jacob),
                    icon = painterResource(R.drawable.lock_24),
                    iconTint = ComposeAppTheme.colors.jacob
                )
            } else  {
                when(whiteListState) {
                    WCWhiteListState.InWhiteList -> {
                        CellRightInfoTextIcon(
                            text = "Secure".hs(color = ComposeAppTheme.colors.remus),
                            icon = painterResource(R.drawable.shield_check_filled_24),
                            iconTint = ComposeAppTheme.colors.remus
                        )
                    }
                    WCWhiteListState.NotInWhiteList -> {
                        CellRightInfoTextIcon(
                            text = "Risky".hs(color = ComposeAppTheme.colors.lucian),
                            icon = painterResource(R.drawable.ic_warning_filled_24),
                            iconTint = ComposeAppTheme.colors.lucian
                        )
                    }
                    //todo update with spinner
                    WCWhiteListState.InProgress -> {
                        CellRightInfo(
                            title = "Checking...".hs,
                        )
                    }
                    WCWhiteListState.Error -> {
                        CellRightInfoTextIcon(
                            text = stringResource(R.string.NotAvailable).hs(color = ComposeAppTheme.colors.leah),
                        )
                    }
                }
            }
        },
        onClick = {
            navController.paidAction(ScamProtection) {
                //show info bottom sheet
            }
        }
    )
}

@Composable
fun WalletName(
    name: String,
) {
    CellPrimary(
        middle = {
            CellMiddleInfo(
                title = stringResource(R.string.WalletConnect_ActiveWallet).hs,
            )
        },
        right = {
            CellRightInfo(
                title = name.hs,
            )
        },
    )
}

@Composable
fun NetworksCell(
    blockchainTypes: List<BlockchainType>?,
    onClick: () -> Unit
) {
    val count = blockchainTypes?.size ?: 0
    CellPrimary(
        middle = {
            CellMiddleInfo(
                title = stringResource(R.string.WalletConnect_Networks).hs,
            )
        },
        right = {
            if (count == 1){
                CellRightInfo(
                    title = blockchainTypes?.firstOrNull()?.title?.hs ?: "".hs,
                )
            } else {
                CellRightInfoTextIcon(
                    text = count.toString().hs,
                    icon = painterResource(R.drawable.arrow_s_down_24),
                    iconTint = ComposeAppTheme.colors.leah
                )
            }
        },
        onClick = if (count > 1) onClick else null
    )
}

@Composable
fun TitleValueCell(title: String, value: String) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        subhead2_grey(text = title)
        Spacer(Modifier.weight(1f))
        subhead1_leah(text = value)
    }
}

@Composable
fun StatusCell(connectionStatus: Status?) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        subhead2_grey(text = stringResource(id = R.string.WalletConnect_Status))
        Spacer(Modifier.weight(1f))
        connectionStatus?.let { status ->
            val color = when (status) {
                Status.OFFLINE -> ComposeAppTheme.colors.lucian
                Status.CONNECTING -> ComposeAppTheme.colors.leah
                Status.ONLINE -> ComposeAppTheme.colors.remus
            }
            Text(
                text = stringResource(status.value),
                color = color,
                style = ComposeAppTheme.typography.subhead
            )
        }
    }
}

@Composable
fun DropDownCell(
    title: String,
    value: String,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable(enabled = enabled, onClick = onSelect),
        onClick = if (enabled) onSelect else null
    ) {
        subhead2_grey(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            subhead1_leah(
                text = value,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            if (enabled) {
                HSpacer(8.dp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
