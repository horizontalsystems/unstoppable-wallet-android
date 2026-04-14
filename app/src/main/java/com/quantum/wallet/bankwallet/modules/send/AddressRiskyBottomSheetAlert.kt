package com.quantum.wallet.bankwallet.modules.send

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.getInput
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryRed
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import com.quantum.wallet.bankwallet.ui.compose.components.TextImportantError
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import com.quantum.wallet.bankwallet.ui.extensions.BottomSheetHeader
import com.quantum.wallet.core.findNavController
import kotlinx.parcelize.Parcelize

class AddressRiskyBottomSheetAlert : BaseComposableBottomSheetFragment() {

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
                navController.getInput<Input>()?.let { input ->
                    RiskyAddressAlertView(
                        alertText = input.alertText,
                        onCloseClick = {
                            navController.popBackStack()
                        },
                        onContinueClick = {
                            navController.setNavigationResultX(Result(true))
                        }
                    )
                }
            }
        }
    }

    @Parcelize
    data class Input(val alertText: String) : Parcelable

    @Parcelize
    data class Result(val canContinue: Boolean) : Parcelable
}

@Composable
private fun RiskyAddressAlertView(
    alertText: String,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
            title = stringResource(R.string.Send_RiskyAddress),
            onCloseClick = onCloseClick
        ) {
            VSpacer(12.dp)
            TextImportantError(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = alertText
            )
            VSpacer(32.dp)
            ButtonPrimaryRed(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.Button_ContinueAnyway),
                onClick = {
                    onContinueClick()
                }
            )
            VSpacer(12.dp)
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.Button_Cancel),
                onClick = onCloseClick
            )
            VSpacer(32.dp)
        }
    }
}
