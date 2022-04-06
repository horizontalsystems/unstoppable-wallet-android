package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController

class InfoFragment : BaseFragment() {

    private val title by lazy { requireArguments().getString(TITLE)!! }
    private val text by lazy { requireArguments().getString(TEXT)!! }

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
                    InfoScreen(
                        title,
                        text,
                        findNavController()
                    )
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
private fun InfoScreen(
    title: String,
    text: String,
    navController: NavController
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.PlainString(stringResource(R.string.Info_Title)),

                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = title,
                    style = ComposeAppTheme.typography.headline2,
                    color = ComposeAppTheme.colors.jacob
                )
                Spacer(Modifier.height(36.dp))
                Text(
                    text = text,
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.bran
                )
                Spacer(Modifier.height(44.dp))
            }
        }

    }
}
