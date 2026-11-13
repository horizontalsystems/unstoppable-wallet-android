package io.horizontalsystems.walletkit.modules.settings.main

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.dapp.core.DAppManager
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.subscriptions.core.PrioritySupport
import io.horizontalsystems.walletkit.BuildConfig
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.RateAppManager
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.StatPremiumTrigger
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.entities.SimulateFailSwapMode
import io.horizontalsystems.walletkit.modules.backuplocal.fullbackup.BackupManagerPage
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsPage
import io.horizontalsystems.walletkit.modules.contacts.ContactsRouterPage
import io.horizontalsystems.walletkit.modules.contacts.Mode
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.settings.about.AboutPage
import io.horizontalsystems.walletkit.modules.settings.appearance.AppearancePage
import io.horizontalsystems.walletkit.modules.settings.banners.DonateBanner
import io.horizontalsystems.walletkit.modules.settings.donate.DonateTokenSelectPage
import io.horizontalsystems.walletkit.modules.settings.donate.WhyDonatePage
import io.horizontalsystems.walletkit.modules.settings.faq.FaqListPage
import io.horizontalsystems.walletkit.modules.settings.labs.UnstoppableLabsPage
import io.horizontalsystems.walletkit.modules.settings.privacy.PrivacySettingsPage
import io.horizontalsystems.walletkit.modules.settings.security.SecuritySettingsPage
import io.horizontalsystems.walletkit.modules.settings.subscription.SubscriptionPage
import io.horizontalsystems.walletkit.modules.walletconnect.WCAccountTypeNotSupportedSheet
import io.horizontalsystems.walletkit.modules.walletconnect.WCErrorNoAccountSheet
import io.horizontalsystems.walletkit.modules.walletconnect.WCManager
import io.horizontalsystems.walletkit.modules.walletconnect.list.WCListPage
import io.horizontalsystems.walletkit.ui.compose.components.AppBar
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.walletkit.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_grey
import io.horizontalsystems.walletkit.ui.helpers.LinkHelper
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.BoxBordered
import io.horizontalsystems.walletkit.uiv3.components.Section
import io.horizontalsystems.walletkit.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.HSString
import io.horizontalsystems.walletkit.uiv3.components.cell.ImageType
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

@Composable
fun SettingsScreen(
    navigation: HSNavigation,
    viewModel: MainSettingsViewModel = viewModel(factory = MainSettingsModule.Factory()),
) {

    Surface(
        color = ComposeAppTheme.colors.tyler
    ) {
        Column {
            AppBar(
                stringResource(R.string.Settings_Title),
            )

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    // The tab bar is drawn over the bottom of this screen
                    .padding(top = 16.dp, bottom = TabBarHeight + 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingSections(
                    viewModel = viewModel,
                    navigation = navigation,
                )
                SettingsFooter(viewModel.appVersion)
            }
        }
    }
}

private val TabBarHeight = 56.dp

@Composable
private fun SettingSections(
    viewModel: MainSettingsViewModel,
    navigation: HSNavigation,
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val isFDroidBuild = App.appConfigProvider.fdroidBuild

    if (isFDroidBuild) {
        DonateBanner(
            modifier = Modifier.testTag(TestTags.SETTINGS_DONATE_BANNER),
            onClick = {
                navigation.slideFromBottom(WhyDonatePage)
            }
        )
    }

    SettingsSection {
        cell {
            SettingsCell(
                title = R.string.SettingsSecurity_ManageKeys,
                icon = R.drawable.wallet_24,
                testTag = TestTags.SETTINGS_MANAGE_WALLETS,
                showAlert = uiState.manageWalletShowAlert,
                onClick = {
                    navigation.slideFromRight(
                        ManageAccountsPage(ManageAccountsModule.Mode.Manage)
                    )

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.ManageWallets))
                }
            )
        }
    }

    SettingsSection {
        cell {
            SettingsCell(
                title = R.string.Settings_SecurityCenter,
                icon = R.drawable.shield_24,
                testTag = TestTags.SETTINGS_SECURITY,
                showAlert = uiState.securityCenterShowAlert,
                onClick = {
                    navigation.slideFromRight(SecuritySettingsPage)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Security))
                }
            )
        }
        cell {
            SettingsCell(
                title = R.string.Settings_Privacy,
                icon = R.drawable.lock_24,
                testTag = TestTags.SETTINGS_PRIVACY,
                onClick = {
                    navigation.slideFromRight(PrivacySettingsPage)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.Privacy))
                }
            )
        }
        cell {
            SettingsCell(
                title = R.string.Settings_AppSettings,
                icon = R.drawable.uw_logo_24,
                testTag = TestTags.SETTINGS_APP_SETTINGS,
                onClick = {
                    navigation.slideFromRight(AppearancePage)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Appearance))
                }
            )
        }
        cell {
            SettingsCell(
                title = R.string.BlockchainSettings_Title,
                icon = R.drawable.box_24,
                testTag = TestTags.SETTINGS_BLOCKCHAIN,
                onClick = {
                    navigation.slideFromRight(BlockchainSettingsPage)

                    stat(
                        page = StatPage.Settings,
                        event = StatEvent.Open(StatPage.BlockchainSettings)
                    )
                }
            )
        }
        if (DAppManager.isAvailable) {
            cell {
                SettingsCell(
                    title = R.string.DAppConnection_Title,
                    icon = R.drawable.link_24,
                    testTag = TestTags.SETTINGS_DAPP_CONNECTION,
                    value = when (val counter = uiState.wcCounterType) {
                        is MainSettingsModule.CounterType.PendingRequestCounter -> {
                            counter.number.toString().hs(color = ComposeAppTheme.colors.lucian)
                        }
                        is MainSettingsModule.CounterType.SessionCounter -> counter.number.toString().hs
                        null -> null
                    },
                    onClick = {
                        when (val state = viewModel.walletConnectSupportState) {
                            WCManager.SupportState.Supported -> {
                                navigation.slideFromRight(WCListPage())

                                stat(
                                    page = StatPage.Settings,
                                    event = StatEvent.Open(StatPage.WalletConnect)
                                )
                            }

                            WCManager.SupportState.NotSupportedDueToNoActiveAccount -> {
                                navigation.slideFromBottom(WCErrorNoAccountSheet)
                            }

                            is WCManager.SupportState.NotSupported -> {
                                navigation.slideFromBottom(
                                    WCAccountTypeNotSupportedSheet(WCAccountTypeNotSupportedSheet.Input(state.accountTypeDescription))
                                )
                            }
                        }
                    }
                )
            }
        }
        cell {
            SettingsCell(
                title = R.string.BackupManager_Title,
                icon = R.drawable.cloud_24,
                testTag = TestTags.SETTINGS_BACKUP_MANAGER,
                onClick = {
                    navigation.slideFromRight(BackupManagerPage)

                    stat(
                        page = StatPage.Settings,
                        event = StatEvent.Open(StatPage.BackupManager)
                    )
                }
            )
        }
    }

    SettingsSection {
        cell {
            SettingsCell(
                title = R.string.Contacts,
                icon = R.drawable.user_24,
                testTag = TestTags.SETTINGS_CONTACTS,
                onClick = {
                    navigation.slideFromRight(
                        ContactsRouterPage(ContactsRouterPage.Input(Mode.Full))
                    )

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Contacts))
                }
            )
        }
    }

    if (!isFDroidBuild) {
        SettingsSection {
            cell {
                SettingsCell(
                    title = R.string.Settings_Subscription,
                    icon = R.drawable.premium_24,
                    testTag = TestTags.SETTINGS_SUBSCRIPTION,
                    value = if (uiState.hasSubscription) stringResource(R.string.SettingsSubscription_Active).hs else null,
                    onClick = {
                        navigation.slideFromRight(SubscriptionPage)
                    }
                )
            }
        }
    }

    SettingsSection {
        cell {
            SettingsCell(
                title = R.string.SettingsAboutApp_Title,
                icon = R.drawable.info_24,
                testTag = TestTags.SETTINGS_ABOUT,
                onClick = {
                    navigation.slideFromRight(AboutPage)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.AboutApp))
                }
            )
        }
        cell {
            SettingsCell(
                title = R.string.Settings_RateUs,
                icon = R.drawable.star_24,
                testTag = TestTags.SETTINGS_RATE_US,
                onClick = {
                    RateAppManager.openPlayMarket(context)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.RateUs))
                }
            )
        }
        cell {
            SettingsCell(
                title = R.string.Settings_ShareThisWallet,
                icon = R.drawable.arrow_out_24,
                testTag = TestTags.SETTINGS_TELL_FRIENDS,
                onClick = {
                    shareAppLink(uiState.appWebPageLink, context)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.TellFriends))
                }
            )
        }
        cell {
            SettingsCell(
                title = R.string.Settings_Faq,
                icon = R.drawable.message_24,
                testTag = TestTags.SETTINGS_FAQ,
                onClick = {
                    navigation.slideFromRight(FaqListPage)
                }
            )
        }
    }

    SettingsSection {
        cell {
            SettingsCell(
                title = R.string.UnstoppableLabs_Title,
                icon = R.drawable.flask_24,
                testTag = TestTags.SETTINGS_LABS,
                onClick = {
                    navigation.slideFromRight(UnstoppableLabsPage)
                }
            )
        }
    }

    SectionPremiumUniversalLawrence {
        SettingsCell(
            title = if (isFDroidBuild) R.string.Settings_Support else R.string.Settings_VipSupport,
            icon = R.drawable.support_24,
            iconTint = ComposeAppTheme.colors.orange,
            testTag = TestTags.SETTINGS_SUPPORT,
            onClick = {
                if (isFDroidBuild) {
                    LinkHelper.openLinkInAppBrowser(context, viewModel.fdroidSupportLink)
                } else {
                    navigation.paidAction(PrioritySupport) {
                        LinkHelper.openLinkInAppBrowser(context, viewModel.vipSupportLink)
                    }
                }

                stat(
                    page = StatPage.Settings,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.VipSupport)
                )
            }
        )
    }

    SettingsSection {
        cell {
            SettingsCell(
                title = R.string.Settings_Telegram,
                icon = R.drawable.telegram_logo_24,
                testTag = TestTags.SETTINGS_TELEGRAM,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, App.appConfigProvider.appTelegramLink)

                    stat(
                        page = StatPage.Settings,
                        event = StatEvent.Open(StatPage.ExternalTelegram)
                    )
                }
            )
        }
        cell {
            SettingsCell(
                title = R.string.Settings_Twitter,
                icon = R.drawable.x_logo_24,
                testTag = TestTags.SETTINGS_TWITTER,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, App.appConfigProvider.appTwitterLink)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.ExternalTwitter))
                }
            )
        }
    }

    if (uiState.donateEnabled) {
        SettingsSection {
            cell {
                SettingsCell(
                    title = R.string.Settings_Donate,
                    icon = R.drawable.ic_heart_24,
                    testTag = TestTags.SETTINGS_DONATE,
                    onClick = {
                        navigation.slideFromRight(DonateTokenSelectPage)

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Donate))
                    }
                )
            }
        }
    }

    if (BuildConfig.DEBUG) {
        DebugSettingsSection()
    }
}

@Composable
private fun DebugSettingsSection() {
    var simulateFailSwap by remember { mutableStateOf(App.localStorage.simulateFailSwap) }
    var showSwapProviderName by remember { mutableStateOf(App.localStorage.showSwapProviderName) }

    CellUniversalLawrenceSection(
        listOf({
            RowUniversal(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(TestTags.SETTINGS_DEBUG_SIMULATE_FAIL_SWAP),
                onClick = {
                    // Cycle None -> Server -> Local -> None
                    val modes = SimulateFailSwapMode.entries
                    val next = modes[(simulateFailSwap.ordinal + 1) % modes.size]
                    simulateFailSwap = next
                    App.localStorage.simulateFailSwap = next
                }
            ) {
                body_leah(
                    text = "Simulate Fail Swap",
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                subhead1_grey(
                    text = simulateFailSwap.name.lowercase(),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }, {
            RowUniversal(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(TestTags.SETTINGS_DEBUG_SWAP_PROVIDER_NAME),
                onClick = {
                    showSwapProviderName = !showSwapProviderName
                    App.localStorage.showSwapProviderName = showSwapProviderName
                }
            ) {
                body_leah(
                    text = "Show swap provider name",
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                subhead1_grey(
                    text = showSwapProviderName.toString(),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        })
    )
}

private class SettingsSectionScope {
    val cells = mutableListOf<@Composable () -> Unit>()

    fun cell(content: @Composable () -> Unit) {
        cells.add(content)
    }
}

@Composable
private fun SettingsSection(content: SettingsSectionScope.() -> Unit) {
    val cells = SettingsSectionScope().apply(content).cells

    Section {
        cells.forEachIndexed { index, cell ->
            BoxBordered(top = index != 0) {
                cell()
            }
        }
    }
}

@Composable
private fun SettingsCell(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    testTag: String,
    iconTint: Color = ComposeAppTheme.colors.grey,
    value: HSString? = null,
    showAlert: Boolean = false,
    onClick: () -> Unit
) {
    CellPrimary(
        modifier = Modifier
            .heightIn(min = 64.dp)
            .testTag(testTag),
        left = {
            CellLeftImage(
                painter = painterResource(icon),
                type = ImageType.Rectangle,
                size = 24,
                colorFilter = ColorFilter.tint(iconTint),
            )
        },
        middle = {
            CellMiddleInfo(title = stringResource(title).hs)
        },
        right = {
            CellRightNavigation(
                subtitle = value,
                icon = if (showAlert) painterResource(R.drawable.warning_filled_24) else null,
                iconTint = ComposeAppTheme.colors.lucian,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun SettingsFooter(appVersion: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
            text = stringResource(R.string.Settings_InfoTitleWithVersion, appVersion) +
                    "\n" + stringResource(R.string.Premium_ApprovedBy),
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.grey,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                R.drawable.bitcoin_logo to R.string.Settings_AuditLogo_Bitcoin,
                R.drawable.wallet_scrutiny_logo to R.string.Settings_AuditLogo_WalletScrutiny,
                R.drawable.certik_logo to R.string.Settings_AuditLogo_Certik,
            ).forEach { (logo, name) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(logo),
                        contentDescription = stringResource(name),
                        tint = ComposeAppTheme.colors.grey,
                    )
                }
            }
        }
    }
}

private fun shareAppLink(appLink: String, context: Context) {
    val shareMessage = Translator.getString(R.string.SettingsShare_Text) + "\n" + appLink + "\n"
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            Translator.getString(R.string.SettingsShare_Title)
        )
    )
}

@Preview
@Composable
private fun Preview_SettingsSection() {
    ComposeAppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSection {
                cell {
                    SettingsCell(
                        title = R.string.Settings_SecurityCenter,
                        icon = R.drawable.shield_24,
                        testTag = TestTags.SETTINGS_SECURITY,
                        showAlert = true,
                        onClick = { }
                    )
                }
                cell {
                    SettingsCell(
                        title = R.string.DAppConnection_Title,
                        icon = R.drawable.link_24,
                        testTag = TestTags.SETTINGS_DAPP_CONNECTION,
                        value = "13".hs(color = ComposeAppTheme.colors.lucian),
                        onClick = { }
                    )
                }
            }
            SettingsFooter("1.0")
        }
    }
}
