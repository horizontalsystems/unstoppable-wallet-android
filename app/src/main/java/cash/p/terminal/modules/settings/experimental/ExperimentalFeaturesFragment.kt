package cash.p.terminal.modules.settings.experimental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
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
                    openTestnetSettings = { findNavController().slideFromRight(R.id.testnetSettingsFragment) },
                    openTimeLock = { findNavController().slideFromRight(R.id.timeLockFragment) },
                )
            }
        }
    }
}

@Composable
private fun ExperimentalScreen(
    onCloseClick: () -> Unit,
    openTestnetSettings: () -> Unit,
    openTimeLock: () -> Unit,
) {
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.ExperimentalFeatures_Title),
                navigationIcon = {
                    HsBackButton(onClick = onCloseClick)
                }
            )
            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    text = stringResource(R.string.ExperimentalFeatures_Description)
                )
                Spacer(Modifier.height(24.dp))
                CellUniversalLawrenceSection(
                    listOf({
                        ItemCell(R.string.BitcoinHodling_Title, openTimeLock)
                    }, {
                        ItemCell(R.string.TestnetSettings_EvmTestnet, openTestnetSettings)
                    })
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ItemCell(title: Int, onClick: () -> Unit) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        B2(
            text = stringResource(title),
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

@Composable
fun ActivateCell(
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalPadding = 0.dp,
                onClick = { onChecked(!checked) }
            ) {
                B2(
                    text = stringResource(R.string.Hud_Text_Activate),
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                HsSwitch(
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
    )
}