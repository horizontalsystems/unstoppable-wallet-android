package io.horizontalsystems.bankwallet.modules.settings.experimental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class ExperimentalFeaturesFragment : BaseFragment() {

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
                ExperimentalScreen(
                    onCloseClick = { findNavController().popBackStack() },
                    openTimeLock = { findNavController().slideFromRight(R.id.timeLockFragment) }
                )
            }
        }
    }
}

@Composable
private fun ExperimentalScreen(
    onCloseClick: () -> Unit,
    openTimeLock: () -> Unit,
) {
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.ExperimentalFeatures_Title),
                navigationIcon = {
                    HsIconButton(onClick = onCloseClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    text = stringResource(R.string.ExperimentalFeatures_Description)
                )
                TimeLockButtonCell(openTimeLock)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TimeLockButtonCell(openTimeLock: () -> Unit) {
    CellSingleLineLawrenceSection(
        listOf {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = openTimeLock)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                B2(
                    text = stringResource(R.string.ExperimentalFeatures_BitcoinHodling),
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                )
            }
        }
    )
}

@Preview
@Composable
private fun PreviewExperimentalScreen() {
    ComposeAppTheme {
        ExperimentalScreen({}, {})
    }
}
