package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapConfirmBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    navController: NavController,
    uiState: SwapUiState,
    viewModel: SwapViewModel
) {
    BottomSheetContent(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        buttons = {
            val coroutineScope = rememberCoroutineScope()
            val view = LocalView.current

            if (uiState.loading) {
                ButtonPrimaryYellow(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    title = stringResource(R.string.Alert_Loading),
                    enabled = false,
                    onClick = { },
                )
            } else if (!uiState.validQuote) {
                ButtonPrimaryDefault(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = {
                        viewModel.refresh()
                    },
                )
            } else {
                var buttonEnabled by remember { mutableStateOf(true) }
                ButtonPrimaryYellow(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    title = stringResource(R.string.Swap),
                    enabled = buttonEnabled && !uiState.expired,
                    onClick = {
                        coroutineScope.launch {
                            buttonEnabled = false
                            HudHelper.showInProcessMessage(
                                view,
                                R.string.Swap_Swapping,
                                SnackbarDuration.INDEFINITE
                            )

                            val result = try {
                                viewModel.swap()

                                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                                delay(1200)
                                SwapConfirmFragment.Result(true)
                            } catch (t: Throwable) {
                                HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                                SwapConfirmFragment.Result(false)
                            }

                            buttonEnabled = true
                            navController.setNavigationResultX(result)
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    ) {
        SwapConfirmScreen2(
            navController = navController,
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun Preview_SwapConfirmBottomSheet() {
    ComposeAppTheme {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

//        SwapConfirmBottomSheet(
//            onDismissRequest = {},
//            sheetState = sheetState,
//        )
    }
}

