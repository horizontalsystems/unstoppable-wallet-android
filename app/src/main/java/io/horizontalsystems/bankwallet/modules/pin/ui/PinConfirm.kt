package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinConfirmViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_lucian

@Composable
fun PinConfirm(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = viewModel<PinConfirmViewModel>(factory = PinConfirmViewModel.Factory())

    if (viewModel.uiState.unlocked) {
        onSuccess.invoke()
        viewModel.unlocked()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ComposeAppTheme.colors.tyler)
    ) {
        PinTopBlock(
            modifier = Modifier.weight(1f),
            title = {
                val error = viewModel.uiState.error
                if (error != null) {
                    headline1_lucian(
                        text = error,
                        textAlign = TextAlign.Center
                    )
                } else {
                    headline1_leah(
                        text = stringResource(R.string.Unlock_Passcode),
                        textAlign = TextAlign.Center
                    )
                }
            },
            enteredCount = viewModel.uiState.enteredCount,
            showCancelButton = true,
            showShakeAnimation = viewModel.uiState.showShakeAnimation,
            inputState = viewModel.uiState.inputState,
            onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() },
            onCancelClick = onCancel
        )

        PinNumpad(
            onNumberClick = { number -> viewModel.onKeyClick(number) },
            onDeleteClick = { viewModel.onDelete() },
            showRandomizer = true,
            inputState = viewModel.uiState.inputState
        )
    }
}