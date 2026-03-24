package io.horizontalsystems.bankwallet.modules.walletconnect.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.navigation3.runtime.NavBackStack
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_remus
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.AlertCard
import io.horizontalsystems.bankwallet.uiv3.components.AlertFormat
import io.horizontalsystems.bankwallet.uiv3.components.AlertType
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
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType

class WCSessionBottomSheet(val input: WCSessionModule.Input) : BaseComposableBottomSheetFragment() {

    private val viewModel by viewModels<WCSessionViewModel> {
        WCSessionModule.Factory(input.sessionTopic)
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
    navController: NavBackStack<HSScreen>,
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
            navController.removeLastOrNull()
        }
    }

    BottomSheetContent(
        onDismissRequest = navController::removeLastOrNull,
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

            SectionHeader(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.WalletConnect_ScamProtection),
                icon = R.drawable.ic_defense_shield_20
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence)
                    .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .clickable(enabled = !uiState.scamProtectionActionAllowed) {
                        navController.slideFromBottom(
                            R.id.defenseSystemFeatureDialog,
                            DefenseSystemFeatureDialog.Input(PremiumFeature.ScamProtectionFeature)
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CellMiddleInfo(title = stringResource(R.string.WalletConnect_DAppCheck).hs)
                }
                Box(
                    modifier = Modifier.widthIn(max = 200.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (!uiState.scamProtectionActionAllowed) {
                        Icon(
                            painter = painterResource(R.drawable.lock_24),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.grey,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        when (uiState.whiteListState) {
                            WCWhiteListState.InWhiteList ->
                                subheadSB_remus(text = stringResource(R.string.WalletConnect_DAppCheck_Secure))
                            WCWhiteListState.NotInWhiteList ->
                                subheadSB_lucian(text = stringResource(R.string.WalletConnect_DAppCheck_Risky))
                            else -> {}
                        }
                    }
                }
            }

            VSpacer(16.dp)

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
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

            if (uiState.scamProtectionActionAllowed &&
                uiState.whiteListState == WCWhiteListState.NotInWhiteList
            ) {
                VSpacer(16.dp)
                AlertCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    format = AlertFormat.Structured,
                    type = AlertType.Critical,
                    titleCustom = stringResource(R.string.WalletConnect_Danger),
                    text = stringResource(R.string.WalletConnect_DefenseMessage_Danger),
                )
            }

            VSpacer(16.dp)
        }
        ActionButtons(
            buttonsStates = buttonsStates,
            onConnectClick = { viewModel.connect() },
            onDisconnectClick = { viewModel.disconnect() },
            onCancelClick = {
                viewModel.rejectProposal()
                navController.removeLastOrNull()
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
    var connectButtonEnabled by remember { mutableStateOf(true) }
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
                    enabled = connectButtonEnabled,
                    onClick = {
                        connectButtonEnabled = false
                        onConnectClick()
                    }
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

