package io.horizontalsystems.bankwallet.modules.manageaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.authorizedAction
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.backupkey.BackupKeyModule
import io.horizontalsystems.bankwallet.modules.balance.HeaderNote
import io.horizontalsystems.bankwallet.modules.balance.ui.NoteError
import io.horizontalsystems.bankwallet.modules.balance.ui.NoteWarning
import io.horizontalsystems.bankwallet.modules.evmprivatekey.EvmPrivateKeyModule
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule.ACCOUNT_ID_KEY
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountViewModel.KeyActionState
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.modules.recoveryphrase.RecoveryPhraseModule
import io.horizontalsystems.bankwallet.modules.showextendedkey.account.ShowExtendedKeyModule
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

                when (viewModel.headerNote) {
                    HeaderNote.NonStandardAccount -> {
                        NoteError(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp,  top = 32.dp),
                            text = stringResource(R.string.AccountRecovery_MigrationRequired),
                            onClick = {
                                openMarkDown(viewModel.getFaqUrl(HeaderNote.NonStandardAccount), navController)
                            }
                        )
                    }
                    HeaderNote.NonRecommendedAccount -> {
                        NoteWarning(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp),
                            text = stringResource(R.string.AccountRecovery_MigrationRecommended),
                            onClick = {
                                openMarkDown(viewModel.getFaqUrl(HeaderNote.NonRecommendedAccount), navController)
                            },
                            onClose = null
                        )
                    }
                    HeaderNote.None -> Unit
                }

                when (viewModel.keyActionState) {
                    KeyActionState.ShowRecoveryPhrase -> {
                        if (viewModel.showRecoveryPhrase) {
                            Spacer(modifier = Modifier.height(32.dp))
                            CellUniversalLawrenceSection(
                                listOf {
                                    AccountActionItem(
                                        title = stringResource(id = R.string.RecoveryPhrase_Title),
                                        icon = painterResource(id = R.drawable.icon_paper_contract_20)
                                    ) {
                                        navController.authorizedAction {
                                            navController.slideFromRight(
                                                R.id.recoveryPhraseFragment,
                                                RecoveryPhraseModule.prepareParams(viewModel.account)
                                            )
                                        }
                                    }
                                })
                        }
                        if (viewModel.showEvmPrivateKey) {
                            Spacer(modifier = Modifier.height(32.dp))
                            CellUniversalLawrenceSection(
                                listOf {
                                AccountActionItem(
                                    title = stringResource(id = R.string.EvmPrivateKey_Title),
                                    icon = painterResource(id = R.drawable.ic_key_20)
                                ) {
                                    navController.authorizedAction {
                                        navController.slideFromRight(
                                            R.id.evmPrivateKeyFragment,
                                            EvmPrivateKeyModule.prepareParams(viewModel.account)
                                        )
                                    }
                                }
                            })
                        }
                        val keyActions = buildList<@Composable () -> Unit> {
                            if (viewModel.bip32RootKey != null) {
                                add {
                                    AccountActionItem(
                                        title = stringResource(id = R.string.Bip32RootKey),
                                        icon = painterResource(id = R.drawable.ic_key_20)
                                    ) {
                                        navController.authorizedAction {
                                            navController.slideFromRight(
                                                R.id.accountExtendedKeyFragment,
                                                ShowExtendedKeyModule.prepareParams(
                                                    viewModel.bip32RootKey,
                                                    ShowExtendedKeyModule.DisplayKeyType.Bip32RootKey
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (viewModel.bip32RootKey != null || viewModel.accountExtendedPrivateKey != null) {
                                add {
                                    AccountActionItem(
                                        title = stringResource(id = R.string.AccountExtendedPrivateKey),
                                        icon = painterResource(id = R.drawable.ic_key_20)
                                    ) {
                                        navController.authorizedAction {
                                            if (viewModel.bip32RootKey != null) {
                                                navController.slideFromRight(
                                                    R.id.accountExtendedKeyFragment,
                                                    ShowExtendedKeyModule.prepareParams(
                                                        viewModel.bip32RootKey,
                                                        ShowExtendedKeyModule.DisplayKeyType.AccountPrivateKey(true)
                                                    )
                                                )
                                            } else if (viewModel.accountExtendedPrivateKey != null) {
                                                navController.slideFromRight(
                                                    R.id.accountExtendedKeyFragment,
                                                    ShowExtendedKeyModule.prepareParams(
                                                        viewModel.accountExtendedPrivateKey,
                                                        ShowExtendedKeyModule.DisplayKeyType.AccountPrivateKey(false)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (viewModel.bip32RootKey != null || viewModel.accountExtendedPublicKey != null || viewModel.accountExtendedPrivateKey != null) {
                                add {
                                    AccountActionItem(
                                        title = stringResource(id = R.string.AccountExtendedPublicKey),
                                        icon = painterResource(id = R.drawable.icon_link_20)
                                    ) {
                                        if (viewModel.bip32RootKey != null) {
                                            navController.slideFromRight(
                                                R.id.accountExtendedKeyFragment,
                                                ShowExtendedKeyModule.prepareParams(
                                                    viewModel.bip32RootKey,
                                                    ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey(true)
                                                )
                                            )
                                        } else if (viewModel.accountExtendedPublicKey != null) {
                                            navController.slideFromRight(
                                                R.id.accountExtendedKeyFragment,
                                                ShowExtendedKeyModule.prepareParams(
                                                    viewModel.accountExtendedPublicKey,
                                                    ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey(false)
                                                )
                                            )
                                        } else if (viewModel.accountExtendedPrivateKey != null) {
                                            navController.slideFromRight(
                                                R.id.accountExtendedKeyFragment,
                                                ShowExtendedKeyModule.prepareParams(
                                                    viewModel.accountExtendedPrivateKey,
                                                    ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey(false)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (keyActions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            CellUniversalLawrenceSection(keyActions)
                        }
                    }
                    KeyActionState.BackupRecoveryPhrase -> {
                        Spacer(modifier = Modifier.height(32.dp))
                        CellUniversalLawrenceSection(
                            listOf {
                                RedActionItem(
                                    title = stringResource(id = R.string.ManageAccount_RecoveryPhraseBackup),
                                    icon = painterResource(id = R.drawable.icon_warning_2_20)
                                ) {
                                    navController.authorizedAction {
                                        navController.slideFromBottom(
                                            R.id.backupKeyFragment,
                                            BackupKeyModule.prepareParams(viewModel.account)
                                        )
                                    }
                                }
                            })
                    }
                    KeyActionState.None -> Unit
                }

                Spacer(modifier = Modifier.height(32.dp))
                CellUniversalLawrenceSection(
                    listOf {
                        RedActionItem(
                            title = stringResource(id = R.string.ManageAccount_Unlink),
                            icon = painterResource(id = R.drawable.ic_delete_20)
                        ) {
                            navController.slideFromBottom(
                                R.id.unlinkConfirmationDialog,
                                UnlinkAccountDialog.prepareParams(viewModel.account)
                            )
                        }
                    })
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun openMarkDown(
    markDownUrl: String,
    navController: NavController
) {
    val arguments = bundleOf(
        MarkdownFragment.markdownUrlKey to markDownUrl,
        MarkdownFragment.handleRelativeUrlKey to true
    )
    navController.slideFromRight(
        R.id.markdownFragment,
        arguments
    )
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

    RowUniversal(
        onClick = onClick
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
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(20.dp),
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

@Composable
private fun RedActionItem(
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {

    RowUniversal(
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = icon,
            contentDescription = null,
            tint = ComposeAppTheme.colors.lucian
        )

        body_lucian(
            text = title,
        )
    }
}
