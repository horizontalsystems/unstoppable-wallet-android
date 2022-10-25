package io.horizontalsystems.bankwallet.modules.managewallets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

class CoinNotSupportedDialog : BaseComposableBottomSheetFragment() {

    private val accountTypeDescription by lazy {
        requireArguments().getString(ACCOUNT_TYPE_DESC) ?: ""
    }

    private val coinTitle by lazy {
        requireArguments().getString(COIN_TITLE) ?: ""
    }

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
                CoinNotSupportedScreen(
                    findNavController(),
                    accountTypeDescription,
                    coinTitle
                )
            }
        }
    }

    companion object {
        private const val ACCOUNT_TYPE_DESC = "account_type_desc"
        private const val COIN_TITLE = "coin_title"

        fun prepareParams(accountTypeDescription: String, coinTitle: String) = bundleOf(
            ACCOUNT_TYPE_DESC to accountTypeDescription,
            COIN_TITLE to coinTitle,
        )
    }
}

@Composable
fun CoinNotSupportedScreen(
    navController: NavController,
    accountTypeDescription: String,
    coinTitle: String
) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = stringResource(R.string.ManageCoins_NotSupported),
            onCloseClick = {
                navController.popBackStack()
            }
        ) {
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(
                    R.string.ManageCoins_NotSupportedDescription,
                    accountTypeDescription,
                    coinTitle
                )
            )
            Spacer(Modifier.height(20.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.Button_Close),
                onClick = {
                    navController.popBackStack()
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
