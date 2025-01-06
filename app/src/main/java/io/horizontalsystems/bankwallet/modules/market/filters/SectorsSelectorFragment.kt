package io.horizontalsystems.bankwallet.modules.market.filters

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.findNavController

class SectorsSelectorFragment : BaseComposeFragment() {

    private val viewModel by navGraphViewModels<MarketFiltersViewModel>(R.id.marketAdvancedSearchFragment) {
        MarketFiltersModule.Factory()
    }

    @Composable
    override fun GetContent(navController: NavController) {
        SectorsSelectorScreen(
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
                    findNavController().popBackStack()
                }
            })
    }

}

@Composable
fun SectorsSelectorScreen(
    viewModel: MarketFiltersViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    var selectedItems by remember { mutableStateOf(uiState.sectors) }
    val sectorItems = viewModel.sectorsViewItemOptions

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
            modifier = Modifier.padding(it)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)
                CellUniversalLawrenceSection(
                    items = sectorItems,
                    showFrame = true
                ) { itemWrapper ->
                    RowUniversal(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalPadding = 0.dp,
                        onClick = {
                            selectedItems =
                                if (selectedItems.contains(itemWrapper) && itemWrapper.item == null) {
                                    listOf(itemWrapper)  //no action when `Any` is already selected and pressed
                                } else if (!selectedItems.contains(itemWrapper) && itemWrapper.item == null) {
                                    listOf(itemWrapper) //on option  `Any` select, reset other selected items
                                } else if (!selectedItems.contains(itemWrapper) && selectedItems.size == 1 && selectedItems[0].item == null) {
                                    listOf(itemWrapper) //on option select, reset `Any` option
                                } else if (selectedItems.contains(itemWrapper) && selectedItems.size == 1) {
                                    listOf(sectorItems[0]) // return `Any` option when last selected item is unselected
                                } else if (selectedItems.contains(itemWrapper)) {
                                    selectedItems - itemWrapper
                                } else {
                                    selectedItems + itemWrapper
                                }
                        }
                    ) {
                        if (itemWrapper.title != null) {
                            body_leah(
                                text = itemWrapper.title,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            body_grey(
                                text = stringResource(R.string.Any),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        if (itemWrapper in selectedItems) {
                            Image(
                                modifier = Modifier.padding(start = 5.dp),
                                painter = painterResource(id = R.drawable.ic_checkmark_20),
                                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                                contentDescription = null
                            )
                        }
                    }
                }
                VSpacer(24.dp)
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Apply),
                    onClick = {
                        viewModel.setSectors(selectedItems)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
