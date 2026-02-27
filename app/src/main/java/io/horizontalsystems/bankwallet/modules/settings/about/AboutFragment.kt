package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesScreen
import io.horizontalsystems.bankwallet.modules.settings.appstatus.AppStatusScreen
import io.horizontalsystems.bankwallet.modules.settings.main.HsSettingCell
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsScreen
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data object AboutScreen : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        AboutScreen(backStack)
    }
}

@Composable
private fun AboutScreen(backStack: NavBackStack<HSScreen>) {
    val aboutViewModel = viewModel<AboutViewModel>(factory = AboutModule.Factory())

    HSScaffold(
        title = stringResource(R.string.SettingsAboutApp_Title),
        onBack = backStack::removeLastOrNull,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            VSpacer(12.dp)
            SettingSections(aboutViewModel, backStack)
            VSpacer(36.dp)
        }
    }
}

@Composable
private fun SettingSections(
    viewModel: AboutViewModel,
    backStack: NavBackStack<HSScreen>
) {

    val context = LocalContext.current

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                title = R.string.SettingsAboutApp_AppVersion,
                icon = R.drawable.ic_info_20,
                value = viewModel.appVersion,
                onClick = {
                    backStack.add(ReleaseNotesScreen(false))

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.WhatsNew))
                }
            )
        }
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.Settings_AppStatus,
                R.drawable.ic_app_status,
                onClick = {
                    backStack.add(AppStatusScreen)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.AppStatus))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Terms,
                R.drawable.ic_terms_20,
                showAlert = viewModel.termsShowAlert,
                onClick = {
                    backStack.add(TermsScreen())

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.Terms))
                }
            )
        })
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.SettingsAboutApp_Github,
                R.drawable.ic_github_20,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, viewModel.githubLink)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.ExternalGithub))
                }
            )
        }, {
            HsSettingCell(
                R.string.SettingsAboutApp_Site,
                R.drawable.ic_globe,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, viewModel.appWebPageLink)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.ExternalWebsite))
                }
            )
        })
    )

    VSpacer(32.dp)
}
