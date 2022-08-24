package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class BackupConfirmKeyFragment : BaseFragment() {

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
                RecoveryPhraseVerifyScreen(
                    findNavController(),
                    arguments?.getParcelable(BackupConfirmKeyModule.ACCOUNT)!!,
                )
            }
        }
    }
}

@Composable
fun RecoveryPhraseVerifyScreen(navController: NavController, account: Account) {
    val viewModel = viewModel<BackupConfirmKeyViewModel>(factory = BackupConfirmKeyModule.Factory(account))
    val uiState = viewModel.uiState
    val contenView = LocalView.current

    LaunchedEffect(uiState.confirmed) {
        if (uiState.confirmed) {
            HudHelper.showSuccessMessage(
                contenView = contenView,
                resId = R.string.Hud_Text_Verified,
                icon = R.drawable.icon_check_1_24,
                iconTint = R.color.white
            )
            delay(300)
            navController.popBackStack(R.id.backupKeyFragment, true)
        }
    }

    uiState.error?.message?.let {
        HudHelper.showErrorMessage(contenView, it)
        viewModel.onErrorShown()
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.RecoveryPhraseVerify_Title),
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
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                InfoText(text = stringResource(R.string.RecoveryPhraseVerify_Description))
                Spacer(Modifier.height(12.dp))

                uiState.hiddenWordItems.forEachIndexed { index, it ->
                    if (index != 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val borderColor = if (uiState.currentHiddenWordItemIndex == index) {
                        ComposeAppTheme.colors.yellow50
                    } else {
                        ComposeAppTheme.colors.steel20
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        body_leah(text = it.toString())
                    }
                }

                Spacer(Modifier.height(24.dp))

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    mainAxisAlignment = FlowMainAxisAlignment.Center,
                    crossAxisSpacing = 16.dp
                ) {
                    uiState.wordOptions.forEach { wordOption ->
                        Box(modifier = Modifier.height(28.dp)) {
                            ButtonSecondaryDefault(
                                title = wordOption.word,
                                enabled = wordOption.enabled,
                                onClick = {
                                    viewModel.onSelectWord(wordOption)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}
