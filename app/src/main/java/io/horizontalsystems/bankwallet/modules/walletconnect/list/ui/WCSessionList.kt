package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WalletConnectSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeCount
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection

@Composable
fun WCSessionList(
    section: WalletConnectListModule.Section,
    navController: NavController
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            text = stringResource(section.version.value),
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.grey
        )
        if (section.pendingRequests != null) {
            CellSingleLineLawrenceSection(listOf(section.pendingRequests)) {
                PendingRequestsCell(it, navController)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        CellMultilineLawrenceSection(section.sessions) {
            SessionCell(it, section.version, navController)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PendingRequestsCell(pendingRequests: Int, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = pendingRequests > 0,
                onClick = {
//                    navController.slideFromRight(R.id.wcPendingRequestsFragment)
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.WalletConnect_PendingRequests),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah
        )
        if (pendingRequests > 0) {
            BadgeCount(
                text = pendingRequests.toString(),
            )
        }
        Image(
            modifier = Modifier.padding(start = 8.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null
        )
    }
}

@Composable
private fun SessionCell(
    session: WalletConnectListModule.SessionViewItem,
    version: WalletConnectListModule.Version,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (version == WalletConnectListModule.Version.Version2){
                    navController.slideFromBottom(
                        R.id.wc2SessionFragment,
                        WC2SessionModule.prepareParams(
                            session.sessionId,
                            null,
                        )
                    )
                } else {
                    navController.slideFromBottom(
                        R.id.walletConnectMainFragment,
                        WalletConnectSessionModule.prepareParams(
                            session.sessionId,
                            null,
                        )
                    )
                }
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp)),
            painter = rememberImagePainter(
                data = session.imageUrl,
                builder = {
                    error(R.drawable.coin_placeholder)
                }
            ),
            contentDescription = null,
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = session.subtitle,
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey
            )
        }
        Image(
            modifier = Modifier.padding(start = 5.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null
        )
    }
}