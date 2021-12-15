package io.horizontalsystems.bankwallet.modules.settings.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
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

    private fun openScreen(destination: Int) {
        val bundle = if (destination == R.id.mainFragment_to_manageKeysFragment) {
            bundleOf(ManageAccountsModule.MODE to ManageAccountsModule.Mode.Manage)
        } else {
            null
        }

        findNavController().navigate(destination, bundle, navOptions())
    }

    private fun openLink(link: String) {
        context?.let { LinkHelper.openLinkInAppBrowser(it, link) }
    }

    private fun subscribeFragmentResult() {
        getNavigationResult(LanguageSettingsFragment.LANGUAGE_CHANGE)?.let {
            viewModel.onLanguageChange()
            activity?.let { MainModule.startAsNewTask(it) }
        }
    }

}

class AppSettingItem(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val value: String? = null,
    val showAlert: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun SettingsScreen(
    viewModel: MainSettingsViewModel,
    onSettingClick: (Int) -> Unit,
    onCompanyLogoClick: () -> Unit
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Settings_Title),
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingSections(viewModel, onSettingClick)
                SettingsFooter(viewModel.appVersion, onCompanyLogoClick)
            }
        }
    }
}

@Composable
fun SettingSections(
    viewModel: MainSettingsViewModel,
    onSettingClick: (Int) -> Unit
) {

    val showAlertManageWallet by viewModel.manageWalletShowAlertLiveData.observeAsState(false)
    val showAlertSecurityCenter by viewModel.securityCenterShowAlertLiveData.observeAsState(false)
    val showAlertAboutApp by viewModel.aboutAppShowAlertLiveData.observeAsState(false)
    val walletConnectSessionCount by viewModel.walletConnectSessionCountLiveData.observeAsState(0)
    val launchScreen by viewModel.launchScreenLiveData.observeAsState()
    val baseCurrency by viewModel.baseCurrencyLiveData.observeAsState()
    val language by viewModel.languageLiveData.observeAsState()
    val theme by viewModel.themeLiveData.observeAsState()

    val section1 = listOf(
        AppSettingItem(
            R.string.SettingsSecurity_ManageKeys,
            R.drawable.ic_wallet_20,
            showAlert = showAlertManageWallet,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_manageKeysFragment) }
        ),
        AppSettingItem(
            R.string.Settings_SecurityCenter,
            R.drawable.ic_security,
            showAlert = showAlertSecurityCenter,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_securitySettingsFragment) }
        )
    )

    val section2 = listOf(
        AppSettingItem(
            R.string.Settings_WalletConnect,
            R.drawable.ic_wallet_connect_20,
            value = if (walletConnectSessionCount > 0) walletConnectSessionCount.toString() else null,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_walletConnect) }
        )
    )

    val section3 = listOf(
        AppSettingItem(
            R.string.Settings_LaunchScreen,
            R.drawable.ic_screen_20,
            value = launchScreen?.titleRes?.let { Translator.getString(it) },
            onClick = { onSettingClick.invoke(R.id.launchScreenSettingsFragment) }
        ),
        AppSettingItem(
            R.string.Settings_BaseCurrency,
            R.drawable.ic_currency,
            value = baseCurrency?.code,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_baseCurrencySettingsFragment) }
        ),
        AppSettingItem(
            R.string.Settings_Language,
            R.drawable.ic_language,
            value = language,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_languageSettingsFragment) }
        ),
        AppSettingItem(
            R.string.Settings_Theme,
            R.drawable.ic_light_mode,
            value = theme?.let { Translator.getString(it) },
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_themeSwitchFragment) }
        ),
        AppSettingItem(
            R.string.Settings_ExperimentalFeatures,
            R.drawable.ic_experimental,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_experimentalFeaturesFragment) }
        )
    )

    val section4 = listOf(
        AppSettingItem(
            R.string.Settings_Faq,
            R.drawable.ic_faq_20,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_faqListFragment) }
        ),
        AppSettingItem(
            R.string.Guides_Title,
            R.drawable.ic_academy_20,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_academyFragment) }
        )
    )

    val section5 = listOf(
        AppSettingItem(
            R.string.SettingsAboutApp_Title,
            R.drawable.ic_about_app_20,
            showAlert = showAlertAboutApp,
            onClick = { onSettingClick.invoke(R.id.mainFragment_to_aboutAppFragment) }
        )
    )

    val sections = listOf(section1, section2, section3, section4, section5)

    SettingSections(sections)
}

@Composable
private fun SettingSections(sections: List<List<AppSettingItem>>) {
    sections.forEach { section ->
        CellSingleLineLawrenceSection(section) { item ->
            SettingCell(
                item.title,
                item.icon,
                item.value,
                item.showAlert,
                item.onClick
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SettingCell(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    value: String? = null,
    showAlert: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clickable(onClick = { onClick.invoke() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
        )
        Text(
            text = stringResource(title),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        value?.let {
            Text(
                text = it,
                style = ComposeAppTheme.typography.subhead1,
                color = ComposeAppTheme.colors.leah,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        if (showAlert) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(id = R.drawable.ic_attention_red_20),
                contentDescription = null,
            )
            Spacer(Modifier.width(12.dp))
        }
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
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
    val section = listOf(
        AppSettingItem(
            R.string.Settings_Faq,
            R.drawable.ic_faq_20,
            onClick = { }
        ),
        AppSettingItem(
            R.string.Guides_Title,
            R.drawable.ic_academy_20,
            onClick = { }
        )
    )

    ComposeAppTheme {
        SettingSections(listOf(section))
    }
}
