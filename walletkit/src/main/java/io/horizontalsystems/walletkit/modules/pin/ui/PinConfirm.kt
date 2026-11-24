package io.horizontalsystems.walletkit.modules.pin.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.pin.unlock.PinConfirmViewModel
import io.horizontalsystems.walletkit.modules.pin.unlock.PinUnlockModule

@Composable
fun PinConfirm(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<PinConfirmViewModel>(factory = PinConfirmViewModel.Factory())

    if (viewModel.uiState.unlocked) {
        onSuccess.invoke()
        viewModel.unlocked()
    }

    val uiState = viewModel.uiState

    PinScreen(
        modifier = modifier,
        onBack = onCancel,
        topBlock = {
            PinTopBlock(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Unlock_EnterPasscode),
                enteredCount = uiState.enteredCount,
                showShakeAnimation = uiState.showShakeAnimation,
                inputState = uiState.inputState,
                onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() },
            )
        },
        numpad = {
            PinNumpad(
                pinRandomized = viewModel.pinRandomized,
                deleteEnabled = uiState.enteredCount > 0,
                onNumberClick = { number -> viewModel.onKeyClick(number) },
                onDeleteClick = { viewModel.onDelete() },
                inputState = uiState.inputState,
            )
        },
        bottomButton = {
            PinRandomButton(
                pinRandomized = viewModel.pinRandomized,
                enabled = uiState.inputState is PinUnlockModule.InputState.Enabled,
                onClick = { random -> viewModel.updatePinRandomized(random) },
            )
        },
    )
}
