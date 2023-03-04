package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah

@Composable
fun BiometricDisabledDialog(onClick: () -> Unit) {
    Dialog(onDismissRequest = onClick) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            Spacer(Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                title3_leah(text = stringResource(R.string.Unlock_BiometricScanner))
                Spacer(Modifier.height(12.dp))
                body_grey(text = stringResource(R.string.Unlock_BiometricScannerDisabled_Description))
                Spacer(Modifier.height(44.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_touch_id_24),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.lucian
                    )
                    Spacer(Modifier.width(24.dp))
                    body_lucian(text = stringResource(R.string.Unlock_BiometricScannerDisabled_Info))
                }
            }
            Spacer(Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 8.dp, bottom = 8.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .clickable { onClick.invoke() },
                contentAlignment = Alignment.Center
            ) {
                headline2_jacob(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.Unlock_Passcode)
                )
            }

        }
    }
}

@Preview
@Composable
fun Preview_BiometricDisabledDialog() {
    ComposeAppTheme {
        BiometricDisabledDialog({})
    }
}
