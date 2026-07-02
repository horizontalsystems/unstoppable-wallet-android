package io.horizontalsystems.walletkit.modules.settings.security.securesend

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetTextBlock
import io.horizontalsystems.walletkit.uiv3.components.cell.CellGroup
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.walletkit.uiv3.components.cell.CellSecondary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import kotlinx.serialization.Serializable

@Serializable
data object SecureSendConfigSheet : HSBottomSheet() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SecureSendConfigScreen(navigation)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecureSendConfigScreen(navigation: HSNavigation) {
    val viewModel = viewModel<SecureSendConfigViewModel>(factory = SecureSendConfigModule.Factory())
    val uiState = viewModel.uiState

    BottomSheetContent(
        onDismissRequest = {
            navigation.removeLastOrNull()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Done),
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
                onClick = {
                    navigation.removeLastOrNull()
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
