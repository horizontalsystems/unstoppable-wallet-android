package io.horizontalsystems.bankwallet.modules.manageaccount.backupconfirmkey

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class BackupConfirmKeyFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Account>(navController) { input ->
            RecoveryPhraseVerifyScreen(navController, input)
        }
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecoveryPhraseVerifyScreen(navController: NavController, account: Account) {
    val viewModel =
        viewModel<BackupConfirmKeyViewModel>(factory = BackupConfirmKeyModule.Factory(account))
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

    HSScaffold(
        title = stringResource(R.string.RecoveryPhraseVerify_Title),
        onBack = { navController.popBackStack() },
    ) {
        Column {
            InfoText(text = stringResource(R.string.RecoveryPhraseVerify_Description))
            Spacer(Modifier.height(12.dp))

            uiState.hiddenWordItems.forEachIndexed { index, it ->
                if (index != 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val borderColor = if (uiState.currentHiddenWordItemIndex == index) {
                    ComposeAppTheme.colors.yellow50
                } else {
                    ComposeAppTheme.colors.blade
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    body_leah(text = it.toString())
                }
            }

            VSpacer(8.dp)

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalArrangement = Arrangement.Center
            ) {
                uiState.wordOptions.forEach { wordOption ->
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        ButtonSecondaryDefault(
                            title = wordOption.word,
                            enabled = wordOption.enabled,
                            onClick = {
                                viewModel.onSelectWord(wordOption)
                            }
                        )
                    }
                }
            }
        }
    }
}
