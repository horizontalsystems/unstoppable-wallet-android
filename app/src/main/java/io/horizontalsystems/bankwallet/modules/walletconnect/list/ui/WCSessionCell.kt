package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun WCSessionCell(
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    session: WalletConnectListModule.SessionViewItem,
    version: WalletConnectListModule.Version,
    navController: NavController
) {
    Column {
        if (showDivider) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Row(
            modifier = modifier
                .height(60.dp)
                .fillMaxWidth()
                .clickable {
                    if (version == WalletConnectListModule.Version.Version2) {
                        navController.slideFromBottom(
                            R.id.wc2SessionFragment,
                            WC2SessionModule.prepareParams(
                                session.sessionId,
                                null,
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
}
