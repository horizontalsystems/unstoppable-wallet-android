package io.horizontalsystems.walletkit.modules.walletconnect.request

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline1_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead_grey
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.info.TextBlock
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private val logger = AppLogger("wallet-connect request")

@Serializable
data object WCRequestSheet : HSBottomSheet() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val wcRequestRouterViewModel =
            viewModel<WCRequestRouterViewModel>(factory = WCRequestRouterViewModel.Factory())

        val uiState = wcRequestRouterViewModel.uiState

        val blockchainType = uiState.blockchainType

        if (blockchainType == null) {
            WcRequestError { navigation.removeLastOrNull() }
        } else if (ChainRegistry[blockchainType]?.WcRequestSheetScreen(navigation) == true) {
            // rendered by the chain plugin
        } else if (blockchainType is BlockchainType.Stellar || blockchainType is BlockchainType.Solana) {
            WcRequestPreScreen(navigation)
        } else {
            WcRequestError { navigation.removeLastOrNull() }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WcRequestError(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss = {
        // Discard synchronously, then animate the sheet out before popping (see WCNewSignRequestScreen).
        WCDelegate.discardActiveSessionRequest()
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
        Unit
    }
    BottomSheetContent(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Close),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
                onClick = dismiss
            )
        },
    ) {
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
            VSpacer(16.dp)
            Icon(
                modifier = Modifier.size(60.dp),
                painter = painterResource(R.drawable.ic_warning_filled_24),
                contentDescription = null,
                tint = ComposeAppTheme.colors.lucian
            )
            VSpacer(8.dp)
            VSpacer(16.dp)
            headline1_leah(
                text = stringResource(R.string.WalletConnect_RequestFailed),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            VSpacer(8.dp)
            TextBlock(
                text = stringResource(R.string.WalletConnect_RequestFailedDescription),
                textAlign = TextAlign.Center,
            )
            VSpacer(16.dp)
        }
    }
}




