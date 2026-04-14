package com.quantum.wallet.bankwallet.modules.multiswap

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
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.requireInput
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSButton
import com.quantum.wallet.bankwallet.uiv3.components.info.TextBlock
import com.quantum.wallet.core.findNavController
import kotlinx.parcelize.Parcelize

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
                val navController = findNavController()
                val input = navController.requireInput<Input>()

                ComposeAppTheme {
                    SwapInfoView(input.title, input.text) { dismiss() }
                }
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
