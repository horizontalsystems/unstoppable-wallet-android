package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
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
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

class ErrorDisplayDialogFragment : BaseComposableBottomSheetFragment() {

    private val title by lazy {
        requireArguments().getString(keyTitle) ?: ""
    }

    private val text by lazy {
        requireArguments().getString(keyText) ?: ""
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
                ErrorDisplayScreen(title, text, findNavController())
            }
        }
    }

    companion object {
        private const val keyTitle = "key_title"
        private const val keyText = "key_text"

        fun prepareParams(title: String, text: String) = bundleOf(
            keyTitle to title,
            keyText to text
        )
    }
}

@Composable
private fun ErrorDisplayScreen(
    title: String,
    errorText: String,
    navController: NavController
) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.icon_24_warning_2),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
            title = title,
            onCloseClick = {
                navController.popBackStack()
            }
        ) {
            VSpacer(12.dp)
            TextImportantError(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = errorText
            )
            VSpacer(8.dp)
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Button_Ok),
                onClick = {
                    navController.popBackStack()
                }
            )
            VSpacer(8.dp)
        }
    }
}
