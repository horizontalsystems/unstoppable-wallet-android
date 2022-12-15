package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeCount
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun WCSessionCell(
    shape: Shape,
    showDivider: Boolean = false,
    session: WalletConnectListModule.SessionViewItem,
    version: WalletConnectListModule.Version,
    navController: NavController,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(ComposeAppTheme.colors.lawrence)
            .clickable {
                if (version == WalletConnectListModule.Version.Version2) {
                    navController.slideFromBottom(
                        R.id.wc2SessionFragment,
                        WC2SessionModule.prepareParams(
                            session.sessionId
                        )
                    )
                } else {
                    navController.slideFromBottom(
                        R.id.wcSessionFragment,
                        WCSessionModule.prepareParams(
                            session.sessionId,
                            null,
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showDivider) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
                painter = rememberAsyncImagePainter(
                    model = session.imageUrl,
                    error = painterResource(R.drawable.ic_platform_placeholder_24)
                ),
                contentDescription = null,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val title = when {
                    session.title.isNotBlank() -> session.title
                    else -> stringResource(id = R.string.WalletConnect_Unnamed)
                }

                body_leah(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subhead2_grey(text = session.subtitle)
            }
            if (session.pendingRequestsCount > 0) {
                BadgeCount(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = session.pendingRequestsCount.toString()
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null
            )
        }
    }
}
