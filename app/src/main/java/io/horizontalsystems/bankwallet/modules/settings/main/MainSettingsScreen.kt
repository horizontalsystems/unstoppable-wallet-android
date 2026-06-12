package io.horizontalsystems.bankwallet.modules.settings.main

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.SimulateFailSwapMode
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupManagerPage
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsPage
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRouterPage
import io.horizontalsystems.bankwallet.modules.contacts.Mode
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.settings.about.AboutPage
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.AddressCheckPage
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppearancePage
import io.horizontalsystems.bankwallet.modules.settings.banners.DonateBanner
import io.horizontalsystems.bankwallet.modules.settings.banners.SubscriptionBanner
import io.horizontalsystems.bankwallet.modules.settings.donate.DonateTokenSelectPage
import io.horizontalsystems.bankwallet.modules.settings.donate.WhyDonatePage
import io.horizontalsystems.bankwallet.modules.settings.faq.FaqListPage
import io.horizontalsystems.bankwallet.modules.settings.guides.GuidesPage
import io.horizontalsystems.bankwallet.modules.settings.main.ui.BannerCarousel
import io.horizontalsystems.bankwallet.modules.settings.privacy.PrivacySettingsPage
import io.horizontalsystems.bankwallet.modules.settings.security.SecuritySettingsPage
import io.horizontalsystems.bankwallet.modules.settings.subscription.SubscriptionPage
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionHavHostPage
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedSheet
import io.horizontalsystems.bankwallet.modules.walletconnect.WCErrorNoAccountSheet
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WCListPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.LocalAppConfigProvider
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeText
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.PremiumHeader
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.dapp.core.DAppManager
import io.horizontalsystems.subscriptions.core.PrioritySupport
import io.horizontalsystems.subscriptions.core.SecureSend

@Composable
fun SettingsScreen(
    navController: HSNavigation,
    viewModel: MainSettingsViewModel = hiltViewModel(),
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
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                SettingSections(
                    viewModel = viewModel,
                    navController = navController,
                    )
                SettingsFooter(viewModel.appVersion, viewModel.companyWebPage)
                VSpacer(70.dp)
            }
        }
    }
}

@Composable
private fun SettingSections(
    viewModel: MainSettingsViewModel,
    navController: HSNavigation,
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val appConfigProvider = LocalAppConfigProvider.current
    val isFDroidBuild = BuildConfig.FDROID_BUILD

    val banners = buildList<@Composable () -> Unit> {
        if (uiState.showPremiumBanner) {
            add {
                SubscriptionBanner(
                    onClick = {
                        navController.slideFromBottom(BuySubscriptionHavHostPage)
                        stat(
                            page = StatPage.Settings,
                            event = StatEvent.OpenPremium(StatPremiumTrigger.Banner)
                        )
                    }
                )
            }
        }
        if (isFDroidBuild) {
            add {
                DonateBanner(
                    onClick = {
                        navController.slideFromBottom(WhyDonatePage)
                    }
                )
            }
        }
    }

    BannerCarousel(banners = banners)

    CellUniversalLawrenceSection(
        listOfNotNull({
            HsSettingCell(
                R.string.SettingsSecurity_ManageKeys,
                R.drawable.wallet_24,
                showAlert = uiState.manageWalletShowAlert,
                onClick = {
                    navController.slideFromRight(
                        ManageAccountsPage(ManageAccountsModule.Mode.Manage)
                    )

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.ManageWallets))
                }
            )
        }, {
            HsSettingCell(
                R.string.BlockchainSettings_Title,
                R.drawable.box_24,
                onClick = {
                    navController.slideFromRight(BlockchainSettingsPage)

                    stat(
                        page = StatPage.Settings,
                        event = StatEvent.Open(StatPage.BlockchainSettings)
                    )
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_SecurityCenter,
                R.drawable.shield_24,
                showAlert = uiState.securityCenterShowAlert,
                onClick = {
                    navController.slideFromRight(SecuritySettingsPage)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Security))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Privacy,
                R.drawable.lock_24,
                onClick = {
                    navController.slideFromRight(PrivacySettingsPage)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.Privacy))
                }
            )
        }, if (DAppManager.isAvailable) {
            {
                HsSettingCell(
                    R.string.DAppConnection_Title,
                    R.drawable.link_24,
                    value = (uiState.wcCounterType as? MainSettingsModule.CounterType.SessionCounter)?.number?.toString(),
                    counterBadge = (uiState.wcCounterType as? MainSettingsModule.CounterType.PendingRequestCounter)?.number?.toString(),
                    onClick = {
                        when (val state = viewModel.walletConnectSupportState) {
                            WCManager.SupportState.Supported -> {
                                navController.slideFromRight(WCListPage())

                                stat(
                                    page = StatPage.Settings,
                                    event = StatEvent.Open(StatPage.WalletConnect)
                                )
                            }

                            WCManager.SupportState.NotSupportedDueToNoActiveAccount -> {
                                navController.slideFromBottom(WCErrorNoAccountSheet)
                            }

                            is WCManager.SupportState.NotSupported -> {
                                navController.slideFromBottom(
                                    WCAccountTypeNotSupportedSheet(WCAccountTypeNotSupportedSheet.Input(state.accountTypeDescription))
                                )
                            }
                        }
                    }
                )
            }
        } else {
            null
        },
//            {
//            HsSettingCell(
//                title = R.string.Settings_TonConnect,
//                icon = R.drawable.ic_ton_connect_24,
//                value = null,
//                counterBadge = null,
//                onClick = {
//                    navController.slideFromRight(R.id.tcListFragment)
//
//                    stat(
//                        page = StatPage.Settings,
//                        event = StatEvent.Open(StatPage.TonConnect)
//                    )
//                }
//            )
//        }
        )
    )

    VSpacer(24.dp)

    CellUniversalLawrenceSection(
        buildList {
            add {
                HsSettingCell(
                    R.string.Contacts,
                    R.drawable.user_24,
                    onClick = {
                        navController.slideFromRight(
                            ContactsRouterPage(ContactsRouterPage.Input(Mode.Full))
                        )

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Contacts))
                    }
                )
            }
        }
    )

    VSpacer(24.dp)

    CellUniversalLawrenceSection(
        buildList {
            add {
                HsSettingCell(
                    R.string.Settings_AppSettings,
                    R.drawable.uw_logo_24,
                    onClick = {
                        navController.slideFromRight(AppearancePage)

                        stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Appearance))
                    }
                )
            }
            if (!BuildConfig.FDROID_BUILD) {
                add {
                    HsSettingCell(
                        R.string.Settings_Subscription,
                        R.drawable.premium_24,
                        value = if (uiState.hasSubscription) stringResource(R.string.SettingsSubscription_Active) else null,
                        onClick = {
                            navController.slideFromRight(SubscriptionPage)
                        }
                    )
                }
            }

            add {
                HsSettingCell(
                    R.string.BackupManager_Title,
                    R.drawable.file_24,
                    onClick = {
                        navController.slideFromRight(BackupManagerPage)

                        stat(
                            page = StatPage.Settings,
                            event = StatEvent.Open(StatPage.BackupManager)
                        )
                    }
                )
            }
        }
    )

    VSpacer(4.dp)

    if (isFDroidBuild) {
        PremiumHeader(R.string.Premium_TitleForDroid)
    } else {
        PremiumHeader()
    }

    SectionPremiumUniversalLawrence {
        HsSettingCell(
            title = if(isFDroidBuild) R.string.Settings_Support else R.string.Settings_VipSupport,
            icon = R.drawable.ic_support_yellow_24,
            iconTint = ComposeAppTheme.colors.jacob,
            onClick = {
                if (isFDroidBuild) {
                    LinkHelper.openLinkInAppBrowser(context, viewModel.fdroidSupportLink)
                } else {
                    navController.paidAction(PrioritySupport) {
                        LinkHelper.openLinkInAppBrowser(context, viewModel.vipSupportLink)
                    }
                }

                stat(
                    page = StatPage.Settings,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.VipSupport)
                )
            }
        )
        HsDivider()
        HsSettingCell(
            title = R.string.SettingsAddressChecker_Title,
            icon = R.drawable.ic_radar_24,
            iconTint = ComposeAppTheme.colors.jacob,
            onClick = {
                navController.paidAction(SecureSend) {
                    navController.slideFromRight(AddressCheckPage)
                }
                stat(
                    page = StatPage.Settings,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.AddressChecker)
                )
            }
        )
    }


    VSpacer(24.dp)

    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.SettingsAboutApp_Title,
                R.drawable.ic_info_20,
                showAlert = uiState.aboutAppShowAlert,
                onClick = {
                    navController.slideFromRight(AboutPage)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.AboutApp))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_RateUs,
                R.drawable.star_24,
                onClick = {
                    RateAppManager.openPlayMarket(context)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.RateUs))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_ShareThisWallet,
                R.drawable.ic_share_24,
                onClick = {
                    shareAppLink(uiState.appWebPageLink, context)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.TellFriends))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Faq,
                R.drawable.message_24,
                onClick = {
                    navController.slideFromRight(FaqListPage)
                }
            )
        }, {
            HsSettingCell(
                R.string.Guides_Title,
                R.drawable.book_24,
                onClick = {
                    navController.slideFromRight(GuidesPage)
                }
            )
        })
    )

    VSpacer(24.dp)

    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.Settings_Telegram,
                R.drawable.ic_telegram_24,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, appConfigProvider.appTelegramLink)

                    stat(
                        page = StatPage.Settings,
                        event = StatEvent.Open(StatPage.ExternalTelegram)
                    )
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Twitter,
                R.drawable.x_logo_24,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, appConfigProvider.appTwitterLink)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.ExternalTwitter))
                }
            )
        })
    )

    VSpacer(24.dp)

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.Settings_Donate,
                R.drawable.ic_heart_24,
                onClick = {
                    navController.slideFromRight(DonateTokenSelectPage)

                    stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Donate))
                }
            )
        }
    )

    if (BuildConfig.DEBUG) {
        VSpacer(24.dp)

        DebugSettingsSection()
    }

    VSpacer(32.dp)
}

@Composable
private fun DebugSettingsSection() {
    var simulateFailSwap by remember { mutableStateOf(App.localStorage.simulateFailSwap) }

    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
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
        }
    )
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
        HsDivider(
            modifier = Modifier
                .width(100.dp)
                .padding(top = 8.dp, bottom = 4.5.dp)
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
