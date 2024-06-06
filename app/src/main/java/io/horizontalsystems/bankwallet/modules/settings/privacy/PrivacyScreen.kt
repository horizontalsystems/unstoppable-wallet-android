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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun PrivacyScreen(navController: NavController) {
    val viewModel = viewModel<PrivacyViewModel>(factory = PrivacyViewModel.Factory())

    val uiState = viewModel.uiState

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
            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                CellUniversal {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share_24px),
                        contentDescription = "Share",
                        tint = ComposeAppTheme.colors.grey
                    )
                    HSpacer(width = 16.dp)
                    body_leah(text = stringResource(R.string.ShareUiData))
                    HFillSpacer(minWidth = 8.dp)
                    HsSwitch(
                        checked = uiState.uiStatsEnabled,
                        onCheckedChange = {
                            viewModel.toggleUiStats(it)

                            stat(page = StatPage.Privacy, event = StatEvent.EnableUiStats(it))
                        }
                    )
                }
            }
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

@Composable
private fun BulletedText(@StringRes text: Int) {
    Row(
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, end = 32.dp, bottom = 12.dp)
    ) {
        Text(
            text = "\u2022 ",
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran,
            modifier = Modifier.width(15.dp),
            textAlign = TextAlign.Center
        )
        HSpacer(width = 8.dp)
        Text(
            text = stringResource(text),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran,
            modifier = Modifier.padding(end = 32.dp)
        )
    }

}