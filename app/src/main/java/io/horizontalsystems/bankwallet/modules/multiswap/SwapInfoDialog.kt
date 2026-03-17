package io.horizontalsystems.bankwallet.modules.multiswap

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
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SwapInfoDialogScreen(val title: String, val text: String) : HSScreen(
    bottomSheet = true
) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        SwapInfoView(title, text) { backStack.removeLastOrNull() }
    }
}

class SwapInfoDialog : BaseComposableBottomSheetFragment() {

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
            }
        }
    }

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
