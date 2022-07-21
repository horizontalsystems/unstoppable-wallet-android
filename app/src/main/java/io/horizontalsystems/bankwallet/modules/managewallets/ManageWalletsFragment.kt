package io.horizontalsystems.bankwallet.modules.managewallets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.CoinViewItemState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.core.findNavController

class ManageWalletsFragment : BaseFragment() {

    private val vmFactory by lazy { ManageWalletsModule.Factory() }
    private val viewModel by viewModels<ManageWalletsViewModel> { vmFactory }
    private val coinSettingsViewModel by viewModels<CoinSettingsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }
    private val coinTokensViewModel by viewModels<CoinTokensViewModel> { vmFactory }

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
                ComposeAppTheme {
                    ManageWalletsScreen(
                        findNavController(),
                        viewModel
                    )
                    ZCashBirthdayHeightDialogWrapper(restoreSettingsViewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observe()
    }

    private fun observe() {
        coinSettingsViewModel.openBottomSelectorLiveEvent.observe(viewLifecycleOwner) { config ->
            hideKeyboard()
            showBottomSelectorDialog(
                config,
                onSelect = { indexes -> coinSettingsViewModel.onSelect(indexes) },
                onCancel = { coinSettingsViewModel.onCancelSelect() }
            )
        }

        coinTokensViewModel.openSelectorEvent.observe(viewLifecycleOwner) { config ->
            showBottomSelectorDialog(
                config,
                onSelect = { indexes -> coinTokensViewModel.onSelect(indexes) },
                onCancel = { coinTokensViewModel.onCancelSelect() }
            )
        }
    }

    private fun showBottomSelectorDialog(
        config: BottomSheetSelectorMultipleDialog.Config,
        onSelect: (indexes: List<Int>) -> Unit,
        onCancel: () -> Unit
    ) {
        hideKeyboard()
        BottomSheetSelectorMultipleDialog.show(
            fragmentManager = childFragmentManager,
            title = config.title,
            icon = config.icon,
            items = config.viewItems,
            selected = config.selectedIndexes,
            notifyUnchanged = true,
            onItemSelected = { onSelect(it) },
            onCancelled = { onCancel() },
            warningTitle = config.descriptionTitle,
            warning = config.description,
            allowEmpty= config.allowEmpty
        )
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ManageWalletsScreen(
    findNavController: NavController,
    viewModel: ManageWalletsViewModel
) {
    val coinItems by viewModel.viewItemsLiveData.observeAsState()

    Column(
        modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
    ) {
        SearchBar(
            title = stringResource(R.string.ManageCoins_title),
            searchHintText = stringResource(R.string.ManageCoins_Search),
            navController = findNavController,
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.ManageCoins_AddToken),
                    icon = R.drawable.ic_add_yellow,
                    onClick = {
                        findNavController.slideFromRight(R.id.addTokenFragment)
                    }
                )
            ),
            onSearchTextChanged = { text ->
                viewModel.updateFilter(text)
            }
        )

        coinItems?.let {
            if (it.isEmpty()) {
                ListEmptyView(
                    text = stringResource(R.string.ManageCoins_NoResults),
                    icon = R.drawable.ic_not_found
                )
            } else {
                LazyColumn {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                        )
                    }
                    items(it) { viewItem ->
                        CoinCell(
                            viewItem = viewItem,
                            onItemClick = { onItemClick(viewItem, viewModel) },
                            onSettingClick = { viewModel.onClickSettings(viewItem.item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinCell(
    viewItem: CoinViewItem<String>,
    onItemClick: () -> Unit,
    onSettingClick: () -> Unit,
) {
    CellMultilineClear(
        borderBottom = true,
        onClick = onItemClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Image(
                painter = viewItem.imageSource.painter(),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    body_leah(
                        text = viewItem.title,
                        maxLines = 1,
                    )
                    viewItem.label?.let { labelText ->
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ComposeAppTheme.colors.jeremy)
                        ) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    end = 4.dp,
                                    bottom = 1.dp
                                ),
                                text = labelText,
                                color = ComposeAppTheme.colors.bran,
                                style = ComposeAppTheme.typography.microSB,
                                maxLines = 1,
                            )
                        }
                    }
                }
                subhead2_grey(
                    text = viewItem.subtitle,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            if (viewItem.state is CoinViewItemState.ToggleVisible) {
                Spacer(Modifier.width(12.dp))
                if (viewItem.state.hasSettings) {
                    HsIconButton(
                        onClick = onSettingClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_20),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.grey
                        )
                    }
                }
                HsSwitch(
                    checked = viewItem.state.enabled,
                    onCheckedChange = { onItemClick.invoke() },
                )
            }
        }
    }
}

private fun onItemClick(viewItem: CoinViewItem<String>, viewModel: ManageWalletsViewModel) {
    if (viewItem.state is CoinViewItemState.ToggleVisible) {
        if (viewItem.state.enabled) {
            viewModel.disable(viewItem.item)
        } else {
            viewModel.enable(viewItem.item)
        }
    }
}

@Preview
@Composable
fun PreviewCoinCell() {
    val viewItem = CoinViewItem(
        "arbitrum",
        ImageSource.Local(R.drawable.logo_arbitrum_24),
        "ARB",
        "Arbitrum",
        CoinViewItemState.ToggleVisible(true, true),
        "Arbitrum"
    )
    ComposeAppTheme {
        CoinCell(
            viewItem,
            {},
            {},
        )
    }
}
