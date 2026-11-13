package io.horizontalsystems.walletkit.modules.settings.about

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.releasenotes.ReleaseNotesScreen
import io.horizontalsystems.walletkit.modules.settings.appstatus.AppStatusScreen
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_grey
import io.horizontalsystems.walletkit.ui.helpers.LinkHelper
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data object AboutPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AboutScreen(
            navigation,
            { navigation.removeLastOrNull() }
        )
    }
}

@Serializable
data object ReleaseNotesPage: HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        ReleaseNotesScreen(false, { navigation.removeLastOrNull() })
    }
}

@Serializable
data object AppStatusPage: HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AppStatusScreen(navigation)
    }
}

@Composable
private fun AboutScreen(
    navigation: HSNavigation,
    onBackPress: () -> Unit,
    aboutViewModel: AboutViewModel = viewModel(factory = AboutModule.Factory()),
) {
    HSScaffold(
        title = stringResource(R.string.SettingsAboutApp_Title),
        onBack = onBackPress,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            VSpacer(12.dp)
            SettingSections(aboutViewModel, navigation)
            VSpacer(36.dp)
        }
    }
}

@Composable
private fun SettingSections(
    viewModel: AboutViewModel,
    navigation: HSNavigation
) {

    val context = LocalContext.current

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                title = R.string.SettingsAboutApp_AppVersion,
                icon = R.drawable.ic_info_20,
                value = viewModel.appVersion,
                onClick = {
                    navigation.add(ReleaseNotesPage)

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
                testTag = TestTags.ABOUT_APP_STATUS,
                onClick = {
                    navigation.add(AppStatusPage)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.AppStatus))
                }
            )
        }, {
            // The screen this row opened is gone and its replacement is not built yet
            HsSettingCell(
                R.string.Settings_Terms,
                R.drawable.ic_terms_20,
                testTag = TestTags.ABOUT_TERMS,
                onClick = {}
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

@Composable
private fun HsSettingCell(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    value: String? = null,
    testTag: String? = null,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
        body_leah(
            text = stringResource(title),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))

        if (value != null) {
            subhead1_grey(
                text = value,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}
