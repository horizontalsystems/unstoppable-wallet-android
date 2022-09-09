package io.horizontalsystems.bankwallet.modules.market.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.icon24
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Blockchain

class BlockchainsSelectorFragment : BaseFragment() {

    private val viewModel by navGraphViewModels<MarketFiltersViewModel>(R.id.marketAdvancedSearchFragment) {
        MarketFiltersModule.Factory()
    }

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
                FilterByBlockchainsScreen(
                    viewModel,
                    findNavController(),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.updateListBySelectedBlockchains()
                    findNavController().popBackStack()
                }
            })
    }

}

@Composable
private fun FilterByBlockchainsScreen(
    viewModel: MarketFiltersViewModel,
    navController: NavController
) {
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.Market_Filter_Blockchains),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                HSSectionRounded {
                    AnyCell(
                        checked = viewModel.selectedBlockchains.isEmpty(),
                        onClick = { viewModel.anyBlockchains() }
                    )
                    viewModel.blockchainOptions.forEach { item ->
                        BlockchainCell(
                            item = item,
                            onCheck = { viewModel.onBlockchainCheck(it) },
                            onUncheck = { viewModel.onBlockchainUncheck(it) },
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BlockchainCell(
    item: MarketFiltersModule.BlockchainViewItem,
    onCheck: (Blockchain) -> Unit,
    onUncheck: (Blockchain) -> Unit,
) {
    CellSingleLineLawrence(borderTop = true) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    if (item.checked) {
                        onUncheck(item.blockchain)
                    } else {
                        onCheck(item.blockchain)
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(item.blockchain.type.icon24),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(24.dp)
            )
            body_leah(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = item.blockchain.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (item.checked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkmark_20),
                        tint = ComposeAppTheme.colors.jacob,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnyCell(
    checked: Boolean,
    onClick: () -> Unit
) {
    CellSingleLineLawrence(borderTop = true) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            body_grey(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.Any),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkmark_20),
                        tint = ComposeAppTheme.colors.jacob,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
