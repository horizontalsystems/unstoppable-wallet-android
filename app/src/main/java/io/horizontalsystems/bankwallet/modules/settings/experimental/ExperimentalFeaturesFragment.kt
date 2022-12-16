package io.horizontalsystems.bankwallet.modules.settings.experimental

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.settings.experimental.testnet.TestnetSettingsCell
import io.horizontalsystems.bankwallet.modules.settings.experimental.testnet.TestnetSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.experimental.testnet.TestnetSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.experimental.timelock.TimeLockCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.core.findNavController

class ExperimentalFeaturesFragment : BaseFragment() {
    private val testnetSettingsViewModel by viewModels<TestnetSettingsViewModel> {
        TestnetSettingsModule.Factory()
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
                ExperimentalScreen(
                    onCloseClick = { findNavController().popBackStack() },
                    openTimeLock = { findNavController().slideFromRight(R.id.timeLockFragment) },
                    testnetSettingsViewModel = testnetSettingsViewModel
                )
            }
        }
    }
}

@Composable
private fun ExperimentalScreen(
    onCloseClick: () -> Unit,
    openTimeLock: () -> Unit,
    testnetSettingsViewModel: TestnetSettingsViewModel
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
                TimeLockCell(openTimeLock)
                Spacer(Modifier.height(24.dp))
                TestnetSettingsCell(testnetSettingsViewModel)
            }
        }
    }
}
