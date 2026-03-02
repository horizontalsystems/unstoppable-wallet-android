package io.horizontalsystems.bankwallet.modules.confirm

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class ErrorBottomSheetScreen(val error: String) : HSScreen(bottomSheet = true) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        ErrorBottomSheetScreen(backStack, error)
    }
}

class ErrorBottomSheet : BaseComposableBottomSheetFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
//                val navController = findNavController()
//                navController.getInput<Input>()?.let { input ->
//                    ErrorBottomSheetScreen(navController, input.error)
//                }
            }
        }
    }

    @Parcelize
    data class Input(val error: String) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBottomSheetScreen(
    backStack: NavBackStack<HSScreen>,
    error: String
) {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = backStack::removeLastOrNull,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.Button_CopyError),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        TextHelper.copyText(error)
                        backStack.removeLastOrNull()
                    }
                )
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.warning_filled_24),
                    imageTint = ComposeAppTheme.colors.lucian,
                    title = stringResource(R.string.Send_UnexpectedError)
                )
                TextBlock(text = stringResource(R.string.Send_UnexpectedError_Description), textAlign = TextAlign.Center)
            }
        )
    }
}

