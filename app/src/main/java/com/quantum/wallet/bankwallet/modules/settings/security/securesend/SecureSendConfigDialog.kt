package com.quantum.wallet.bankwallet.modules.settings.security.securesend

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
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import com.quantum.wallet.bankwallet.uiv3.components.bottomsheet.BottomSheetTextBlock
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellGroup
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightControlsSwitcher
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellSecondary
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSButton
import com.quantum.wallet.core.findNavController

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
                val navController = findNavController()

                ComposeAppTheme {
                    SecureSendConfigScreen(navController)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecureSendConfigScreen(navController: NavController) {
    val viewModel = viewModel<SecureSendConfigViewModel>(factory = SecureSendConfigModule.Factory())
    val uiState = viewModel.uiState

    BottomSheetContent(
        onDismissRequest = {
            navController.popBackStack()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Done),
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
                onClick = {
                    navController.popBackStack()
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
