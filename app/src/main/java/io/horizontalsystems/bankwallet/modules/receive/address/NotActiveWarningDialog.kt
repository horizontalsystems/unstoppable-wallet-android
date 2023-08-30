package io.horizontalsystems.bankwallet.modules.receive.address

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
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController

class NotActiveWarningDialog : BaseComposableBottomSheetFragment() {

    private val title by lazy {
        requireArguments().getString(TITLE) ?: ""
    }

    private val text by lazy {
        requireArguments().getString(TEXT) ?: ""
    }

    private val showAsWarning by lazy {
        requireArguments().getBoolean(SHOW_AS_WARNING)
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
                NotActiveWarningScreen(findNavController(), title, text, showAsWarning)
            }
        }
    }

    companion object {
        private const val TITLE = "title"
        private const val TEXT = "text"
        private const val SHOW_AS_WARNING = "show_as_warning"

        fun prepareParams(title: String, text: String, showAsWarning: Boolean = true) = bundleOf(
            TITLE to title,
            TEXT to text,
            SHOW_AS_WARNING to showAsWarning
        )
    }
}

@Composable
fun NotActiveWarningScreen(
    navController: NavController,
    title: String,
    text: String,
    showAsWarning: Boolean
) {
    ComposeAppTheme {
        val icon = if (showAsWarning) R.drawable.ic_attention_24 else R.drawable.ic_info_24
        val iconTint = if (showAsWarning) {
            ColorFilter.tint(ComposeAppTheme.colors.jacob)
        } else {
            ColorFilter.tint(ComposeAppTheme.colors.grey)
        }
        BottomSheetHeader(
            iconPainter = painterResource(icon),
            iconTint = iconTint,
            title = title,
            onCloseClick = {
                navController.popBackStack()
            }
        ) {
            if (showAsWarning) {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    text = text
                )
            } else {
                body_bran(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                    text = text
                )
            }

            if (showAsWarning) {
                VSpacer(12.dp)
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Understand),
                    onClick = {
                        navController.popBackStack()
                    }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
