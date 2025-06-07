package cash.p.terminal.modules.managewallets

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromBottomForResult
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import cash.p.terminal.modules.restoreaccount.restoreblockchains.CoinViewItem
import cash.p.terminal.modules.zcashconfigure.ZcashConfigure
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.SearchBar
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefaults
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper

class ManageWalletsFragment : BaseComposeFragment() {

    private val vmFactory by lazy { ManageWalletsModule.Factory() }
    private val viewModel by viewModels<ManageWalletsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }

    @Composable
    override fun GetContent(navController: NavController) {
        ManageWalletsScreen(
            navController = navController,
            viewModel = viewModel,
            onBackPressed = {
                if (viewModel.showScanToAddButton) {
                    viewModel.requestScanToAddTokens()
                } else {
                    navController.popBackStack()
                }
            },
            requestScan = viewModel::requestScanToAddTokens,
            restoreSettingsViewModel = restoreSettingsViewModel
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle back press for hardware wallet
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.showScanToAddButton) {
                        viewModel.requestScanToAddTokens()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed() // Trigger the back press again, which will now be handled by other callbacks or the default system behavior.
                    }
                }
            })
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ManageWalletsScreen(
    navController: NavController,
    viewModel: ManageWalletsViewModel,
    onBackPressed: () -> Unit,
    requestScan: () -> Unit,
    restoreSettingsViewModel: RestoreSettingsViewModel
) {
    val coinItems by viewModel.viewItemsLiveData.observeAsState()
    val context = LocalView.current

    if (restoreSettingsViewModel.openZcashConfigure != null) {
        restoreSettingsViewModel.zcashConfigureOpened()

        navController.slideFromBottomForResult<ZcashConfigure.Result>(R.id.zcashConfigure) {
            if (it.config != null) {
                restoreSettingsViewModel.onEnter(it.config)
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            SearchBar(
                title = stringResource(R.string.ManageCoins_title),
                searchHintText = stringResource(R.string.ManageCoins_Search),
                menuItems = if (viewModel.addTokenEnabled) {
                    listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.ManageCoins_AddToken),
                            icon = R.drawable.ic_add_yellow,
                            onClick = {
                                navController.slideFromRight(R.id.addTokenFragment)
                            }
                        ))
                } else {
                    listOf()
                },
                onClose = onBackPressed,
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
                                onItemClick = {
                                    if (viewItem.enabled) {
                                        viewModel.disable(viewItem.item)
                                    } else {
                                        viewModel.enable(viewItem.item)
                                    }
                                },
                                onInfoClick = {
                                    navController.slideFromBottom(
                                        R.id.configuredTokenInfo,
                                        viewItem.item
                                    )
                                }
                            )
                        }
                        item {
                            VSpacer(height = 32.dp)
                        }
                        if (viewModel.showScanToAddButton) {
                            item {
                                VSpacer(height = ButtonPrimaryDefaults.MinHeight + 32.dp)
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = viewModel.showScanToAddButton,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            ScanToAddBlock(requestScan)
        }
        LaunchedEffect(viewModel.errorMsg) {
            if(viewModel.errorMsg != null) {
                HudHelper.showErrorMessage(context, viewModel.errorMsg!!)
            }
        }
    }
}

@Composable
private fun ScanToAddBlock(requestScan: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        contentColor = ComposeAppTheme.colors.light,
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + with(LocalDensity.current) {
                        NavigationBarDefaults.windowInsets
                            .getBottom(LocalDensity.current)
                            .toDp()
                    },
                    top = 16.dp
                ),
            title = stringResource(R.string.scan_card_to_add),
            onClick = requestScan
        )
    }

}

@Composable
private fun CoinCell(
    viewItem: CoinViewItem<cash.p.terminal.wallet.Token>,
    onItemClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Column {
        RowUniversal(
            onClick = onItemClick,
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalPadding = 0.dp
        ) {
            Image(
                painter = viewItem.imageSource.painter(),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
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
            Spacer(Modifier.width(12.dp))
            if (viewItem.hasInfo) {
                HsIconButton(onClick = onInfoClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            }
            HsSwitch(
                modifier = Modifier.padding(0.dp),
                checked = viewItem.enabled,
                onCheckedChange = { onItemClick.invoke() },
            )
        }
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
    }
}

//@Preview
//@Composable
//fun PreviewCoinCell() {
//    val viewItem = CoinViewItem(
//        item = "ethereum",
//        imageSource = ImageSource.Local(R.drawable.logo_ethereum_24),
//        title = "ETH",
//        subtitle = "Ethereum",
//        enabled = true,
//        hasSettings = true,
//        hasInfo = true,
//        label = "Ethereum"
//    )
//    ComposeAppTheme {
//        CoinCell(
//            viewItem,
//            {},
//            {},
//            {}
//        )
//    }
//}
