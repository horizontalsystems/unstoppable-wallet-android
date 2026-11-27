package io.horizontalsystems.walletkit.modules.pin.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.pin.set.PinSetModule
import io.horizontalsystems.walletkit.modules.pin.set.PinSetViewModel
import io.horizontalsystems.walletkit.ui.compose.animations.CrossSlide

@Composable
fun PinSet(
    title: String,
    description: String,
    dismissWithSuccess: () -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
    forDuress: Boolean = false,
    viewModel: PinSetViewModel = viewModel(factory = PinSetModule.Factory(forDuress))
) {
    if (viewModel.uiState.finished) {
        dismissWithSuccess.invoke()
        viewModel.finished()
    }

    val uiState = viewModel.uiState

    PinScreen(
        modifier = modifier,
        onBack = onBackPress,
        topBlock = {
            CrossSlide(
                targetState = uiState.stage,
                modifier = Modifier.weight(1f),
                reverseAnimation = uiState.reverseSlideAnimation
            ) { stage ->
                when (stage) {
                    PinSetModule.SetStage.Enter -> {
                        PinTopBlock(
                            title = title,
                            description = description,
                            error = uiState.error,
                            enteredCount = uiState.enteredCount,
                        )
                    }

                    PinSetModule.SetStage.Confirm -> {
                        PinTopBlock(
                            title = stringResource(R.string.PinSet_ConfirmInfo),
                            enteredCount = uiState.enteredCount,
                        )
                    }
                }
            }
        },
        numpad = {
            PinNumpad(
                onNumberClick = { number -> viewModel.onKeyClick(number) },
                onDeleteClick = { viewModel.onDelete() },
                deleteEnabled = uiState.enteredCount > 0,
            )
        },
    )
}
