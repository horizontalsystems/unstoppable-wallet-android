package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.edit.PinEditModule
import io.horizontalsystems.bankwallet.modules.pin.edit.PinEditViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.animations.CrossSlide
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian

@Composable
fun PinEdit(
    dismissWithSuccess: () -> Unit,
    onBackPress: () -> Unit,
    viewModel: PinEditViewModel = viewModel(factory = PinEditModule.Factory())
) {
    if (viewModel.uiState.finished) {
        dismissWithSuccess.invoke()
        viewModel.finished()
    }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.EditPin_Title),
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
                        PinEditModule.EditStage.Unlock -> {
                            PinTopBlock(
                                title = {
                                    subhead2_grey(
                                        text = stringResource(stage.title),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                enteredCount = viewModel.uiState.enteredCount,
                                showShakeAnimation = viewModel.uiState.showShakeAnimation,
                                onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() }
                            )
                        }
                        PinEditModule.EditStage.Enter -> {
                            PinTopBlock(
                                title = {
                                    val error = viewModel.uiState.error
                                    if (error != null) {
                                        subhead2_lucian(
                                            text = error,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        subhead2_grey(
                                            text = stringResource(stage.title),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                },
                                enteredCount = viewModel.uiState.enteredCount,
                            )
                        }
                        PinEditModule.EditStage.Confirm -> {
                            PinTopBlock(
                                title = {
                                    subhead2_grey(
                                        text = stringResource(stage.title),
                                        textAlign = TextAlign.Center
                                    )
                                },
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
