package io.horizontalsystems.bankwallet.modules.evmfee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.BottomSheetHeaderCentered
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.core.findNavController

class FeeSettingsInfoDialog : BaseComposableBottomSheetFragment() {
    private val title by lazy { requireArguments().getString(TITLE) }
    private val text by lazy { requireArguments().getString(TEXT) }

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
                ComposeAppTheme {
                    FeeSettingsInfoScreen(title, text, findNavController())
                }
            }
        }
    }

    companion object {
        private const val TITLE = "title"
        private const val TEXT = "text"

        fun prepareParams(title: String, text: String) = bundleOf(
            TITLE to title,
            TEXT to text,
        )
    }
}

@Composable
fun FeeSettingsInfoScreen(title: String?, text: String?, navController: NavController) {
    BottomSheetHeaderCentered(
        iconPainter = painterResource(R.drawable.ic_info_24),
        title = title ?: ""
    ) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        Text(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            text = text ?: "",
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            title = stringResource(R.string.FeeSettings_Info_IUnderstand),
            onClick = {
                navController.popBackStack()
            }
        )
    }
}
