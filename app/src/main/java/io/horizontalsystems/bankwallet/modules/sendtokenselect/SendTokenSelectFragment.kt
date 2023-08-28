package io.horizontalsystems.bankwallet.modules.sendtokenselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardInner
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.findNavController

class SendTokenSelectFragment : BaseFragment() {

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
                SendTokenSelectScreen(findNavController())
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SendTokenSelectScreen(
    navController: NavController,
) {
    val viewModel = viewModel<SendTokenSelectViewModel>(factory = SendTokenSelectViewModel.Factory())

    val uiState = viewModel.uiState

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                if (uiState.filteringEnabled) {
                    SearchBar(
                        title = stringResource(R.string.Balance_Send),
                        searchHintText = "",
                        menuItems = listOf(),
                        onClose = { navController.popBackStack() },
                        onSearchTextChanged = { text ->
                            viewModel.updateFilter(text)
                        }
                    )
                } else {
                    AppBar(
                        title = TranslatableString.ResString(R.string.Balance_Send),
                        navigationIcon = {
                            HsBackButton(onClick = { navController.popBackStack() })
                        }
                    )
                }
            }
        ) { paddingValues ->
            val balanceViewItems = uiState.items
            if (balanceViewItems.isEmpty()) {
                ListEmptyView(
                    text = stringResource(R.string.Balance_NoAssetsToSend),
                    icon = R.drawable.ic_empty_wallet
                )
            } else {
                LazyColumn(contentPadding = paddingValues) {
                    item {
                        VSpacer(12.dp)
                    }
                    itemsIndexed(balanceViewItems) { index, item ->
                        val lastItem = index == balanceViewItems.size - 1
                        val modifier = if (item.sendEnabled) {
                            Modifier.clickable {
                                navController.slideFromRight(
                                    R.id.sendXFragment,
                                    SendFragment.prepareParams(item.wallet)
                                )
                            }
                        } else {
                            Modifier
                        }

                        Box(modifier = modifier) {
                            SectionUniversalItem(
                                borderTop = true,
                                borderBottom = lastItem
                            ) {
                                BalanceCardInner(viewItem = item)
                            }
                        }
                    }
                    item {
                        VSpacer(32.dp)
                    }
                }
            }
        }
    }
}
