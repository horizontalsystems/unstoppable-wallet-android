package io.horizontalsystems.bankwallet.modules.manageaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule.ACCOUNT_ID_KEY
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountViewModel.KeyActionState
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.UnlinkConfirmationDialog
import io.horizontalsystems.bankwallet.modules.networksettings.NetworkSettingsModule
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class ManageAccountFragment : BaseFragment() {

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
                ManageAccountScreen(findNavController(), arguments?.getString(ACCOUNT_ID_KEY)!!)
            }
        }
    }
}

@Composable
fun ManageAccountScreen(navController: NavController, accountId: String) {
    val viewModel = viewModel<ManageAccountViewModel>(factory = ManageAccountModule.Factory(accountId))

    val saveEnabled by viewModel.saveEnabledLiveData.observeAsState(false)
    val keyActionState by viewModel.keyActionStateLiveData.observeAsState()
    val additionalViewItems by viewModel.additionalViewItemsLiveData.observeAsState(listOf())
    val finish by viewModel.finishLiveEvent.observeAsState()

    if (finish != null) {
        navController.popBackStack()
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.PlainString(viewModel.account.name),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.ManageAccount_Save),
                        onClick = {
                            viewModel.onSave()
                        },
                        enabled = saveEnabled
                    )
                )
            )

            Column {
                Header {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(id = R.string.ManageAccount_Name),
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead1
                    )
                }

                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = viewModel.account.name,
                    hint = "",
                    onValueChange = {
                        viewModel.onChange(it)
                    }
                )

                val actionItems = mutableListOf<@Composable () -> Unit>()

                when (keyActionState) {
                    KeyActionState.ShowRecoveryPhrase -> {
                        actionItems.add {
                            AccountActionItem(
                                title = stringResource(id = R.string.ManageAccount_RecoveryPhraseShow),
                                icon = painterResource(id = R.drawable.ic_key_20)
                            ) {
                                navController.slideFromRight(
                                    R.id.manageAccountFragment_to_showKeyFragment,
                                    ShowKeyModule.prepareParams(viewModel.account)
                                )
                            }
                        }
                    }
                    KeyActionState.BackupRecoveryPhrase -> {
                        actionItems.add {
                            AccountActionItem(
                                title = stringResource(id = R.string.ManageAccount_RecoveryPhraseBackup),
                                icon = painterResource(id = R.drawable.ic_key_20),
                                attention = true
                            ) {
                                navController.slideFromRight(
                                    R.id.manageAccountFragment_to_backupKeyFragment,
                                    BackupKeyModule.prepareParams(viewModel.account)
                                )

                            }
                        }
                    }
                }
                actionItems.add {
                    AccountActionItem(
                        title = stringResource(id = R.string.ManageAccount_NetworkSettings),
                        icon = painterResource(id = R.drawable.ic_blocks)
                    ) {
                        navController.slideFromRight(
                            R.id.manageAccountFragment_to_networkSettingsFragment,
                            NetworkSettingsModule.prepareParams(viewModel.account)
                        )
                    }
                }

                additionalViewItems.forEach { additionViewItem ->
//                    val platformCoin = additionViewItem.platformCoin
//                    setRemoteImage(
//                        platformCoin.coin.iconUrl,
//                        platformCoin.coinType.iconPlaceholder
//                    )

                    actionItems.add {
                        AccountActionItem(
                            title = additionViewItem.title,
                            icon = painterResource(id = R.drawable.ic_blocks),
                            badge = additionViewItem.value
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                CellSingleLineLawrenceSection(actionItems)

                Spacer(modifier = Modifier.height(32.dp))
                CellSingleLineLawrenceSection(listOf {
                    CellSingleLineLawrence {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    navController.slideFromBottom(
                                        R.id.unlinkConfirmationDialog,
                                        UnlinkConfirmationDialog.prepareParams(viewModel.account)
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(id = R.drawable.ic_delete_20),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.lucian
                            )

                            Text(
                                text = stringResource(id = R.string.ManageAccount_Unlink),
                                color = ComposeAppTheme.colors.lucian,
                                style = ComposeAppTheme.typography.body
                            )
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun AccountActionItem(
    title: String,
    icon: Painter,
    attention: Boolean = false,
    badge: String? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = icon,
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah
        )

        if (attention) {
            Icon(
                modifier = Modifier.padding(horizontal = 16.dp),
                painter = painterResource(id = R.drawable.ic_attention_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.lucian
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        badge?.let {
            val view = LocalView.current
            val clipboardManager = LocalClipboardManager.current

            ButtonSecondaryDefault(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = it,
                onClick = {
                    clipboardManager.setText(AnnotatedString(it))
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                }
            )
        }

        onClick?.let {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}
