package io.horizontalsystems.bankwallet.modules.watchaddress.selectblockchains

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineClear
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class SelectBlockchainsFragment : BaseFragment() {
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
                    val popUpToInclusiveId =
                        arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.selectBlockchainsFragment) ?: R.id.selectBlockchainsFragment
                    val inclusive =
                        arguments?.getBoolean(ManageAccountsModule.popOffInclusiveKey) ?: false
                    val accountType = arguments?.getParcelable<AccountType>(SelectBlockchainsModule.accountTypeKey)
                    val accountName = arguments?.getString(SelectBlockchainsModule.accountNameKey)
                    if (accountType != null) {
                        SelectBlockchainsScreen(
                            accountType,
                            accountName,
                            findNavController(),
                            popUpToInclusiveId,
                            inclusive
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectBlockchainsScreen(
    accountType: AccountType,
    accountName: String?,
    navController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean
) {
    val viewModel = viewModel<SelectBlockchainsViewModel>(factory = SelectBlockchainsModule.Factory(accountType, accountName))

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

    Column(
        modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            title = TranslatableString.ResString(title),
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Watch_Address_Watch),
                    onClick = viewModel::onClickWatch,
                    enabled = submitEnabled
                )
            ),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
                )
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
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ComposeAppTheme.colors.jeremy)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(
                                                start = 4.dp,
                                                end = 4.dp,
                                                bottom = 1.dp
                                            ),
                                            text = labelText,
                                            color = ComposeAppTheme.colors.bran,
                                            style = ComposeAppTheme.typography.microSB,
                                            maxLines = 1,
                                        )
                                    }
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
