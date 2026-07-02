package io.horizontalsystems.walletkit.modules.createaccount

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.InfoTextBody
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.ButtonsStack
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import kotlinx.serialization.Serializable

@Serializable
data object RestorePasskeyNotSupportedSheet : HSBottomSheet() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        RestorePasskeyNotSupportedScreen(navigation)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestorePasskeyNotSupportedScreen(navigation: HSNavigation) {
    BottomSheetContent(
        onDismissRequest = navigation::removeLastOrNull,
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
                navigation.removeLastOrNull()
            }
        }
    }
}
