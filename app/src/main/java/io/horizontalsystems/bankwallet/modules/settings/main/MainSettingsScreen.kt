package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.contacts.ContactsFragment
import io.horizontalsystems.bankwallet.modules.contacts.Mode
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.CounterType
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeCount
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainSettingsViewModel = viewModel(factory = MainSettingsModule.Factory()),
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Settings_Title),
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingSections(viewModel, navController)
                SettingsFooter(viewModel.appVersion, viewModel.companyWebPage)
            }
        }
    }
}

@Composable
private fun SettingSections(
    viewModel: MainSettingsViewModel,
    navController: NavController
) {

    val showAlertManageWallet by viewModel.manageWalletShowAlertLiveData.observeAsState(false)
    val showAlertSecurityCenter by viewModel.securityCenterShowAlertLiveData.observeAsState(false)
    val showAlertAboutApp by viewModel.aboutAppShowAlertLiveData.observeAsState(false)
    val wcCounter by viewModel.wcCounterLiveData.observeAsState()
    val baseCurrency by viewModel.baseCurrencyLiveData.observeAsState()
    val language by viewModel.languageLiveData.observeAsState()

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.Settings_Donate,
                R.drawable.ic_heart_jacob_48,
                onClick = {
                    navController.slideFromRight(R.id.donateFragment)
                }
            )
        }
    )

    VSpacer(32.dp)

    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.SettingsSecurity_ManageKeys,
                R.drawable.ic_wallet_20,
                showAlert = showAlertManageWallet,
                onClick = {
                    navController.slideFromRight(
                        R.id.manageAccountsFragment,
                        bundleOf(ManageAccountsModule.MODE to ManageAccountsModule.Mode.Manage)
                    )
                }
            )
        }, {
            HsSettingCell(
                R.string.BlockchainSettings_Title,
                R.drawable.ic_blocks_20,
                onClick = {
                    navController.slideFromRight(R.id.blockchainSettingsFragment)
                }
            )
        })
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.Settings_WalletConnect,
                R.drawable.ic_wallet_connect_20,
                value = (wcCounter as? CounterType.SessionCounter)?.number?.toString(),
                counterBadge = (wcCounter as? CounterType.PendingRequestCounter)?.number?.toString(),
                onClick = {
                    when (val state = viewModel.getWalletConnectSupportState()) {
                        WC1Manager.SupportState.Supported -> {
                            navController.slideFromRight(R.id.wallet_connect_graph)
                        }
                        WC1Manager.SupportState.NotSupportedDueToNoActiveAccount -> {
                            navController.slideFromBottom(R.id.wcErrorNoAccountFragment)
                        }
                        is WC1Manager.SupportState.NotSupportedDueToNonBackedUpAccount -> {
                            val text = Translator.getString(R.string.WalletConnect_Error_NeedBackup, state.account.name)
                            navController.slideFromBottom(
                                R.id.backupRequiredDialog,
                                BackupRequiredDialog.prepareParams(state.account, text)
                            )
                        }
                        is WC1Manager.SupportState.NotSupported -> {
                            navController.slideFromBottom(
                                R.id.wcAccountTypeNotSupportedDialog,
                                WCAccountTypeNotSupportedDialog.prepareParams(state.accountTypeDescription)
                            )
                        }
                    }
                }
            )
        }
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
        listOf(
            {
                HsSettingCell(
                    R.string.Settings_SecurityCenter,
                    R.drawable.ic_security,
                    showAlert = showAlertSecurityCenter,
                    onClick = {
                        navController.slideFromRight(R.id.securitySettingsFragment)
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Contacts,
                    R.drawable.ic_user_20,
                    onClick = {
                        navController.slideFromRight(R.id.contactsFragment, ContactsFragment.prepareParams(Mode.Full))
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Settings_Appearance,
                    R.drawable.ic_brush_20,
                    onClick = {
                        navController.slideFromRight(R.id.appearanceFragment)
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Settings_BaseCurrency,
                    R.drawable.ic_currency,
                    value = baseCurrency?.code,
                    onClick = {
                        navController.slideFromRight(R.id.baseCurrencySettingsFragment)
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Settings_Language,
                    R.drawable.ic_language,
                    value = language,
                    onClick = {
                        navController.slideFromRight(R.id.languageSettingsFragment)
                    }
                )
            },
        )
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.Settings_ExperimentalFeatures,
                R.drawable.ic_experimental,
                onClick = {
                    navController.slideFromRight(R.id.experimentalFeaturesFragment)
                }
            )
        }
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.Settings_Faq,
                R.drawable.ic_faq_20,
                onClick = {
                    navController.slideFromRight(R.id.faqListFragment)
                }
            )
        }, {
            HsSettingCell(
                R.string.Guides_Title,
                R.drawable.ic_academy_20,
                onClick = {
                    navController.slideFromRight(R.id.academyFragment)
                }
            )
        })
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.SettingsAboutApp_Title,
                R.drawable.ic_about_app_20,
                showAlert = showAlertAboutApp,
                onClick = {
                    navController.slideFromRight(R.id.aboutAppFragment)
                }
            )
        }
    )

    Spacer(Modifier.height(32.dp))
}

@Composable
fun HsSettingCell(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    value: String? = null,
    counterBadge: String? = null,
    showAlert: Boolean = false,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        Image(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
        )
        body_leah(
            text = stringResource(title),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))

        if (counterBadge != null) {
            BadgeCount(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = counterBadge
            )
        } else if (value != null) {
            subhead1_grey(
                text = value,
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
private fun SettingsFooter(appVersion: String, companyWebPage: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        caption_grey(text = stringResource(R.string.Settings_InfoTitleWithVersion, appVersion).uppercase())
        Divider(
            modifier = Modifier
                .width(100.dp)
                .padding(top = 8.dp, bottom = 4.5.dp),
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
                    LinkHelper.openLinkInAppBrowser(context, companyWebPage)
                },
            painter = painterResource(id = R.drawable.ic_company_logo),
            contentDescription = null,
        )
        caption_grey(
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
            text = stringResource(R.string.Settings_CompanyName),
        )
    }
}

@Preview
@Composable
private fun previewSettingsScreen() {
    ComposeAppTheme {
        Column {
            CellSingleLineLawrenceSection(
                listOf({
                    HsSettingCell(
                        R.string.Settings_Faq,
                        R.drawable.ic_faq_20,
                        showAlert = true,
                        onClick = { }
                    )
                }, {
                    HsSettingCell(
                        R.string.Guides_Title,
                        R.drawable.ic_academy_20,
                        onClick = { }
                    )
                })
            )

            Spacer(Modifier.height(32.dp))

            CellSingleLineLawrenceSection(
                listOf {
                    HsSettingCell(
                        R.string.Settings_WalletConnect,
                        R.drawable.ic_wallet_connect_20,
                        counterBadge = "13",
                        onClick = { }
                    )
                }
            )
        }
    }
}
