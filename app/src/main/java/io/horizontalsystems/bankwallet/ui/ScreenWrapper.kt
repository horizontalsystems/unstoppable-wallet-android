package io.horizontalsystems.bankwallet.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView

@Composable
fun <T> ScreenWrapper(
    provider: () -> InputState<T>,
    content: @Composable (T) -> Unit
) {
    val viewModel = viewModel<ViewModelPre<T>>(initializer = {
        object : ViewModelPre<T>() {
            override val output: InputState<T> = provider.invoke()
        }
    })

    when (val output = viewModel.output) {
        is InputState.Error<T> -> {
            ListErrorView(output.e.message ?: output.e.javaClass.simpleName) { }
        }

        is InputState.Success<T> -> {
            content.invoke(output.data)
        }
    }
}

sealed class InputState<T> {
    data class Error<T>(val e: Throwable) : InputState<T>()
    data class Success<T>(val data: T) : InputState<T>()
}


abstract class ViewModelPre<T> : ViewModel() {
    abstract val output: InputState<T>
}
