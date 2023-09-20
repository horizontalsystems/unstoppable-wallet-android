package io.horizontalsystems.bankwallet.modules.settings.privacy

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody


@Composable
fun PrivacyScreen(navController: NavController) {
    ComposeAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = stringResource(R.string.Settings_Privacy),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                InfoTextBody(
                    text = stringResource(R.string.Privacy_Information),
                )

                BulletedText(R.string.Privacy_BulletedText1)
                BulletedText(R.string.Privacy_BulletedText2)
                BulletedText(R.string.Privacy_BulletedText3)
                BulletedText(R.string.Privacy_BulletedText4)
                Spacer(modifier = Modifier.height(32.dp))
            }

            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.FooterText),
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun BulletedText(@StringRes text: Int) {
    Row(Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "\u2022 ",
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(text),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran,
            modifier = Modifier.padding(end = 32.dp)
        )
    }

}