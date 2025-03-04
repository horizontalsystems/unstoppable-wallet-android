package cash.p.terminal.modules.settings.main

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.RateAppManager
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.modules.contacts.ContactsFragment
import cash.p.terminal.modules.contacts.Mode
import cash.p.terminal.modules.manageaccount.dialogs.BackupRequiredDialog
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.walletconnect.WCAccountTypeNotSupportedDialog
import cash.p.terminal.modules.walletconnect.WCManager
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui.compose.components.BadgeText
import cash.p.terminal.ui_compose.components.CellSingleLineLawrenceSection
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.caption_grey
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.components.subhead1_jacob
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainSettingsViewModel = viewModel(factory = MainSettingsModule.Factory()),
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                stringResource(R.string.Settings_Title),
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
    val uiState = viewModel.uiState
    val context = LocalContext.current

    if (uiState.isUpdateAvailable) {
        CellUniversalLawrenceSection(
            listOf {
                HsSettingCell(
                    title = R.string.update_app,
                    icon = R.drawable.ic_refresh,
                    iconTint = ComposeAppTheme.colors.jacob,
                    showAlert = true,
                    onClick = {
                        RateAppManager.openPlayMarket(context)

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Update))
                    }
                )
            }
        )

        VSpacer(32.dp)
    }

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                title = R.string.Settings_Donate,
                icon = R.drawable.ic_heart_filled_24,
                iconTint = ComposeAppTheme.colors.jacob,
                onClick = {
                    navController.slideFromRight(R.id.donateTokenSelectFragment)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Donate))
                }
            )
        }
    )

    VSpacer(32.dp)

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.Settings_GetYourTokens,
                R.drawable.ic_uwt2_24,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, "https://t.me/piratecash_bot?start=mobile")
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
                showAlert = uiState.manageWalletShowAlert,
                onClick = {
                    navController.slideFromRight(
                        R.id.manageAccountsFragment,
                        ManageAccountsModule.Mode.Manage
                    )

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.ManageWallets))
                }
            )
        }, {
            HsSettingCell(
                R.string.BlockchainSettings_Title,
                R.drawable.ic_blocks_20,
                onClick = {
                    navController.slideFromRight(R.id.blockchainSettingsFragment)

                    stat(
                        page = StatPage.Settings,
                        event = StatEvent.Open(StatPage.BlockchainSettings)
                    )
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_WalletConnect,
                R.drawable.ic_wallet_connect_20,
                value = (uiState.wcCounterType as? MainSettingsModule.CounterType.SessionCounter)?.number?.toString(),
                counterBadge = (uiState.wcCounterType as? MainSettingsModule.CounterType.PendingRequestCounter)?.number?.toString(),
                onClick = {
                    when (val state = viewModel.walletConnectSupportState) {
                        WCManager.SupportState.Supported -> {
                            navController.slideFromRight(R.id.wcListFragment)

                            stat(
                                page = StatPage.Settings,
                                event = StatEvent.Open(StatPage.WalletConnect)
                            )
                        }

                        WCManager.SupportState.NotSupportedDueToNoActiveAccount -> {
                            navController.slideFromBottom(R.id.wcErrorNoAccountFragment)
                        }

                        is WCManager.SupportState.NotSupportedDueToNonBackedUpAccount -> {
                            val text = Translator.getString(R.string.WalletConnect_Error_NeedBackup)
                            navController.slideFromBottom(
                                R.id.backupRequiredDialog,
                                BackupRequiredDialog.Input(state.account, text)
                            )

                            stat(
                                page = StatPage.Settings,
                                event = StatEvent.Open(StatPage.BackupRequired)
                            )
                        }

                        is WCManager.SupportState.NotSupported -> {
                            navController.slideFromBottom(
                                R.id.wcAccountTypeNotSupportedDialog,
                                WCAccountTypeNotSupportedDialog.Input(state.accountTypeDescription)
                            )
                        }
                    }
                }
            )
        }, {
            HsSettingCell(
                title = R.string.Settings_TonConnect,
                icon = R.drawable.ic_ton_connect_24,
                value = null,
                counterBadge = null,
                onClick = {
                    navController.slideFromRight(R.id.tcListFragment)

                    stat(
                        page = StatPage.Settings,
                        event = StatEvent.Open(StatPage.TonConnect)
                    )
                }
            )
        }, {
            HsSettingCell(
                R.string.BackupManager_Title,
                R.drawable.ic_file_24,
                onClick = {
                    navController.slideFromRight(R.id.backupManagerFragment)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.BackupManager))
                }
            )
        }
        )
    )

    VSpacer(32.dp)

    CellUniversalLawrenceSection(
        listOf(
            {
                HsSettingCell(
                    R.string.Settings_SecurityCenter,
                    R.drawable.ic_security,
                    showAlert = uiState.securityCenterShowAlert,
                    onClick = {
                        navController.slideFromRight(R.id.securitySettingsFragment)

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Security))
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Contacts,
                    R.drawable.ic_user_20,
                    onClick = {
                        navController.slideFromRight(
                            R.id.contactsFragment,
                            ContactsFragment.Input(Mode.Full)
                        )

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Contacts))
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Settings_Appearance,
                    R.drawable.ic_brush_20,
                    onClick = {
                        navController.slideFromRight(R.id.appearanceFragment)

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Appearance))
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Settings_BaseCurrency,
                    R.drawable.ic_currency,
                    value = uiState.baseCurrencyCode,
                    onClick = {
                        navController.slideFromRight(R.id.baseCurrencySettingsFragment)

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.BaseCurrency))
                    }
                )
            },
            {
                HsSettingCell(
                    R.string.Settings_Language,
                    R.drawable.ic_language,
                    value = uiState.currentLanguage,
                    onClick = {
                        navController.slideFromRight(R.id.languageSettingsFragment)

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Language))
                    }
                )
            }
        )
    )

    VSpacer(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        subhead1_jacob(text = stringResource(id = R.string.Settings_JoinUnstoppables).uppercase())
    }
    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.Settings_Telegram,
                R.drawable.ic_telegram_filled_24,
                ComposeAppTheme.colors.jacob,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, App.appConfigProvider.appTelegramLink)

                    stat(
                        page = StatPage.Settings,
                        event = StatEvent.Open(StatPage.ExternalTelegram)
                    )
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Twitter,
                R.drawable.ic_twitter_filled_24,
                ComposeAppTheme.colors.jacob,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, App.appConfigProvider.appTwitterLink)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.ExternalTwitter))
                }
            )
        })
    )
    InfoText(
        text = stringResource(R.string.Settings_JoinUnstoppables_Description),
    )

    VSpacer(32.dp)
/*
    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.Settings_Faq,
                R.drawable.ic_faq_20,
                onClick = {
                    navController.slideFromRight(R.id.faqListFragment)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Faq))
                }
            )
        }, {
            HsSettingCell(
                R.string.Guides_Title,
                R.drawable.ic_academy_20,
                onClick = {
                    navController.slideFromRight(R.id.academyFragment)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Academy))
                }
            )
        })
    )

    VSpacer(32.dp)
*/
    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.SettingsAboutApp_Title,
                R.drawable.ic_about_app_20,
                showAlert = uiState.aboutAppShowAlert,
                onClick = {
                    navController.slideFromRight(R.id.aboutAppFragment)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.AboutApp))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_RateUs,
                R.drawable.ic_star_20,
                onClick = {
                    RateAppManager.openPlayMarket(context)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.RateUs))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_ShareThisWallet,
                R.drawable.ic_share_20,
                onClick = {
                    shareAppLink(uiState.appWebPageLink, context)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.TellFriends))
                }
            )
        }, {
            HsSettingCell(
                R.string.SettingsContact_Title,
                R.drawable.ic_mail_24,
                onClick = {
                    navController.slideFromBottom(R.id.contactOptionsDialog)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.ContactUs))
                },
            )
        })
    )

    VSpacer(32.dp)
}

@Composable
fun HsSettingCell(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    iconTint: Color? = null,
    value: String? = null,
    counterBadge: String? = null,
    showAlert: Boolean = false,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = iconTint ?: ComposeAppTheme.colors.grey
        )
        body_leah(
            text = stringResource(title),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.weight(1f))

        if (counterBadge != null) {
            BadgeText(
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
        caption_grey(
            text = stringResource(
                R.string.Settings_InfoTitleWithVersion,
                appVersion
            ).uppercase()
        )
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

private fun shareAppLink(appLink: String, context: Context) {
    val shareMessage = cash.p.terminal.strings.helpers.Translator.getString(R.string.SettingsShare_Text) + "\n" + appLink + "\n"
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            cash.p.terminal.strings.helpers.Translator.getString(R.string.SettingsShare_Title)
        )
    )
}

@Preview
@Composable
private fun previewSettingsScreen() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
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
