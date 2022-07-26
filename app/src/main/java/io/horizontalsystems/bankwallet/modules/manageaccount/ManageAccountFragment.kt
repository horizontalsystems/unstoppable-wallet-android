package io.horizontalsystems.bankwallet.modules.manageaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
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
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule.ACCOUNT_ID_KEY
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountViewModel.KeyActionState
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule
import io.horizontalsystems.bankwallet.modules.unlinkaccount.UnlinkAccountDialog
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
                    HsIconButton(onClick = { navController.popBackStack() }) {
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
                HeaderText(stringResource(id = R.string.ManageAccount_Name))

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
                                navController.slideFromBottom(
                                    R.id.showKeyFragment,
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
                                navController.slideFromBottom(
                                    R.id.backupKeyFragment,
                                    BackupKeyModule.prepareParams(viewModel.account)
                                )

                            }
                        }
                    }
                    KeyActionState.None -> Unit
                }

                additionalViewItems.forEach { additionViewItem ->
                    val token = additionViewItem.token
                    actionItems.add {
                        AccountActionItem(
                            title = additionViewItem.title,
                            coinIconUrl = token.coin.iconUrl,
                            coinIconPlaceholder = token.iconPlaceholder,
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
                                        UnlinkAccountDialog.prepareParams(viewModel.account)
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

                            body_lucian(text = stringResource(id = R.string.ManageAccount_Unlink))
                        }
                    }
                })
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AccountActionItem(
    title: String,
    icon: Painter? = null,
    coinIconUrl: String? = null,
    coinIconPlaceholder: Int? = null,
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
        icon?.let {
            Icon(
                modifier = Modifier.padding(horizontal = 16.dp),
                painter = icon,
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }

        if (coinIconUrl != null) {
            CoinImage(
                modifier = Modifier.padding(horizontal = 16.dp).size(20.dp),
                iconUrl = coinIconUrl,
                placeholder = coinIconPlaceholder
            )
        }

        body_leah(
            modifier = Modifier.weight(1f),
            text = title,
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
