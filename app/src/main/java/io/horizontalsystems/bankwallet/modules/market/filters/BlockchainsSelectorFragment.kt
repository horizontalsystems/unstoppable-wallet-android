package io.horizontalsystems.bankwallet.modules.market.filters

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.Blockchain

class BlockchainsSelectorFragment : BaseComposeFragment() {

    private val viewModel by navGraphViewModels<MarketFiltersViewModel>(R.id.marketAdvancedSearchFragment) {
        MarketFiltersModule.Factory()
    }

    @Composable
    override fun GetContent(navController: NavController) {
        FilterByBlockchainsScreen(
            viewModel,
            navController,
        )
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
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Market_Filter_Blockchains),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(height = 12.dp)

            SectionUniversalLawrence {
                AnyCell(
                    checked = uiState.selectedBlockchains.isEmpty(),
                    onClick = { viewModel.anyBlockchains() }
                )
                uiState.blockchainOptions.forEach { item ->
                    BlockchainCell(
                        item = item,
                        onCheck = { viewModel.onBlockchainCheck(it) },
                        onUncheck = { viewModel.onBlockchainUncheck(it) },
                    )
                }
            }

            VSpacer(height = 32.dp)
        }
    }
}

@Composable
private fun BlockchainCell(
    item: MarketFiltersModule.BlockchainViewItem,
    onCheck: (Blockchain) -> Unit,
    onUncheck: (Blockchain) -> Unit,
) {
    CellUniversal(
        onClick = {
            if (item.checked) {
                onUncheck(item.blockchain)
            } else {
                onCheck(item.blockchain)
            }
        }
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = item.blockchain.type.imageUrl,
                error = painterResource(R.drawable.ic_platform_placeholder_32)
            ),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        body_leah(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            text = item.blockchain.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.checked) {
            Icon(
                painter = painterResource(R.drawable.ic_checkmark_20),
                tint = ComposeAppTheme.colors.jacob,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun AnyCell(
    checked: Boolean,
    onClick: () -> Unit
) {
    CellUniversal(
        borderTop = false,
        onClick = onClick
    ) {
        body_grey(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f),
            text = stringResource(R.string.Any),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.ic_checkmark_20),
            tint = ComposeAppTheme.colors.jacob,
            contentDescription = null,
            modifier = Modifier.alpha(if (checked) 1f else 0f)
        )
    }
}
