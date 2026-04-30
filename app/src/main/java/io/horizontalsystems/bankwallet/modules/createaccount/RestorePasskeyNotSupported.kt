package io.horizontalsystems.bankwallet.modules.createaccount

import android.os.Bundle
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
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.ButtonsStack
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.core.findNavController

class RestorePasskeyNotSupported : BaseComposableBottomSheetFragment() {
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
                ComposeAppTheme {
                    RestorePasskeyNotSupportedScreen(findNavController())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestorePasskeyNotSupportedScreen(navController: NavController) {
    BottomSheetContent(
        onDismissRequest = navController::popBackStack,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.warning_filled_24),
            imageTint = ComposeAppTheme.colors.jacob,
            title = stringResource(R.string.ImportWallet_RestorePasskeyNotSupported_Title)
        )
        InfoTextBody(
            text = stringResource(R.string.ImportWallet_RestorePasskeyNotSupported_Description),
            color = ComposeAppTheme.colors.grey,
            textAlign = TextAlign.Center
        )
        ButtonsStack {
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                title = stringResource(R.string.ImportWallet_Button_Understood)
            ) {
                navController.popBackStack()
            }
        }
    }
}
