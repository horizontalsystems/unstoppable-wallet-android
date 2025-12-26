package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeText
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun WCSessionCell(
    shape: Shape,
    showDivider: Boolean = false,
    session: WalletConnectListModule.SessionViewItem,
    navController: NavController,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(ComposeAppTheme.colors.lawrence)
            .clickable {
                navController.slideFromBottom(
                    R.id.wcSessionBottomSheetDialog,
                    WCSessionModule.Input(session.sessionTopic)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (showDivider) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
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
                BadgeText(
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
