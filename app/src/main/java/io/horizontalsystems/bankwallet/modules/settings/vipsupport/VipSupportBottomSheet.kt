package io.horizontalsystems.bankwallet.modules.settings.vipsupport

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipSupportBottomSheet(
    isBottomSheetVisible: Boolean,
    close: () -> Unit
) {
    val viewModel = viewModel<VipSupportViewModel>(factory = VipSupportModule.Factory())
    val focusRequester = remember { FocusRequester() }
    val modalBottomSheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val uiState = viewModel.uiState

    LaunchedEffect(uiState.showError) {
        if (uiState.showError) {
            Toast.makeText(context, R.string.Settings_PersonalSupport_Requestfailed, Toast.LENGTH_SHORT).show()
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
            BottomSheetHeader(
                iconPainter = painterResource(R.drawable.prem_vip_support_24),
                title = stringResource(R.string.Settings_VipSupport),
                onCloseClick = close,
                iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
            ) {
                if (uiState.showRequestForm) {
                    InfoText(
                        text = stringResource(R.string.Settings_PersonalSupport_EnterTelegramAccountDescription),
                    )
                    FormsInput(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        hint = stringResource(R.string.Settings_PersonalSupport_UsernameHint),
                        onValueChange = viewModel::onUsernameChange
                    )
                    VSpacer(24.dp)
                    ButtonPrimaryYellowWithSpinner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        title = stringResource(R.string.Settings_PersonalSupport_Request),
                        showSpinner = uiState.showSpinner,
                        enabled = uiState.buttonEnabled,
                        onClick = {
                            viewModel.onRequestClicked()
                        }
                    )
                    VSpacer(24.dp)
                } else {
                    InfoText(
                        text = stringResource(R.string.Settings_PersonalSupport_YouAlreadyRequestedSupportDescription),
                        paddingBottom = 32.dp
                    )
                    VSpacer(24.dp)
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp),
                        title = stringResource(R.string.Settings_PersonalSupport_OpenTelegram),
                        onClick = {
                            context.packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                                ?.let {
                                    context.startActivity(it)
                                }
                        },
                    )
                    VSpacer(16.dp)
                    ButtonPrimaryDefault(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        title = stringResource(R.string.Settings_PersonalSupport_NewRequest),
                        onClick = {
                            viewModel.showRequestForm()
                        },
                    )
                    VSpacer(24.dp)
                }
            }
        }
    }
}
