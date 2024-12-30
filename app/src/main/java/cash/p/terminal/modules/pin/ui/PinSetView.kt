package cash.p.terminal.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.modules.pin.set.PinSetModule
import cash.p.terminal.modules.pin.set.PinSetViewModel
import cash.p.terminal.ui.compose.animations.CrossSlide
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun PinSet(
    title: String,
    description: String,
    dismissWithSuccess: () -> Unit,
    onBackPress: () -> Unit,
    forDuress: Boolean = false,
    viewModel: PinSetViewModel = viewModel(factory = PinSetModule.Factory(forDuress))
) {
    if (viewModel.uiState.finished) {
        dismissWithSuccess.invoke()
        viewModel.finished()
    }

    Scaffold(
        backgroundColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = title,
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = ComposeAppTheme.colors.tyler)
            ) {
                CrossSlide(
                    targetState = viewModel.uiState.stage,
                    modifier = Modifier.weight(1f),
                    reverseAnimation = viewModel.uiState.reverseSlideAnimation
                ) { stage ->
                    when (stage) {
                        PinSetModule.SetStage.Enter -> {
                            PinTopBlock(
                                title = description,
                                error = viewModel.uiState.error,
                                enteredCount = viewModel.uiState.enteredCount,
                            )
                        }
                        PinSetModule.SetStage.Confirm -> {
                            PinTopBlock(
                                title = stringResource(R.string.PinSet_ConfirmInfo),
                                enteredCount = viewModel.uiState.enteredCount,
                            )
                        }
                    }
                }

                PinNumpad(
                    onNumberClick = { number -> viewModel.onKeyClick(number) },
                    onDeleteClick = { viewModel.onDelete() },
                )
            }
        }
    }
}
