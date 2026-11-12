package io.horizontalsystems.walletkit.modules.createaccount

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.components.InfoTextBody
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetBody
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.ButtonsStack
import kotlinx.serialization.Serializable

@Serializable
data class CreatePasskeyNotSupportedSheet(val input: ManageAccountsModule.Input?) : HSBottomSheet(expanded = true) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        CreatePasskeyNotSupportedScreen(navigation, input)
    }
}

@Composable
fun CreatePasskeyNotSupportedScreen(navigation: HSNavigation, input: ManageAccountsModule.Input?) {
    BottomSheetBody {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.warning_filled_24),
            imageTint = ComposeAppTheme.colors.jacob,
            title = stringResource(R.string.CreateNewWallet_CreatePasskeyNotSupported_Title)
        )
        InfoTextBody(
            text = stringResource(R.string.CreateNewWallet_CreatePasskeyNotSupported_Description),
            color = ComposeAppTheme.colors.grey,
            textAlign = TextAlign.Center
        )
        ButtonsStack {
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                title = stringResource(R.string.CreateNewWallet_Button_CreateStandardWallet)
            ) {
                // Back to the wallet type choice, whether it is the page or the sheet
                navigation.removeLastUntil(CreateAccountPasskeyPage::class, true)
                navigation.slideFromRight(CreateAccountStandardPage(input))
            }
        }
    }
}
