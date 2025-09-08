package io.horizontalsystems.bankwallet.modules.moneronetwork

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager.MoneroNode
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader


@Composable
fun MoneroNodeTrustBottomSheet(
    node: MoneroNode,
    onDone: (Boolean) -> Unit,
    onCloseClick: () -> Unit,
) {
    var checked by remember(node.host) { mutableStateOf(node.trusted) }

    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_settings_jacob),
            title = stringResource(R.string.MoneroNodeSettings_TrustedTitle),
            onCloseClick = onCloseClick
        ) {

            InfoTextBody(
                text = stringResource(R.string.MoneroNodeSettings_TrustedDescription)
            )
            VSpacer(12.dp)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            ) {
                SectionUniversalItem {
                    RowUniversal(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalPadding = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            headline2_leah(text = stringResource(R.string.MoneroNodeSettings_Trusted))
                            subhead2_grey(text = node.host)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        HsSwitch(
                            modifier = Modifier.padding(start = 5.dp),
                            checked = checked,
                            onCheckedChange = { checked = it }
                        )
                    }
                }
            }
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                title = stringResource(R.string.Button_Done),
                onClick = {
                    onDone(checked)
                }
            )
        }
    }
}
