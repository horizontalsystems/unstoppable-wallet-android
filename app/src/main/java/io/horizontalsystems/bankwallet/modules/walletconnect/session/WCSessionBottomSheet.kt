package io.horizontalsystems.bankwallet.modules.walletconnect.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
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
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType

class WCSessionBottomSheet : BaseComposableBottomSheetFragment() {

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
                    WCSessionScreen(navController, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCSessionScreen(
    navController: NavController,
    viewModel: WCSessionViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState = viewModel.uiState
    val buttonsStates = uiState.buttonStates

    val connectionTitleRes =
        if (uiState.connected) R.string.WalletConnect_ConnectedTo else R.string.WalletConnect_ConnectTo
    val connectedDAppName = stringResource(connectionTitleRes, uiState.peerMeta?.name ?: "")

    LaunchedEffect(uiState.closeDialog) {
        if (uiState.closeDialog) {
            navController.popBackStack()
        }
    }

    BottomSheetContent(
        onDismissRequest = navController::popBackStack,
        sheetState = sheetState,
    ) { snackbarActions ->
        uiState.showError?.let {
            snackbarActions.showErrorMessage(it)
            viewModel.errorShown()
        }
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
            DefenseSystemMessage(
                activated = uiState.hasSubscription,
                whiteListState = uiState.whiteListState,
                onActivateClick = {
                    navController.slideFromBottom(
                        R.id.defenseSystemFeatureDialog,
                        DefenseSystemFeatureDialog.Input(PremiumFeature.ScamProtectionFeature, true)
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
fun TitleValueCell(
    title: String,
    value: String,
    secondaryValue: String? = null,
) {
    CellSecondary(
        middle = {
            CellMiddleInfo(
                subtitle = title.hs,
            )
        },
        right = {
            CellRightInfo(
                title = value.hs,
                subtitle = secondaryValue?.hs
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
                subtitle = stringResource(R.string.WalletConnect_Networks).hs,
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
fun DefenseSystemMessage(
    activated: Boolean,
    whiteListState: WCWhiteListState,
    onActivateClick: () -> Unit = {},
) {
    val state = when {
        !activated -> DefenseSystemState.WARNING
        whiteListState == WCWhiteListState.NotInWhiteList -> DefenseSystemState.DANGER
        whiteListState == WCWhiteListState.InWhiteList -> DefenseSystemState.SAFE
        whiteListState == WCWhiteListState.InProgress -> DefenseSystemState.CHECKING
        else -> DefenseSystemState.CHECKING
    }

    val clickableModifier = when (state) {
        DefenseSystemState.WARNING -> Modifier.clickable(
            onClick = { onActivateClick.invoke() },
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        )

        else -> Modifier
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Message bubble
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(state.bubbleColor)
                .then(clickableModifier)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column {
                if (state == DefenseSystemState.CHECKING) {
                    headline2_leah(
                        text = stringResource(R.string.WalletConnect_Checking)
                    )
                    VSpacer(30.dp)
                } else {
                    val titleIcon: Int? = when (state) {
                        DefenseSystemState.WARNING -> R.drawable.warning_filled_24
                        DefenseSystemState.DANGER -> R.drawable.warning_filled_24
                        DefenseSystemState.SAFE -> R.drawable.shield_check_filled_24
                        DefenseSystemState.CHECKING -> null
                    }
                    val title: Int? = when (state) {
                        DefenseSystemState.WARNING -> R.string.WalletConnect_Attention
                        DefenseSystemState.DANGER -> R.string.WalletConnect_Danger
                        DefenseSystemState.SAFE -> R.string.WalletConnect_Safe
                        DefenseSystemState.CHECKING -> null
                    }
                    if (titleIcon != null && title != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(titleIcon),
                                contentDescription = null,
                                tint = when (state) {
                                    DefenseSystemState.SAFE,
                                    DefenseSystemState.WARNING -> Color.Black

                                    DefenseSystemState.DANGER -> Color.White
                                    else -> Color.Black
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            HSpacer(8.dp)
                            Text(
                                text = stringResource(title),
                                style = ComposeAppTheme.typography.headline2,
                                color = when (state) {
                                    DefenseSystemState.WARNING,
                                    DefenseSystemState.SAFE -> Color.Black

                                    DefenseSystemState.DANGER -> Color.White
                                    else -> Color.Black
                                }
                            )
                        }
                    }
                    // Message Text
                    Text(
                        text = when (state) {
                            DefenseSystemState.WARNING -> stringResource(R.string.WalletConnect_DefenseMessage_Warning)
                            DefenseSystemState.DANGER -> stringResource(R.string.WalletConnect_DefenseMessage_Danger)
                            DefenseSystemState.SAFE -> stringResource(R.string.WalletConnect_DefenseMessage_Safe)
                            DefenseSystemState.CHECKING -> ""
                        },
                        style = ComposeAppTheme.typography.subheadR,
                        color = when (state) {
                            DefenseSystemState.WARNING,
                            DefenseSystemState.SAFE,
                            DefenseSystemState.CHECKING -> Color.Black

                            DefenseSystemState.DANGER -> Color.White
                        }
                    )

                    if (state == DefenseSystemState.WARNING) {
                        VSpacer(12.dp)
                        Row(modifier = Modifier.align(Alignment.End)) {
                            Text(
                                text = "Activate",
                                style = ComposeAppTheme.typography.subheadSB,
                                color = Color.Black
                            )
                            HSpacer(8.dp)
                            Icon(
                                painter = painterResource(R.drawable.arrow_m_right_24),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        // Speech bubble tail
        Box(
            modifier = Modifier
                .offset(x = 48.dp, y = (-8).dp)
                .size(16.dp)
                .rotate(45f)
                .background(state.bubbleColor)
        )
    }

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_defense_shield_20),
            contentDescription = null,
        )
        Box(modifier = Modifier.weight(1f)) {
            CellMiddleInfo(
                subtitle = stringResource(R.string.Premium_DefenseSystem).hs
            )
        }
    }
}

enum class DefenseSystemState {
    WARNING,
    CHECKING,
    DANGER,
    SAFE;

    val bubbleColor: Color
        @Composable
        get() {
            return when (this) {
                WARNING -> ComposeAppTheme.colors.yellowD
                CHECKING -> ComposeAppTheme.colors.andy
                DANGER -> ComposeAppTheme.colors.redL
                SAFE -> ComposeAppTheme.colors.greenD
            }
        }
}