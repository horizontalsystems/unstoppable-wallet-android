package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.addtoken.blockchainselector.AddTokenBlockchainSelectorScreen
import io.horizontalsystems.bankwallet.modules.addtoken.blockchainselector.BlockchainSelectorResult
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.TitleValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.delay

class AddTokenFragment : BaseFragment() {

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
                AddTokenNavHost(findNavController())
            }
        }
    }
}

private const val AddTokenPage = "add_token"
private const val BlockchainSelectorPage = "blockchain_selector"

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AddTokenNavHost(
    fragmentNavController: NavController,
    viewModel: AddTokenViewModel = viewModel(factory = AddTokenModule.Factory())
) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = AddTokenPage,
    ) {
        composable(AddTokenPage) {
            AddTokenScreen(
                navController = navController,
                closeScreen = { fragmentNavController.popBackStack() },
                viewModel = viewModel
            )
        }
        composablePage(BlockchainSelectorPage) {
            AddTokenBlockchainSelectorScreen(
                blockchains = viewModel.blockchains,
                selectedBlockchain = viewModel.selectedBlockchain,
                navController = navController
            )
        }
    }
}

@Composable
private fun AddTokenScreen(
    navController: NavController,
    closeScreen: () -> Unit,
    viewModel: AddTokenViewModel,
) {
    navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<List<Blockchain>>(BlockchainSelectorResult, emptyList())
        ?.collectAsState()?.value?.let { selectedItems ->
            if (selectedItems.isNotEmpty()) {
                viewModel.onBlockchainSelect(selectedItems.first())
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set<List<Blockchain>>(BlockchainSelectorResult, emptyList())
            }
        }

    val uiState = viewModel.uiState
    val view = LocalView.current

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) {
            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.LONG)
            delay(300)
            closeScreen.invoke()
        }
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.AddToken_Title),
                navigationIcon = {
                    HsBackButton(onClick = closeScreen)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Add),
                        onClick = viewModel::onAddClick,
                        enabled = uiState.addButtonEnabled
                    )
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                CellUniversalLawrenceSection(
                    listOf {
                        RowUniversal(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = { navController.navigate(BlockchainSelectorPage) }
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_blocks_24),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            body_leah(
                                text = stringResource(R.string.AddToken_Blockchain),
                                modifier = Modifier.weight(1f)
                            )
                            subhead1_grey(
                                text = viewModel.selectedBlockchain.name,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.grey
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    enabled = false,
                    hint = stringResource(R.string.AddToken_AddressOrSymbol),
                    state = getState(uiState.caution, uiState.loading),
                    qrScannerEnabled = true,
                ) {
                    viewModel.onEnterText(it)
                }

                Spacer(modifier = Modifier.height(32.dp))

                uiState.tokenInfo?.let { tokenInfo ->
                    CellUniversalLawrenceSection(
                        listOf(
                            {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_CoinName),
                                    tokenInfo.token.coin.name
                                )
                            }, {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_CoinCode),
                                    tokenInfo.token.coin.code
                                )
                            }, {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_Decimals),
                                    tokenInfo.token.decimals.toString()
                                )
                            }
                        )
                    )
                }
            }
        }
    }
}

private fun getState(caution: Caution?, loading: Boolean) = when (caution?.type) {
    Caution.Type.Error -> DataState.Error(Exception(caution.text))
    Caution.Type.Warning -> DataState.Error(FormsInputStateWarning(caution.text))
    null -> if (loading) DataState.Loading else null
}
