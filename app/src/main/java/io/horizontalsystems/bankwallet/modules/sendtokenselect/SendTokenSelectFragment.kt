package cash.p.terminal.modules.sendtokenselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.send.SendFragment
import cash.p.terminal.modules.tokenselect.TokenSelectScreen
import cash.p.terminal.modules.tokenselect.TokenSelectViewModel
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
                val navController = findNavController()
                TokenSelectScreen(
                    navController = navController,
                    title = stringResource(R.string.Balance_Send),
                    onClickEnabled = { it.sendEnabled },
                    onClickItem = {
                        navController.slideFromRight(
                            R.id.sendXFragment,
                            SendFragment.prepareParams(it.wallet, R.id.sendTokenSelectFragment)
                        )
                    },
                    viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend()),
                    emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
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
