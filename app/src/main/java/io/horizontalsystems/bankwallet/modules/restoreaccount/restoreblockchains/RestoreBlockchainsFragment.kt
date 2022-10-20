package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.ZCashConfig
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.zcashconfigure.ZcashConfigure
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.delay

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
                    val popUpToInclusiveId = arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, -1) ?: -1
                    ManageWalletsScreen(
                        findNavController(),
                        viewModel,
                        restoreSettingsViewModel,
                        popUpToInclusiveId
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observe()
    }

    private fun observe() {
        coinSettingsViewModel.openBottomSelectorLiveEvent.observe(viewLifecycleOwner) { config ->
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
        BottomSheetSelectorMultipleDialog.show(
            fragmentManager = childFragmentManager,
            title = config.title,
            icon = config.icon,
            items = config.viewItems,
            selected = config.selectedIndexes,
            onItemSelected = { onSelect(it) },
            onCancelled = { onCancel() },
            warningTitle = config.descriptionTitle,
            warning = config.description,
            notifyUnchanged = true,
            allowEmpty = config.allowEmpty
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
    viewModel: RestoreBlockchainsViewModel,
    restoreSettingsViewModel: RestoreSettingsViewModel,
    popUpToInclusiveId: Int
) {
    val view = LocalView.current

    val coinItems by viewModel.viewItemsLiveData.observeAsState()
    val doneButtonEnabled by viewModel.restoreEnabledLiveData.observeAsState(false)
    val restored = viewModel.restored

    if (restoreSettingsViewModel.openZcashConfigure != null) {
        restoreSettingsViewModel.zcashConfigureOpened()

        navController.getNavigationResult(ZcashConfigure.resultBundleKey) { bundle ->
            val requestResult = bundle.getInt(ZcashConfigure.requestResultKey)

            if (requestResult == ZcashConfigure.RESULT_OK) {
                val zcashConfig = bundle.getParcelable<ZCashConfig>(ZcashConfigure.zcashConfigKey)
                zcashConfig?.let { config ->
                    restoreSettingsViewModel.onEnter(config)
                }
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }

        navController.slideFromBottom(R.id.zcashConfigure)
    }

    LaunchedEffect(restored) {
        if (restored) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Restored,
                icon = R.drawable.icon_add_to_wallet_2_24,
                iconTint = R.color.white
            )
            delay(300)
            if (popUpToInclusiveId != -1) {
                navController.popBackStack(popUpToInclusiveId, true)
            } else {
                navController.popBackStack()
            }
        }
    }

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
                                body_leah(
                                    text = viewItem.title,
                                    maxLines = 1,
                                )
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
                                        onClick = { viewModel.onClickSettings(viewItem.item) }
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

private fun onItemClick(viewItem: CoinViewItem<Blockchain>, viewModel: RestoreBlockchainsViewModel) {
    if (viewItem.state is CoinViewItemState.ToggleVisible) {
        if (viewItem.state.enabled) {
            viewModel.disable(viewItem.item)
        } else {
            viewModel.enable(viewItem.item)
        }
    }
}