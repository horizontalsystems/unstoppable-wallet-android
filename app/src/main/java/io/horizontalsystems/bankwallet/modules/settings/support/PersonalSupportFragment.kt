package io.horizontalsystems.bankwallet.modules.settings.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class PersonalSupportFragment : BaseFragment() {

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
                    PersonalSupportScreen(
                        findNavController()
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalSupportScreen(navController: NavController) {
    val viewModel = viewModel<PersonalSupportViewModel>(factory = PersonalSupportModule.Factory())
    val uiState = viewModel.uiState
    val view = LocalView.current

    LaunchedEffect(uiState.showError) {
        if (uiState.showError) {
            HudHelper.showErrorMessage(view, R.string.Settings_PersonalSupport_Requestfailed)
            viewModel.errorShown()
        }
    }
    LaunchedEffect(uiState.showSuccess) {
        if (uiState.showSuccess) {
            HudHelper.showSuccessMessage(view, R.string.Settings_PersonalSupport_Requested)
            viewModel.successShown()
        }
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Settings_PersonalSupport),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                )
            }
        ) { paddingValues ->
            if (uiState.showRequestForm) {
                RequestForm(paddingValues, viewModel, uiState)
            } else {
                SupportEnabled(paddingValues, viewModel)
            }
        }
    }
}

@Composable
private fun SupportEnabled(
    paddingValues: PaddingValues,
    viewModel: PersonalSupportViewModel,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(paddingValues)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = ComposeAppTheme.colors.raina,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(R.drawable.ic_support_24),
                    contentDescription = "",
                    tint = ComposeAppTheme.colors.grey
                )
            }
            Spacer(Modifier.height(32.dp))
            subhead2_grey(
                text = stringResource(R.string.Settings_PersonalSupport_YouAlreadyRequestedSupportDescription),
                modifier = Modifier.padding(horizontal = 48.dp)
            )
            Spacer(Modifier.height(32.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                title = stringResource(R.string.Settings_PersonalSupport_OpenTelegram),
                onClick = {
                    context.packageManager.getLaunchIntentForPackage("org.telegram.messenger")?.let {
                        context.startActivity(it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                title = stringResource(R.string.Settings_PersonalSupport_NewRequest),
                onClick = {
                    viewModel.showRequestForm()
                }
            )
        }
    }
}

@Composable
private fun RequestForm(
    paddingValues: PaddingValues,
    viewModel: PersonalSupportViewModel,
    uiState: PersonalSupportModule.UiState
) {
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.padding(paddingValues)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {
            VSpacer(12.dp)
            HeaderText(text = stringResource(R.string.Settings_PersonalSupport_Account).uppercase())
            FormsInput(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = 16.dp),
                pasteEnabled = false,
                hint = stringResource(R.string.Settings_PersonalSupport_UsernameHint),
                onValueChange = viewModel::onUsernameChange
            )
            InfoText(text = stringResource(R.string.Settings_PersonalSupport_EnterTelegramAccountDescription))
            VSpacer(32.dp)
        }
        ButtonsGroupWithShade {
            ButtonPrimaryYellowWithSpinner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                title = stringResource(R.string.Settings_PersonalSupport_Request),
                showSpinner = uiState.showSpinner,
                enabled = uiState.buttonEnabled,
                onClick = {
                    viewModel.onRequestClicked()
                },
            )
        }
    }
}
