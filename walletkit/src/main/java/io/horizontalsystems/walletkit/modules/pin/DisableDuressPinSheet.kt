package io.horizontalsystems.walletkit.modules.pin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetBody
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetTextBlock
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import kotlinx.serialization.Serializable

// Only asks for confirmation; the caller performs the removal after the passcode check,
// so the sheet is already gone when the unlock screen appears.
@Serializable
data object DisableDuressPinSheet : HSBottomSheet() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current

        BottomSheetBody(
            buttons = {
                HSButton(
                    title = stringResource(R.string.DisableDuressPin_Button),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = {
                        resultEventBus.sendResult(Result(true))
                        navigation.removeLastOrNull()
                    }
                )
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.trash_filled_24),
                    imageTint = ComposeAppTheme.colors.lucian,
                    title = stringResource(R.string.DisableDuressPin_Title)
                )
                BottomSheetTextBlock(stringResource(R.string.DisableDuressPin_Description))
            },
        )
    }

    data class Result(val confirmed: Boolean)
}
