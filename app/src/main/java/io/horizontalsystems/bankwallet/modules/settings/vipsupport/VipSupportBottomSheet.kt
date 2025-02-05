package io.horizontalsystems.bankwallet.modules.settings.vipsupport

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipSupportBottomSheet(
    isBottomSheetVisible: Boolean,
    close: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = viewModel<VipSupportViewModel>(factory = VipSupportModule.Factory())
    val uiState = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(uiState.openTelegramGroup) {
        if (uiState.openTelegramGroup != null) {
            LinkHelper.openLinkInAppBrowser(context, uiState.openTelegramGroup)
            viewModel.telegramGroupOpened()
        }
    }
    LaunchedEffect(uiState.showError) {
        if (uiState.showError) {
            Toast.makeText(
                context,
                R.string.Settings_PersonalSupport_Requestfailed,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.errorShown()
        }
    }

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                close.invoke()
            },
            sheetState = modalBottomSheetState,
            containerColor = ComposeAppTheme.colors.transparent
        ) {
            VipSupportView(
                showSpinner = uiState.showSpinner,
                buttonEnabled = uiState.buttonEnabled,
                close = close,
                onRequestClicked = { viewModel.onRequestClicked() }
            )
        }
    }
}

@Composable
private fun VipSupportView(
    showSpinner: Boolean,
    buttonEnabled: Boolean,
    close: () -> Unit,
    onRequestClicked: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.prem_vip_support_24),
        title = stringResource(R.string.Settings_VipSupport),
        onCloseClick = close,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        VSpacer(24.dp)
        Image(
            painter = painterResource(id = R.drawable.ic_support_106_112),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        VSpacer(24.dp)
        InfoText(
            text = stringResource(R.string.Settings_VipSupport_Description),
            paddingBottom = 32.dp
        )
        VSpacer(24.dp)
        ButtonPrimaryYellowWithSpinner(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.Settings_VipSupport_StartChat),
            showSpinner = showSpinner,
            enabled = buttonEnabled,
            onClick = onRequestClicked
        )
        VSpacer(32.dp)
    }
}

@Preview
@Composable
fun VipSupportBottomSheetPreview() {
    ComposeAppTheme {
        VipSupportView(
            showSpinner = false,
            buttonEnabled = true,
            close = {},
            onRequestClicked = {}
        )
    }
}
