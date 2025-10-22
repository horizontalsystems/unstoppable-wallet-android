package io.horizontalsystems.bankwallet.modules.watchaddress.selectblockchains

import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineClear
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

class SelectBlockchainsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            SelectBlockchainsScreen(
                input.accountType,
                input.accountName,
                navController,
                input.popOffOnSuccess,
                input.popOffInclusive
            )
        }
    }

    @Parcelize
    data class Input(
        val popOffOnSuccess: Int,
        val popOffInclusive: Boolean,
        val accountType: AccountType,
        val accountName: String?,
    ) : Parcelable

}

@Composable
private fun SelectBlockchainsScreen(
    accountType: AccountType,
    accountName: String?,
    navController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean
) {
    val viewModel = viewModel<SelectBlockchainsViewModel>(
        factory = SelectBlockchainsModule.Factory(
            accountType,
            accountName
        )
    )

    val view = LocalView.current
    val uiState = viewModel.uiState
    val title = uiState.title
    val accountCreated = uiState.accountCreated
    val submitEnabled = uiState.submitButtonEnabled
    val blockchainViewItems = uiState.coinViewItems

    LaunchedEffect(accountCreated) {
        if (accountCreated) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_AddressAdded,
                icon = R.drawable.icon_binocule_24,
                iconTint = R.color.white
            )
            delay(300)
            navController.popBackStack(popUpToInclusiveId, inclusive)
        }
    }

    HSScaffold(
        title = stringResource(title),
        onBack = navController::popBackStack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Done),
                onClick = viewModel::onClickWatch,
                enabled = submitEnabled,
                tint = ComposeAppTheme.colors.jacob
            )
        ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                HsDivider()
            }
            items(blockchainViewItems) { viewItem ->
                CellMultilineClear(
                    borderBottom = true,
                    onClick = { viewModel.onToggle(viewItem.item) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Image(
                            painter = viewItem.imageSource.painter(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                body_leah(
                                    text = viewItem.title,
                                    maxLines = 1,
                                )
                                viewItem.label?.let { labelText ->
                                    Badge(
                                        text = labelText,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                            subhead2_grey(
                                text = viewItem.subtitle,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        HsSwitch(
                            checked = viewItem.enabled,
                            onCheckedChange = { viewModel.onToggle(viewItem.item) },
                        )
                    }
                }
            }
        }
    }
}
