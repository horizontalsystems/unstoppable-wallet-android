package io.horizontalsystems.bankwallet.modules.evmnetwork

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BlockchainSettingCell
import io.horizontalsystems.bankwallet.modules.evmnetwork.addrpc.AddRpcScreen
import io.horizontalsystems.bankwallet.modules.info.EvmNetworkInfoScreen
import io.horizontalsystems.bankwallet.modules.settings.about.*
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.ActionsRow
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.getShape
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.showDivider
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class EvmNetworkFragment : BaseFragment() {

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
                    EvmNetworkNavHost(
                        requireArguments(),
                        findNavController()
                    )
                }
            }
        }
    }

}

private const val EvmNetworkPage = "evm_network"
private const val EvmNetworkInfoPage = "evm_network_info"
private const val AddRpcPage = "add_rpc"

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EvmNetworkNavHost(
    arguments: Bundle,
    fragmentNavController: NavController
) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = EvmNetworkPage,
    ) {
        composable(EvmNetworkPage) {
            EvmNetworkScreen(
                arguments = arguments,
                navController = navController,
                onBackPress = { fragmentNavController.popBackStack() }
            )
        }
        composablePopup(AddRpcPage) { AddRpcScreen(navController, arguments) }
        composablePopup(EvmNetworkInfoPage) { EvmNetworkInfoScreen(navController) }
    }
}

@Composable
private fun EvmNetworkScreen(
    arguments: Bundle,
    navController: NavController,
    onBackPress: () -> Unit,
    viewModel: EvmNetworkViewModel = viewModel(factory = EvmNetworkModule.Factory(arguments))
) {

    var revealedCardId by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.PlainString(viewModel.title),
                navigationIcon = {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = viewModel.blockchain.type.imageUrl,
                            error = painterResource(R.drawable.ic_platform_placeholder_32)
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .size(24.dp)
                    )
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            onBackPress.invoke()
                        }
                    )
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {

                item {
                    HeaderText(stringResource(R.string.AddEvmSyncSource_RpcSource)) {
                        navController.navigate(EvmNetworkInfoPage)
                    }
                }

                item {
                    CellUniversalLawrenceSection(viewModel.viewState.defaultItems) { item ->
                        BlockchainSettingCell(item.name, item.url, item.selected) {
                            viewModel.onSelectSyncSource(item.syncSource)
                        }
                    }
                }

                if (viewModel.viewState.customItems.isNotEmpty()) {
                    CustomRpcListSection(
                        viewModel.viewState.customItems,
                        revealedCardId,
                        onClick = { syncSource ->
                            viewModel.onSelectSyncSource(syncSource)
                        },
                        onReveal = { id ->
                            if (revealedCardId != id) {
                                revealedCardId = id
                            }
                        },
                        onConceal = {
                            revealedCardId = null
                        }
                    ) {
                        viewModel.onRemoveCustomRpc(it)
                        HudHelper.showErrorMessage(view, R.string.Hud_Removed)
                    }
                }

                item {
                    Spacer(Modifier.height(32.dp))
                    AddButton {
                        navController.navigate(AddRpcPage)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.CustomRpcListSection(
    items: List<EvmNetworkViewModel.ViewItem>,
    revealedCardId: String?,
    onClick: (EvmSyncSource) -> Unit,
    onReveal: (String) -> Unit,
    onConceal: () -> Unit,
    onDelete: (EvmSyncSource) -> Unit
) {
    item {
        Spacer(Modifier.height(32.dp))
        HeaderText(
            stringResource(R.string.EvmNetwork_Added),
        )
    }
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        val showDivider = showDivider(items.size, index)
        val shape = getShape(items.size, index)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ActionsRow(
                content = {
                    HsIconButton(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp),
                        onClick = { onDelete(item.syncSource) },
                        content = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_circle_minus_24),
                                tint = Color.Gray,
                                contentDescription = "delete",
                            )
                        }
                    )
                },
            )
            DraggableCardSimple(
                isRevealed = revealedCardId == item.id,
                cardOffset = 72f,
                onReveal = { onReveal(item.id) },
                onConceal = onConceal,
                content = {
                    RpcCell(
                        shape = shape,
                        showDivider = showDivider,
                        item = item,
                        onItemClick = onClick
                    )
                }
            )
        }
    }
}

@Composable
private fun AddButton(
    onClick: () -> Unit
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                onClick = onClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    modifier = Modifier.size(24.dp),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null
                )
                Spacer(Modifier.width(16.dp))
                body_jacob(
                    text = stringResource(R.string.EvmNetwork_AddNew)
                )
            }
        }
    )
}

@Composable
fun RpcCell(
    shape: Shape,
    showDivider: Boolean = false,
    item: EvmNetworkViewModel.ViewItem,
    onItemClick: (EvmSyncSource) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(ComposeAppTheme.colors.lawrence)
            .clickable {
                onItemClick.invoke(item.syncSource)
            },
        contentAlignment = Alignment.Center
    ) {
        if (showDivider) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val title = when {
                    item.name.isNotBlank() -> item.name
                    else -> stringResource(id = R.string.WalletConnect_Unnamed)
                }

                body_leah(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subhead2_grey(text = item.url)
            }
            if (item.selected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null
                )
            }
        }
    }
}
