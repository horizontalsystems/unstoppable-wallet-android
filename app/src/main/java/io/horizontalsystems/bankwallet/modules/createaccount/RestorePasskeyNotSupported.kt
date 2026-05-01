package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.ButtonsStack
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton

class RestorePasskeyNotSupported : BaseComposableBottomSheetFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        RestorePasskeyNotSupportedScreen(navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestorePasskeyNotSupportedScreen(navController: NavBackStack<HSScreen>) {
    BottomSheetContent(
        onDismissRequest = navController::removeLastOrNull,
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
                navController.removeLastOrNull()
            }
        }
    }
}
