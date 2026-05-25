package io.horizontalsystems.bankwallet.modules.multiswap

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheet
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SwapInfoDialog(val input: Input) : BaseComposableBottomSheet() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        SwapInfoView(input.title, input.text) { navController.removeLastOrNull() }
    }

    @Serializable
    @Parcelize
    data class Input(val title: String, val text: String) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapInfoView(title: String, text: String, onCloseClick: () -> Unit) {
    BottomSheetContent(
        onDismissRequest = onCloseClick,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Understand),
                variant = ButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCloseClick
            )
        }
    ) {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.book_24),
            title = title
        )
        TextBlock(
            text = text,
            textAlign = TextAlign.Center
        )
    }
}
