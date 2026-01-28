package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInputX
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionButtonStates
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCSessionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCWhiteListState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseAlertLevel
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType

class WCRequestBottomSheet : BaseComposableBottomSheetFragment() {

    private val viewModel by viewModels<WCSessionViewModel> {
        val input = arguments?.getInputX<WCSessionModule.Input>()
        WCSessionModule.Factory(input?.sessionTopic)
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
                val navController = findNavController()

                ComposeAppTheme {
                    WCRequestScreen(navController, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCRequestScreen(
    navController: NavController,
    viewModel: WCSessionViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState = viewModel.uiState
    val buttonsStates = uiState.buttonStates

    val connectionTitleRes =
        if (uiState.connected) R.string.WalletConnect_ConnectedTo else R.string.WalletConnect_ConnectTo
    val connectedDAppName = stringResource(connectionTitleRes, uiState.peerMeta?.name ?: "")

    BottomSheetContent(
        onDismissRequest = navController::popBackStack,
        sheetState = sheetState
    ) { snackbarActions ->
        uiState.showError?.let { snackbarActions.showErrorMessage(it) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .size(52.dp, 4.dp)
                    .background(ComposeAppTheme.colors.blade, RoundedCornerShape(50))
            ) { }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    painter = rememberAsyncImagePainter(
                        model = uiState.peerMeta?.icon,
                        error = painterResource(R.drawable.ic_platform_placeholder_24)
                    ),
                    contentDescription = null,
                )
            }
            VSpacer(16.dp)
            headline1_leah(
                text = connectedDAppName,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            VSpacer(8.dp)
            subhead_grey(
                text = uiState.peerMeta?.url?.let { TextHelper.getCleanedUrl(it) } ?: "",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            VSpacer(16.dp)

            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp)
            ) {
                WalletCell(uiState.peerMeta?.accountName ?: "")
                NetworkCell(
                    blockchainTypes = uiState.blockchainTypes
                )
            }

            TextBlock(
                text = stringResource(R.string.WalletConnect_ConnectWarning),
            )
            VSpacer(16.dp)
            WCDefenseSystemMessage(
                activated = uiState.hasSubscription,
                whiteListState = uiState.whiteListState,
                onActivateClick = {
                    navController.slideFromBottom(
                        R.id.defenseSystemFeatureDialog,
                        DefenseSystemFeatureDialog.Input(PremiumFeature.ScamProtectionFeature)
                    )
                }
            )
        }
        ActionButtons(
            buttonsStates = buttonsStates,
            onConnectClick = { viewModel.connect() },
            onDisconnectClick = { viewModel.disconnect() },
            onCancelClick = {
                viewModel.rejectProposal()
                navController.popBackStack()
            }
        )

        VSpacer(8.dp)
    }
}

@Composable
private fun ActionButtons(
    buttonsStates: WCSessionButtonStates?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    buttonsStates?.let { buttons ->
        ButtonsGroupHorizontal {
            if (buttons.cancel.visible) {
                HSButton(
                    title = stringResource(R.string.Button_Cancel),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    modifier = Modifier.weight(1f),
                    onClick = onCancelClick
                )
            }
            if (buttons.connect.visible) {
                HSButton(
                    title = stringResource(R.string.Button_Connect),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onConnectClick
                )
            }
            if (buttons.disconnect.visible || buttons.remove.visible) {
                HSButton(
                    title = stringResource(R.string.Button_Disconnect),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    modifier = Modifier.weight(1f),
                    onClick = onDisconnectClick
                )
            }
        }
    }
}

@Composable
fun WalletCell(
    name: String,
) {
    CellSecondary(
        middle = {
            CellMiddleInfo(
                eyebrow = stringResource(R.string.Wallet_Title).hs,
            )
        },
        right = {
            CellRightInfo(
                titleSubheadSb = name.hs,
            )
        },
    )
}

@Composable
fun NetworkCell(
    blockchainTypes: List<BlockchainType>?,
) {
    CellSecondary(
        middle = {
            CellMiddleInfo(
                title = stringResource(R.string.WalletConnect_Networks).hs,
            )
        },
        right = {
            IconsFromUrls(urls = blockchainTypes?.reversed()?.map { it.imageUrl } ?: emptyList())
        },
    )
}

@Composable
fun IconsFromUrls(
    urls: List<String>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        urls.forEachIndexed { index, url ->
            HsImage(
                url = url,
                modifier = Modifier
                    .offset(x = -(index * 12).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ComposeAppTheme.colors.white)
            )
        }
    }
}

@Composable
private fun WCDefenseSystemMessage(
    activated: Boolean,
    whiteListState: WCWhiteListState,
    onActivateClick: () -> Unit = {},
) {
    val level = when {
        !activated -> DefenseAlertLevel.WARNING
        whiteListState == WCWhiteListState.NotInWhiteList -> DefenseAlertLevel.DANGER
        whiteListState == WCWhiteListState.InWhiteList -> DefenseAlertLevel.SAFE
        whiteListState == WCWhiteListState.InProgress -> DefenseAlertLevel.IDLE
        else -> DefenseAlertLevel.IDLE
    }

    val title: Int = when (level) {
        DefenseAlertLevel.WARNING -> R.string.WalletConnect_Attention
        DefenseAlertLevel.DANGER -> R.string.WalletConnect_Danger
        DefenseAlertLevel.SAFE -> R.string.WalletConnect_Safe
        DefenseAlertLevel.IDLE -> R.string.WalletConnect_Checking
    }

    val content: Int? = when (level) {
        DefenseAlertLevel.WARNING -> R.string.WalletConnect_DefenseMessage_Warning
        DefenseAlertLevel.DANGER -> R.string.WalletConnect_DefenseMessage_Danger
        DefenseAlertLevel.SAFE -> R.string.WalletConnect_DefenseMessage_Safe
        DefenseAlertLevel.IDLE -> null
    }

    val icon = when (level) {
        DefenseAlertLevel.WARNING -> R.drawable.warning_filled_24
        DefenseAlertLevel.DANGER -> R.drawable.warning_filled_24
        DefenseAlertLevel.SAFE -> R.drawable.shield_check_filled_24
        DefenseAlertLevel.IDLE -> null
    }

    DefenseSystemMessage(
        level = level,
        title = stringResource(title),
        content = content?.let { stringResource(it) },
        icon = icon,
        actionText = if (level == DefenseAlertLevel.WARNING) stringResource(R.string.Button_Activate) else null,
        onClick = onActivateClick
    )
}