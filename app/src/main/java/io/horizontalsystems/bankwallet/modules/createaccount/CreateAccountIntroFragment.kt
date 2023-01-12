package io.horizontalsystems.bankwallet.modules.createaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class CreateAccountIntroFragment : BaseFragment() {

    private val nightMode by lazy { AppCompatDelegate.getDefaultNightMode() == MODE_NIGHT_YES }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                val popUpToInclusiveId =
                    arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.createAccountIntroFragment) ?: R.id.createAccountIntroFragment
                CreateAccountIntroScreen(findNavController(), popUpToInclusiveId, nightMode)
            }
        }
    }
}

@Composable
private fun CreateAccountIntroScreen(navController: NavController, popUpToInclusiveId: Int, nightMode: Boolean) {
    val viewModel = viewModel<CreateAccountViewModel>(factory = CreateAccountModule.Factory())
    val view = LocalView.current

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = it,
                icon = R.drawable.icon_add_to_wallet_24,
                iconTint = R.color.white
            )
            delay(300)

            navController.popBackStack(popUpToInclusiveId, true)
            viewModel.onSuccessMessageShown()
        }
    }

    ComposeAppTheme {
        Box {
            Image(
                painter = painterResource(if (nightMode) R.drawable.ic_intro_background else R.drawable.ic_intro_background_light),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(Modifier.fillMaxSize()) {
            AppBar(
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                backgroundColor = Color.Transparent
            )
            Spacer(Modifier.weight(1f))

            title2_leah(
                modifier = Modifier.padding(horizontal = 48.dp),
                text = stringResource(R.string.CreateWallet_Intro_Title),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.Button_Create),
                onClick = {
                    viewModel.createAccount()
                }
            )
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.Button_Advanced),
                onClick = {
                    navController.slideFromRight(R.id.createAccountFragment, ManageAccountsModule.prepareParams(popUpToInclusiveId))
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
