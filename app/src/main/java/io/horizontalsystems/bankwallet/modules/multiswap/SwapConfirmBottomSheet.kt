package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton

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
            HSButton(
                title = "Close",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
//                    coroutineScope.launch {
//                        sheetState.hide()
//                    }.invokeOnCompletion {
//                        if (!sheetState.isVisible) {
//                            showBottomSheet = false
//                        }
//                    }
                }
            )
        }
    ) {
        SwapConfirmScreen2(
            navController = navController,
            uiState = uiState,
            viewModel = viewModel
        )
    }

//    BottomSheetHeaderV3(
//        title = stringResource(R.string.SwapConfirm_Title)
//    )
//
//    CellSecondary(
//        middle = {
//            CellMiddleInfoTextIcon(
//                text = stringResource(R.string.Swap_Slippage).hs,
//
//            )
//        },
//        right = {
//
//        }
//    )
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

