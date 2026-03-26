package cash.p.terminal.modules.transactionInfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.AmlStatusManager
import cash.p.terminal.core.restartMain
import cash.p.terminal.navigation.popBackStackOrExecute
import cash.p.terminal.core.orHide
import org.koin.compose.koinInject
import cash.p.terminal.modules.settings.addresschecker.AddressCheckFragment
import cash.p.terminal.modules.transactions.AmlCheckInfoBottomSheet
import cash.p.terminal.modules.transactions.AmlStatus
import cash.p.terminal.modules.transactions.TransactionsModule
import cash.p.terminal.modules.transactions.TransactionsViewModel
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.DescriptionCell
import cash.p.terminal.ui.compose.components.PriceWithToggleCell
import cash.p.terminal.ui.compose.components.SectionTitleCell
import cash.p.terminal.ui.compose.components.TransactionAmountCell
import cash.p.terminal.ui.compose.components.TransactionInfoAddressCell
import cash.p.terminal.ui.compose.components.TransactionInfoBtcLockCell
import cash.p.terminal.ui.compose.components.TransactionInfoCancelCell
import cash.p.terminal.ui.compose.components.TransactionInfoContactCell
import cash.p.terminal.ui.compose.components.TransactionInfoDoubleSpendCell
import cash.p.terminal.ui.compose.components.TransactionInfoExplorerCell
import cash.p.terminal.ui.compose.components.TransactionInfoRawTransaction
import cash.p.terminal.ui.compose.components.TransactionInfoAmlCheckCell
import cash.p.terminal.ui.compose.components.TransactionInfoSentToSelfCell
import cash.p.terminal.ui.compose.components.TransactionInfoSpeedUpCell
import cash.p.terminal.ui.compose.components.TransactionInfoStatusCell
import cash.p.terminal.ui.compose.components.TransactionInfoTransactionHashCell
import cash.p.terminal.ui.compose.components.TransactionNftAmountCell
import cash.p.terminal.modules.transactions.poison_status.AddressPoisoningInfoDialog
import cash.p.terminal.ui.compose.components.PoisonWarningCell
import cash.p.terminal.ui.compose.components.WarningMessageCell
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.CoinFragmentInput
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.ConnectionStatusView
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.TitleAndValueCell
import cash.p.terminal.ui_compose.components.TitleAndValueClickableCell
import cash.p.terminal.ui_compose.components.TitleAndValueColoredCell
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

class TransactionInfoFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val viewModelTxs: TransactionsViewModel? = try {
            navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }.value
        } catch (e: IllegalStateException) {
            null
        }

        val viewItem = viewModelTxs?.tmpItemToShow
        if (viewItem == null) {
            navController.popBackStackOrExecute(R.id.transactionInfoFragment, true) { activity?.restartMain() }
            return
        }

        val viewModel by navGraphViewModels<TransactionInfoViewModel>(R.id.transactionInfoFragment) {
            TransactionInfoModule.Factory(viewItem)
        }

        TransactionInfoScreen(viewModel, navController)
    }

}

@Composable
fun TransactionInfoScreen(
    viewModel: TransactionInfoViewModel,
    navController: NavController,
    amlStatusManager: AmlStatusManager = koinInject()
) {
    var showAmlInfoSheet by remember { mutableStateOf(false) }
    var amlAddressSelectionData by remember { mutableStateOf<AmlAddressSelectionData?>(null) }

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(R.string.TransactionInfo_Title),
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = {
                        navController.popBackStack()
                    }
                )
            )
        )
        Box(modifier = Modifier.weight(1f)) {
            TransactionInfo(
                viewModel = viewModel,
                navController = navController,
                onAmlInfoClick = { showAmlInfoSheet = true },
                onAmlRiskClick = { addresses, status ->
                    if (addresses.size == 1) {
                        navController.slideFromRight(
                            R.id.addressCheckFragment,
                            AddressCheckFragment.Input(addresses.first())
                        )
                    } else {
                        amlAddressSelectionData = AmlAddressSelectionData(
                            addresses = addresses.map { address ->
                                address to (amlStatusManager.getAddressStatus(address) ?: status)
                            }
                        )
                    }
                }
            )
            ConnectionStatusView(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }

    if (showAmlInfoSheet) {
        AmlCheckInfoBottomSheet(
            onPremiumSettingsClick = {
                showAmlInfoSheet = false
                navController.slideFromRight(R.id.premiumSettingsFragment)
            },
            onLaterClick = { showAmlInfoSheet = false },
            onDismiss = { showAmlInfoSheet = false }
        )
    }

    amlAddressSelectionData?.let { data ->
        AmlAddressSelectionBottomSheet(
            addresses = data.addresses,
            onAddressSelected = { address ->
                amlAddressSelectionData = null
                navController.slideFromRight(
                    R.id.addressCheckFragment,
                    AddressCheckFragment.Input(address)
                )
            },
            onLaterClick = { amlAddressSelectionData = null },
            onDismiss = { amlAddressSelectionData = null }
        )
    }
}

private data class AmlAddressSelectionData(
    val addresses: List<Pair<String, AmlStatus>>
)

@Composable
fun TransactionInfo(
    viewModel: TransactionInfoViewModel,
    navController: NavController,
    onAmlInfoClick: () -> Unit = {},
    onAmlRiskClick: (List<String>, AmlStatus) -> Unit = { _, _ -> }
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        items(viewModel.viewItems) { section ->
            TransactionInfoSection(
                section = section,
                navController = navController,
                onSensitiveValueClick = {
                    HudHelper.vibrate(App.instance)
                    viewModel.toggleBalanceVisibility()
                },
                getRawTransaction = viewModel::getRawTransaction,
                hideSensitiveInfo = viewModel.balanceHidden,
                onAmlInfoClick = onAmlInfoClick,
                onAmlRiskClick = onAmlRiskClick
            )
        }
    }
}

@Composable
fun TransactionInfoSection(
    section: List<TransactionInfoViewItem>,
    navController: NavController,
    onSensitiveValueClick: () -> Unit,
    getRawTransaction: () -> String?,
    hideSensitiveInfo: Boolean,
    onAmlInfoClick: () -> Unit = {},
    onAmlRiskClick: (List<String>, AmlStatus) -> Unit = { _, _ -> }
) {
    //items without background
    if (section.size == 1) {
        when (val item = section[0]) {
            is TransactionInfoViewItem.WarningMessage -> {
                WarningMessageCell(item.message)
                return
            }

            is TransactionInfoViewItem.PoisonWarning -> {
                var showPoisoningInfo by remember { mutableStateOf(false) }
                PoisonWarningCell(onInfoClick = { showPoisoningInfo = true })
                if (showPoisoningInfo) {
                    AddressPoisoningInfoDialog(onDismiss = { showPoisoningInfo = false })
                }
                return
            }

            is TransactionInfoViewItem.Description -> {
                DescriptionCell(text = item.text)
                return
            }

            else -> {
                //do nothing
            }
        }
    }

    CellUniversalLawrenceSection(
        buildList {
            for (viewItem in section) {
                when (viewItem) {
                    is TransactionInfoViewItem.Transaction -> {
                        add {
                            SectionTitleCell(
                                title = viewItem.leftValue,
                                value = viewItem.rightValue,
                                iconResId = viewItem.icon
                            )
                        }
                    }

                    is TransactionInfoViewItem.Amount -> {
                        add {
                            TransactionAmountCell(
                                amountType = viewItem.amountType,
                                fiatAmount = viewItem.fiatValue,
                                coinAmount = viewItem.coinValue,
                                coinIconUrl = viewItem.coinIconUrl,
                                alternativeCoinIconUrl = viewItem.alternativeCoinIconUrl,
                                badge = viewItem.badge,
                                coinIconPlaceholder = viewItem.coinIconPlaceholder,
                                onValueClick = onSensitiveValueClick,
                                onClick = viewItem.coinUid?.let {
                                    {
                                        navController.slideFromRight(
                                            R.id.coinFragment,
                                            CoinFragmentInput(it)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    is TransactionInfoViewItem.NftAmount -> {
                        add {
                            TransactionNftAmountCell(
                                viewItem.title,
                                viewItem.nftValue,
                                viewItem.nftName,
                                viewItem.iconUrl,
                                viewItem.iconPlaceholder,
                                viewItem.badge,
                            )
                        }
                    }

                    is TransactionInfoViewItem.Value -> {
                        add {
                            TitleAndValueCell(
                                title = viewItem.title,
                                value = viewItem.value,
                            )
                        }
                    }

                    is TransactionInfoViewItem.ValueClickable -> {
                        add {
                            TitleAndValueClickableCell(
                                title = viewItem.title,
                                value = viewItem.value,
                                onClick = onSensitiveValueClick,
                            )
                        }
                    }

                    is TransactionInfoViewItem.ValueColored -> {
                        add {
                            TitleAndValueColoredCell(
                                title = viewItem.title,
                                value = viewItem.value,
                                color = viewItem.color,
                            )
                        }
                    }

                    is TransactionInfoViewItem.PriceWithToggle -> {
                        add {
                            PriceWithToggleCell(
                                title = viewItem.title,
                                valueOne = viewItem.valueTwo,
                                valueTwo = viewItem.valueOne
                            )
                        }
                    }

                    is TransactionInfoViewItem.Address -> {
                        add {
                            TransactionInfoAddressCell(
                                title = viewItem.title,
                                value = viewItem.value.orHide(hideSensitiveInfo),
                                showAdd = viewItem.showAdd,
                                blockchainType = viewItem.blockchainType,
                                navController = navController,
                                textAlign = if (!hideSensitiveInfo) TextAlign.End else TextAlign.Start,
                                onCopy = {
                                },
                                onAddToExisting = {
                                },
                                onAddToNew = {
                                },
                                onValueClick = onSensitiveValueClick,
                                showCopyWarning = viewItem.showCopyWarning,
                            )
                        }
                    }

                    is TransactionInfoViewItem.ContactItem -> {
                        add {
                            TransactionInfoContactCell(viewItem.contact.name)
                        }
                    }

                    is TransactionInfoViewItem.Status -> {
                        add {
                            TransactionInfoStatusCell(
                                status = viewItem.status,
                                navController = navController
                            )
                        }
                    }

                    is TransactionInfoViewItem.SpeedUpCancel -> {
                        add {
                            TransactionInfoSpeedUpCell(
                                transactionHash = viewItem.transactionHash,
                                blockchainType = viewItem.blockchainType,
                                navController = navController
                            )
                        }
                        add {
                            TransactionInfoCancelCell(
                                transactionHash = viewItem.transactionHash,
                                blockchainType = viewItem.blockchainType,
                                navController = navController
                            )
                        }
                    }

                    is TransactionInfoViewItem.TransactionHash -> {
                        if (viewItem.transactionHash.isNotEmpty()) {
                            add {
                                TransactionInfoTransactionHashCell(transactionHash = viewItem.transactionHash)
                            }
                        }
                    }

                    is TransactionInfoViewItem.Explorer -> {
                        viewItem.url?.let {
                            add {
                                TransactionInfoExplorerCell(
                                    title = viewItem.title,
                                    url = viewItem.url
                                )
                            }
                        }
                    }

                    is TransactionInfoViewItem.RawTransaction -> {
                        add {
                            TransactionInfoRawTransaction(rawTransaction = getRawTransaction)
                        }
                    }

                    is TransactionInfoViewItem.LockState -> {
                        add {
                            TransactionInfoBtcLockCell(
                                lockState = viewItem,
                                navController = navController
                            )
                        }
                    }

                    is TransactionInfoViewItem.DoubleSpend -> {
                        add {
                            TransactionInfoDoubleSpendCell(
                                transactionHash = viewItem.transactionHash,
                                conflictingHash = viewItem.conflictingHash,
                                navController = navController
                            )
                        }
                    }

                    is TransactionInfoViewItem.SentToSelf -> {
                        add {
                            TransactionInfoSentToSelfCell()
                        }
                    }

                    is TransactionInfoViewItem.AmlCheck -> {
                        add {
                            TransactionInfoAmlCheckCell(
                                status = viewItem.status,
                                onInfoClick = onAmlInfoClick,
                                onRiskClick = {
                                    onAmlRiskClick(
                                        viewItem.senderAddresses,
                                        viewItem.status
                                    )
                                }
                            )
                        }
                    }

                    else -> {
                        //do nothing
                    }
                }
            }
        }
    )
}

