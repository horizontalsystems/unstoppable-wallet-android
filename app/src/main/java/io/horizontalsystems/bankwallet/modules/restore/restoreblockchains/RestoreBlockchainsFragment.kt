package io.horizontalsystems.bankwallet.modules.restore.restoreblockchains

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.ZcashBirthdayHeightDialog
import io.horizontalsystems.core.findNavController

class RestoreBlockchainsFragment : BaseFragment() {

    val vmFactory by lazy {
        RestoreBlockchainsModule.Factory(
            arguments?.getString(ACCOUNT_NAME_KEY)!!,
            arguments?.getParcelable(ACCOUNT_TYPE_KEY)!!
        )
    }

    private val viewModel by viewModels<RestoreBlockchainsViewModel> { vmFactory }
    private val coinSettingsViewModel by viewModels<CoinSettingsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }
    private val coinPlatformsViewModel by viewModels<CoinPlatformsViewModel> { vmFactory }

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
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observe()
    }

    private fun observe() {
        viewModel.successLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack(R.id.restoreMnemonicFragment, true)
        }

        coinSettingsViewModel.openBottomSelectorLiveEvent.observe(viewLifecycleOwner) { config ->
            showBottomSelectorDialog(
                config,
                onSelect = { indexes -> coinSettingsViewModel.onSelect(indexes) },
                onCancel = { coinSettingsViewModel.onCancelSelect() }
            )
        }

        restoreSettingsViewModel.openBirthdayAlertSignal.observe(viewLifecycleOwner) {
            val zcashBirthdayHeightDialog = ZcashBirthdayHeightDialog()
            zcashBirthdayHeightDialog.onEnter = {
                restoreSettingsViewModel.onEnter(it)
            }
            zcashBirthdayHeightDialog.onCancel = {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }

            zcashBirthdayHeightDialog.show(
                requireActivity().supportFragmentManager,
                "ZcashBirthdayHeightDialog"
            )
        }

        coinPlatformsViewModel.openPlatformsSelectorEvent.observe(viewLifecycleOwner) { config ->
            showBottomSelectorDialog(
                config,
                onSelect = { indexes -> coinPlatformsViewModel.onSelect(indexes) },
                onCancel = { coinPlatformsViewModel.onCancelSelect() }
            )
        }
    }

    private fun showBottomSelectorDialog(
        config: BottomSheetSelectorMultipleDialog.Config,
        onSelect: (indexes: List<Int>) -> Unit,
        onCancel: () -> Unit
    ) {
        BottomSheetSelectorMultipleDialog.show(
            fragmentManager = childFragmentManager,
            title = config.title,
            subtitle = config.subtitle,
            icon = config.icon,
            items = config.viewItems,
            selected = config.selectedIndexes,
            notifyUnchanged = true,
            onItemSelected = { onSelect(it) },
            onCancelled = { onCancel() },
            warning = config.description
        )
    }

    companion object {
        const val ACCOUNT_NAME_KEY = "account_name_key"
        const val ACCOUNT_TYPE_KEY = "account_type_key"
    }
}

@Composable
private fun ManageWalletsScreen(
    navController: NavController,
    viewModel: RestoreBlockchainsViewModel
) {
    val coinItems by viewModel.viewItemsLiveData.observeAsState()
    val doneButtonEnabled by viewModel.restoreEnabledLiveData.observeAsState(false)

    Column(
        modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            title = TranslatableString.ResString(R.string.Restore_Title),
            navigationIcon = {
                HsIconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "back",
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Restore),
                    onClick = { viewModel.onRestore() },
                    enabled = doneButtonEnabled
                )
            ),
        )

        LazyColumn {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
                )
            }
            coinItems?.let {
                items(it) { viewItem ->
                    CellMultilineClear(
                        borderBottom = true,
                        onClick = { onItemClick(viewItem, viewModel) }
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = viewItem.title,
                                    color = ComposeAppTheme.colors.leah,
                                    style = ComposeAppTheme.typography.body,
                                    maxLines = 1,
                                )
                                Text(
                                    text = viewItem.subtitle,
                                    color = ComposeAppTheme.colors.grey,
                                    style = ComposeAppTheme.typography.subhead2,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                            if (viewItem.state is CoinViewItemState.ToggleVisible) {
                                Spacer(Modifier.width(12.dp))
                                if (viewItem.state.hasSettings) {
                                    HsIconButton(
                                        onClick = { viewModel.onClickSettings(viewItem.uid) }
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
                                    onCheckedChange = { onItemClick(viewItem, viewModel) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun onItemClick(viewItem: CoinViewItem, viewModel: RestoreBlockchainsViewModel) {
    if (viewItem.state is CoinViewItemState.ToggleVisible) {
        if (viewItem.state.enabled) {
            viewModel.disable(viewItem.uid)
        } else {
            viewModel.enable(viewItem.uid)
        }
    }
}