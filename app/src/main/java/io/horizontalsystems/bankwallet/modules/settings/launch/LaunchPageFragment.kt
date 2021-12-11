package io.horizontalsystems.bankwallet.modules.settings.launch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import coil.annotation.ExperimentalCoilApi
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.core.findNavController

class LaunchPageFragment : BaseFragment() {

    val viewModel by viewModels<LaunchPageViewModel> { LaunchPageModule.Factory() }

    @ExperimentalCoilApi
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
                    LaunchScreen(viewModel) { findNavController().popBackStack() }
                }
            }
        }
    }
}

@ExperimentalCoilApi
@Composable
fun LaunchScreen(
    viewModel: LaunchPageViewModel,
    onCloseButtonClick: () -> Unit,
) {
    val options by viewModel.optionsLiveData.observeAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Settings_LaunchScreen),
                navigationIcon = {
                    IconButton(onCloseButtonClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            options?.let {
                ScreenOptionsView(it) { launchPage -> viewModel.onLaunchPageSelect(launchPage) }
            }
        }
    }

}

@Composable
private fun ScreenOptionsView(
    select: Select<LaunchPage>,
    onClick: ((LaunchPage) -> Unit)
) {
    CellSingleLineLawrenceSection(select.options) { option ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    onClick.invoke(option)
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = option.iconRes),
                contentDescription = "option icon",
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.grey)
            )
            Text(
                text = option.title.getString(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz
            )
            if (option == select.selected) {
                Image(
                    painter = painterResource(id = R.drawable.ic_checkmark_20),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob)
                )
            }
        }
    }
}

@Preview
@Composable
fun ScreenOptionsViewPreview() {
    val select =
        Select(LaunchPage.Auto, listOf(LaunchPage.Auto, LaunchPage.Market, LaunchPage.Watchlist))
    ComposeAppTheme {
        ScreenOptionsView(select, { })
    }
}
