package io.horizontalsystems.bankwallet.modules.info

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
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
import kotlinx.parcelize.Parcelize

class InfoFragment : BaseFragment() {

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
                        requireArguments().getParcelable(INFO_DATA)!!,
                        findNavController()
                    )
                }
            }
        }
    }

    companion object {
        private const val INFO_DATA = "info_data"

        fun prepareParams(infoData: List<InfoBlock>) = bundleOf(INFO_DATA to InfoData(infoData))
    }

}

@Parcelize
data class InfoData(val items: List<InfoBlock>) : Parcelable

sealed class InfoBlock : Parcelable {
    @Parcelize
    class Header(@StringRes val text: Int) : InfoBlock()

    @Parcelize
    class SubHeader(@StringRes val text: Int) : InfoBlock()

    @Parcelize
    class Body(@StringRes val text: Int) : InfoBlock()

    @Parcelize
    class BodyString(val text: String) : InfoBlock()
}

@Composable
private fun InfoScreen(
    infoData: InfoData,
    navController: NavController
) {

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
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
                infoData.items.forEach { block ->
                    when (block) {
                        is InfoBlock.Header -> infoHeader(block.text)
                        is InfoBlock.SubHeader -> infoSubHeader(block.text)
                        is InfoBlock.Body -> infoBody(block.text)
                        is InfoBlock.BodyString -> infoBodyString(block.text)
                    }
                }
                Spacer(Modifier.height(44.dp))
            }
        }
    }
}

@Composable
fun infoSubHeader(text: Int) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(text),
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.jacob
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
fun infoBodyString(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = text,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.bran
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
fun infoBody(text: Int) {
    infoBodyString(stringResource(text))
}

@Composable
fun infoHeader(text: Int) {
    Spacer(Modifier.height(5.dp))
    Text(
        text = stringResource(text),
        style = ComposeAppTheme.typography.title2,
        color = ComposeAppTheme.colors.leah
    )
    Spacer(Modifier.height(8.dp))
    Divider(
        thickness = 1.dp,
        color = ComposeAppTheme.colors.grey50
    )
    Spacer(Modifier.height(5.dp))
}
