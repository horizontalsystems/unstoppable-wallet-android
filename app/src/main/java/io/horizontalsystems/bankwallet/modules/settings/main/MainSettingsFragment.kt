package io.horizontalsystems.bankwallet.modules.settings.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.languageswitcher.LanguageSettingsFragment

class MainSettingsFragment : BaseFragment() {

    private val viewModel by viewModels<MainSettingsViewModel> { MainSettingsModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    SettingsScreen(
                        viewModel,
                        { setting -> openScreen(setting) },
                        { openLink(viewModel.companyWebPage) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeFragmentResult()
    }

    private fun openScreen(setting: MainSettingsModule.Setting) {
        findNavController().navigate(setting.destination, setting.navigationBundle, navOptions())
    }

    private fun openLink(link: String) {
        context?.let { LinkHelper.openLinkInAppBrowser(it, link) }
    }

    private fun subscribeFragmentResult() {
        getNavigationResult(LanguageSettingsFragment.LANGUAGE_CHANGE)?.let {
            viewModel.setAppRelaunchingFromSettings()
            activity?.let { MainModule.startAsNewTask(it) }
        }
    }

}

@Composable
private fun SettingsScreen(
    viewModel: MainSettingsViewModel,
    onSettingClick: (MainSettingsModule.Setting) -> Unit,
    onCompanyLogoClick: () -> Unit
) {

    val settingItems by viewModel.settingItemsLiveData.observeAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Settings_Title),
            )

            settingItems?.let {
                SettingSection(
                    it,
                    viewModel.appVersion,
                    onSettingClick,
                    onCompanyLogoClick
                )
            }
        }
    }
}

@Composable
private fun SettingSection(
    sections: List<List<MainSettingsModule.SettingViewItem>>,
    appVersion: String,
    onSettingClick: (MainSettingsModule.Setting) -> Unit,
    onCompanyLogoClick: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(12.dp))
        sections.forEach { section ->
            CellSingleLineLawrenceSection(section) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = { onSettingClick.invoke(item.setting) }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(id = item.setting.icon),
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(item.setting.title),
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.leah,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    item.value?.let {
                        Text(
                            text = it,
                            style = ComposeAppTheme.typography.subhead1,
                            color = ComposeAppTheme.colors.leah,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    if (item.showAlert) {
                        Image(
                            modifier = Modifier.padding(start = 8.dp, end = 12.dp).size(20.dp),
                            painter = painterResource(id = R.drawable.ic_attention_red_20),
                            contentDescription = null,
                        )
                    }
                    Image(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = null,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
        SettingsFooter(appVersion, onCompanyLogoClick)
    }
}

@Composable
private fun SettingsFooter(appVersion: String, onCompanyLogoClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.Settings_InfoTitleWithVersion, appVersion).uppercase(),
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.grey,
        )
        Divider(
            modifier = Modifier.width(100.dp).padding(top = 8.dp, bottom = 4.5.dp),
            thickness = 0.5.dp,
            color = ComposeAppTheme.colors.steel20
        )
        Text(
            text = stringResource(R.string.Settings_InfoSubtitle),
            style = ComposeAppTheme.typography.micro,
            color = ComposeAppTheme.colors.grey,
        )
        Image(
            modifier = Modifier
                .padding(top = 32.dp)
                .size(32.dp)
                .clickable {
                    onCompanyLogoClick.invoke()
                },
            painter = painterResource(id = R.drawable.ic_company_logo),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
            text = stringResource(R.string.Settings_CompanyName),
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.grey,
        )
    }
}

@Preview
@Composable
private fun previewSettingsScreen() {
    val section1 = listOf(
        MainSettingsModule.SettingViewItem(MainSettingsModule.Setting.ManageWallets, "Light"),
        MainSettingsModule.SettingViewItem(MainSettingsModule.Setting.SecurityCenter),
        MainSettingsModule.SettingViewItem(MainSettingsModule.Setting.WalletConnect),
    )
    val section2 = listOf(
        MainSettingsModule.SettingViewItem(MainSettingsModule.Setting.LaunchScreen, "Value"),
        MainSettingsModule.SettingViewItem(
            MainSettingsModule.Setting.BaseCurrency,
            showAlert = true
        ),
    )

    val testItems = listOf(section1, section2)

    ComposeAppTheme {
        SettingSection(testItems, "0.24", { }, { })
    }
}
