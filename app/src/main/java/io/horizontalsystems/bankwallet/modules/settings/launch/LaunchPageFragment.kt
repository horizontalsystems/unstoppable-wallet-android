package io.horizontalsystems.bankwallet.modules.settings.launch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import coil.annotation.ExperimentalCoilApi
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.settings.launch.LaunchPageModule.LaunchPageViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
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
                    LaunchScreen(
                        viewModel,
                    ) { findNavController().popBackStack() }
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
            TopToolbar(
                R.string.Settings_LaunchScreen,
                onCloseButtonClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            options?.let {
                ScreenOptionsView(
                    it
                ) { launchPage -> viewModel.onLaunchPageSelect(launchPage) }
            }
        }
    }

}

@Composable
fun TopToolbar(title: Int, onBackButtonClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onBackButtonClick.invoke()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "back button",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
                tint = ComposeAppTheme.colors.jacob
            )
        }
        Text(
            text = stringResource(title),
            color = ComposeAppTheme.colors.oz,
            style = ComposeAppTheme.typography.title3,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ScreenOptionsView(
    options: List<LaunchPageViewItem>,
    onClick: ((LaunchPage) -> Unit)? = null
) {
    CellSingleLineLawrenceSection(options) { option ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    onClick?.invoke(option.launchPage)
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = option.launchPage.icon),
                contentDescription = "option icon",
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.grey)
            )
            Text(
                text = stringResource(option.launchPage.titleResId),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz
            )
            if (option.selected) {
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
    val options = listOf(
        LaunchPageViewItem(LaunchPage.Auto, false),
        LaunchPageViewItem(LaunchPage.Market, true),
        LaunchPageViewItem(LaunchPage.Watchlist, false)
    )
    ComposeAppTheme {
        ScreenOptionsView(options)
    }
}
