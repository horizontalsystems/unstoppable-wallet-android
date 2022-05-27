package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration

class AddTokenFragment : BaseFragment() {

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
                AddTokenScreen(findNavController())
            }
        }
    }
}

@Composable
private fun AddTokenScreen(
    navController: NavController,
    viewModel: AddTokenViewModel = viewModel(factory = AddTokenModule.Factory())
) {
    if (viewModel.closeScreen) {
        navController.popBackStack()
    }

    if (viewModel.showSuccessMessage) {
        HudHelper.showSuccessMessage(
            LocalView.current, R.string.Hud_Text_Done, SnackbarDuration.LONG
        )
    }

    val dots = stringResource(R.string.AddToken_Dots)

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.AddToken_Title),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    enabled = false,
                    hint = stringResource(R.string.AddToken_AddressOrSymbol),
                    state = getState(viewModel.caution, viewModel.loading),
                    qrScannerEnabled = true,
                ) {
                    viewModel.onTextChange(it)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    TitleValueCell(
                        R.string.AddToken_CoinTypes,
                        viewModel.viewItem?.coinType ?: dots,
                        false
                    )
                    TitleValueCell(
                        R.string.AddToken_CoinName,
                        viewModel.viewItem?.coinName ?: dots
                    )
                    TitleValueCell(
                        R.string.AddToken_Symbol,
                        viewModel.viewItem?.symbol ?: dots
                    )
                    TitleValueCell(
                        R.string.AddToken_Decimals,
                        viewModel.viewItem?.decimals?.toString() ?: dots
                    )
                }
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(R.string.Button_Add),
                    onClick = { viewModel.onAddClick() },
                    enabled = viewModel.buttonEnabled
                )
            }
        }
    }
}

@Composable
private fun TitleValueCell(@StringRes title: Int, value: String, showTopBorder: Boolean = true) {
    if (showTopBorder) {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
    }
    Row(
        modifier = Modifier
            .background(ComposeAppTheme.colors.lawrence)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(title),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
        Text(
            text = value,
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.subhead1
        )
    }
}

private fun getState(caution: Caution?, loading: Boolean) = when (caution?.type) {
    Caution.Type.Error -> DataState.Error(Exception(caution.text))
    Caution.Type.Warning -> DataState.Error(FormsInputStateWarning(caution.text))
    null -> if (loading) DataState.Loading else null
}
