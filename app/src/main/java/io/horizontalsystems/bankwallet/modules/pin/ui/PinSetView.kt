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
import io.horizontalsystems.bankwallet.modules.pin.set.PinSetModule
import io.horizontalsystems.bankwallet.modules.pin.set.PinSetViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.animations.CrossSlide
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian

@Composable
fun PinSet(
    dismissWithSuccess: () -> Unit,
    onBackPress: () -> Unit,
    viewModel: PinSetViewModel = viewModel(factory = PinSetModule.Factory())
) {
    if (viewModel.uiState.finished) {
        dismissWithSuccess.invoke()
        viewModel.finished()
    }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.PinSet_Title),
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
                        PinSetModule.SetStage.Confirm -> {
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
