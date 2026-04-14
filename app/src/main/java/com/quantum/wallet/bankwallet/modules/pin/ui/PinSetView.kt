package com.quantum.wallet.bankwallet.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.modules.pin.set.PinSetModule
import com.quantum.wallet.bankwallet.modules.pin.set.PinSetViewModel
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.animations.CrossSlide
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

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

    HSScaffold(
        title = title,
        onBack = onBackPress,
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
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
