package io.horizontalsystems.bankwallet.modules.settings.security.securesend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetTextBlock
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellGroup
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import kotlinx.serialization.Serializable

@Serializable
data object SecureSendConfigScreen : HSScreen(bottomSheet = true) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        SecureSendConfigScreen(backStack)
    }
}

class SecureSendConfigDialog : BaseComposableBottomSheetFragment() {
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecureSendConfigScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<SecureSendConfigViewModel>(factory = SecureSendConfigModule.Factory())
    val uiState = viewModel.uiState

    BottomSheetContent(
        onDismissRequest = {
            backStack.removeLastOrNull()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Done),
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
                onClick = {
                    backStack.removeLastOrNull()
                }
            )
        },
        content = {
            BottomSheetHeaderV3(
                title = stringResource(R.string.Premium_UpgradeFeature_SecureSend)
            )

            BottomSheetTextBlock(stringResource(R.string.SecureSend_Config_Subtitle))

            VSpacer(8.dp)
            CellGroup(paddingValues = PaddingValues(horizontal = 16.dp)) {
                CellSecondary(
                    middle = {
                        CellMiddleInfo(
                            title = stringResource(R.string.Send_Address_PhishingCheck).hs,
                            subtitle = stringResource(R.string.SecureSend_Config_PhishingCheckDescription).hs,
                        )
                    },
                    right = {
                        CellRightControlsSwitcher(
                            checked = uiState.phishingEnabled,
                            onCheckedChange = { viewModel.setPhishingEnabled(it) }
                        )
                    }
                )
                HsDivider()
                CellSecondary(
                    middle = {
                        CellMiddleInfo(
                            title = stringResource(R.string.Send_Address_BlacklistCheck).hs,
                            subtitle = stringResource(R.string.SecureSend_Config_BlacklistCheckDescription).hs,
                        )
                    },
                    right = {
                        CellRightControlsSwitcher(
                            checked = uiState.blacklistEnabled,
                            onCheckedChange = { viewModel.setBlacklistEnabled(it) }
                        )
                    }
                )
                HsDivider()
                CellSecondary(
                    middle = {
                        CellMiddleInfo(
                            title = stringResource(R.string.Send_Address_SanctionCheck).hs,
                            subtitle = stringResource(R.string.SecureSend_Config_SanctionCheckDescription).hs,
                        )
                    },
                    right = {
                        CellRightControlsSwitcher(
                            checked = uiState.sanctionsEnabled,
                            onCheckedChange = { viewModel.setSanctionsEnabled(it) }
                        )
                    }
                )
            }
            VSpacer(24.dp)
        }
    )
}
