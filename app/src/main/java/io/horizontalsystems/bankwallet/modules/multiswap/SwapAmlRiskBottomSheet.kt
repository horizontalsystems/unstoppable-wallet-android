package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapAmlRiskBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onChooseAnotherProvider: () -> Unit,
) {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            buttons = {
                HSButton(
                    title = stringResource(R.string.Swap_AmlRisk_Button),
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    onClick = onChooseAnotherProvider,
                )
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.ic_warning_64),
                    imageTint = ComposeAppTheme.colors.lucian,
                    title = stringResource(R.string.Swap_AmlRisk_Title),
                )
                TextBlock(
                    text = stringResource(R.string.Swap_AmlRisk_Description),
                    textAlign = TextAlign.Center,
                )
            }
        )
    }
}
