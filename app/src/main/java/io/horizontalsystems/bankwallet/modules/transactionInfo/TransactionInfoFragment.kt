package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class TransactionInfoFragment : BaseFragment() {

    private val viewModelTxs by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val viewItem = viewModelTxs.tmpItemToShow ?: run {
            findNavController().popBackStack()
            return null
        }

        val viewModel by navGraphViewModels<TransactionInfoViewModel>(R.id.transactionInfoFragment) {
            TransactionInfoModule.Factory(viewItem)
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                TransactionInfoScreen(viewModel, findNavController())
            }
        }
    }
}

@Composable
fun TransactionInfoScreen(
    viewModel: TransactionInfoViewModel,
    navController: NavController
) {

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.TransactionInfo_Title),
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
            TransactionInfo(viewModel, navController)
        }
    }
}

@Composable
fun TransactionInfo(
    viewModel: TransactionInfoViewModel,
    navController: NavController
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)) {
        items(viewModel.viewItems) { section ->
            TransactionInfoSection(section, navController, viewModel::getRawTransaction)
        }
    }
}

@Composable
fun TransactionInfoSection(
    section: List<TransactionInfoViewItem>,
    navController: NavController,
    getRawTransaction: () -> String?
) {
    CellUniversalLawrenceSection(
        buildList {
            for (viewItem in section) {
                when (viewItem) {
                    is TransactionInfoViewItem.Transaction -> {
                        add {
                            SectionTitleCell(title = viewItem.leftValue, value = viewItem.rightValue, iconResId = viewItem.icon)
                        }
                    }
                    is TransactionInfoViewItem.Amount -> {
                        add {
                            TransactionAmountCell(
                                fiatAmount = viewItem.fiatValue,
                                coinAmount = viewItem.coinValue,
                                coinIconUrl = viewItem.coinIconUrl,
                                coinIconPlaceholder = viewItem.coinIconPlaceholder,
                                coinUid = viewItem.coinUid,
                                navController = navController
                            )
                        }
                    }
                    is TransactionInfoViewItem.NftAmount -> {
                        add {
                            TransactionNftAmountCell(viewItem.nftValue, viewItem.iconUrl, viewItem.iconPlaceholder, viewItem.nftUid, viewItem.providerCollectionUid, navController)
                        }
                    }
                    is TransactionInfoViewItem.Value -> {
                        add {
                            TitleAndValueCell(title = viewItem.title, value = viewItem.value)
                        }
                    }
                    is TransactionInfoViewItem.Address -> {
                        add {
                            TransactionInfoAddressCell(
                                title = viewItem.title,
                                value = viewItem.value,
                                showAdd = viewItem.showAdd,
                                blockchainType = viewItem.blockchainType,
                                navController = navController
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
                            TransactionInfoStatusCell(status = viewItem.status, navController = navController)
                        }
                    }
                    is TransactionInfoViewItem.SpeedUpCancel -> {
                        add {
                            TransactionInfoSpeedUpCell(transactionHash = viewItem.transactionHash, navController = navController)
                        }
                        add {
                            TransactionInfoCancelCell(transactionHash = viewItem.transactionHash, navController = navController)
                        }
                    }
                    is TransactionInfoViewItem.TransactionHash -> {
                        add {
                            TransactionInfoTransactionHashCell(transactionHash = viewItem.transactionHash)
                        }
                    }
                    is TransactionInfoViewItem.Explorer -> {
                        viewItem.url?.let {
                            add {
                                TransactionInfoExplorerCell(title = viewItem.title, url = viewItem.url)
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
                            TransactionInfoBtcLockCell(lockState = viewItem, navController = navController)
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
                }
            }
        }
    )
}

