package cash.p.terminal.modules.sendtokenselect

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
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.balance.ui.BalanceCardInner
import cash.p.terminal.modules.send.SendFragment
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.SearchBar
import cash.p.terminal.ui.compose.components.SectionUniversalItem
import cash.p.terminal.ui.compose.components.VSpacer
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

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                SearchBar(
                    title = stringResource(R.string.Balance_Send),
                    searchHintText = "",
                    menuItems = listOf(),
                    onClose = { navController.popBackStack() },
                    onSearchTextChanged = { text ->
                        viewModel.updateFilter(text)
                    }
                )
            }
        ) { paddingValues ->
            val balanceViewItems = viewModel.uiState.items
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
