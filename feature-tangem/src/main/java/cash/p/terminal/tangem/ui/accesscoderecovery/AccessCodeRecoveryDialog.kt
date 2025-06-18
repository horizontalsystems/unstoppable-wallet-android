package cash.p.terminal.tangem.ui.accesscoderecovery

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.BaseComposableBottomSheetFragment
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.R
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsCheckbox
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.headline2_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.setNavigationResultX
import kotlinx.parcelize.Parcelize
import org.koin.androidx.viewmodel.ext.android.viewModel

class AccessCodeRecoveryDialog : BaseComposableBottomSheetFragment() {

    private val viewModel: AccessCodeRecoveryViewModel by viewModel<AccessCodeRecoveryViewModel>()
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
                LaunchedEffect(Unit) {
                    navController.getInput<Input>()?.let {
                        viewModel.enabledDefaultState = it.recoveryEnabled
                        viewModel.setEnabled(it.recoveryEnabled)
                    }
                }
                LaunchedEffect(viewModel.success.value) {
                    if (viewModel.success.value) {
                        navController.setNavigationResultX(Result(viewModel.enabled.value))
                        navController.popBackStack()
                    }
                }
                AccessCodeRecoveryScreen(
                    enabled = viewModel.enabled.value,
                    saveButtonEnabled = viewModel.enabled.value != viewModel.enabledDefaultState,
                    onSaveClicked = viewModel::saveChanges,
                    onEnableClick = viewModel::setEnabled,
                    onCloseClick = { close() }
                )
            }
        }
    }

    @Parcelize
    data class Input(val recoveryEnabled: Boolean) : Parcelable

    @Parcelize
    data class Result(val recoveryEnabled: Boolean) : Parcelable
}


@Composable
private fun AccessCodeRecoveryScreen(
    enabled: Boolean,
    saveButtonEnabled: Boolean,
    onEnableClick: (Boolean) -> Unit,
    onSaveClicked: () -> Unit,
    onCloseClick: () -> Unit
) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.icon_unlocked_48),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = stringResource(R.string.card_settings_access_code_recovery_title),
            onCloseClick = onCloseClick
        ) {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    onEnableClick(true)
                },
                verticalAlignment = Alignment.Top
            ) {
                HsCheckbox(
                    checked = enabled,
                    onCheckedChange = {
                        onEnableClick(true)
                    },
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    headline2_leah(
                        text = stringResource(R.string.enabled)
                    )
                    subhead2_grey(
                        text = stringResource(R.string.card_settings_access_code_recovery_enabled_description),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    onEnableClick(false)
                },
                verticalAlignment = Alignment.Top
            ) {
                HsCheckbox(
                    checked = !enabled,
                    onCheckedChange = {
                        onEnableClick(false)
                    },
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    headline2_leah(
                        text = stringResource(R.string.disabled)
                    )
                    subhead2_grey(
                        text = stringResource(R.string.card_settings_access_code_recovery_disabled_description),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                title = stringResource(R.string.save),
                onClick = onSaveClicked,
                enabled = saveButtonEnabled
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetToFactorySettingsScreenPreview() {
    ComposeAppTheme {
        AccessCodeRecoveryScreen(
            enabled = true,
            saveButtonEnabled = true,
            onSaveClicked = {},
            onCloseClick = {},
            onEnableClick = {}
        )
    }
}