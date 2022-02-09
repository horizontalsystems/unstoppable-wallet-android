package io.horizontalsystems.bankwallet.modules.nft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.nft.ui.NftsCollectionSection
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.core.findNavController

class NftsFragment : BaseFragment() {

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
                NftsScreen(findNavController())
            }
        }
    }
}

@Composable
fun NftsScreen(navController: NavController) {
    val viewModel = viewModel<NftsViewModel>(factory = NftsModule.Factory())

    val viewState = viewModel.viewState
    val collections = viewModel.collections

    val loading = viewModel.loading

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Nfts_Title),
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }

            )
            HSSwipeRefresh(
                state = rememberSwipeRefreshState(loading),
                onRefresh = viewModel::refresh
            ) {
                when (viewState) {
                    is ViewState.Error -> {
                        ListErrorView(stringResource(R.string.Error)) {

                        }
                    }
                    ViewState.Success -> {
                        Column {
                            CellSingleLineClear(borderTop = true) {
                                Text(
                                    text = "$ total",
                                    style = ComposeAppTheme.typography.headline2,
                                    color = ComposeAppTheme.colors.jacob,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                ButtonSecondaryToggle(
                                    select = Select(viewModel.priceType, PriceType.values().toList()),
                                    onSelect = {
                                        viewModel.changePriceType(it)
                                    }
                                )
                            }

                            LazyColumn {
                                items(collections) { collection ->
                                    NftsCollectionSection(collection, viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

