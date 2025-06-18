package cash.p.terminal.modules.manageaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.core.managers.FaqManager
import cash.p.terminal.modules.balance.HeaderNote
import cash.p.terminal.modules.balance.ui.NoteError
import cash.p.terminal.modules.balance.ui.NoteWarning
import cash.p.terminal.modules.manageaccount.ManageAccountModule.BackupItem
import cash.p.terminal.modules.manageaccount.ManageAccountModule.KeyAction
import cash.p.terminal.modules.resettofactorysettings.ResetToFactorySettingsFragment
import cash.p.terminal.modules.settings.main.HsSettingCell
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsImage
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_jacob
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.body_lucian
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.slideFromBottom
import io.horizontalsystems.core.slideFromBottomForResult

@Composable
internal fun ManageAccountScreen(
    navController: NavController,
    viewState: ManageAccountModule.ViewState,
    account: Account,
    onCloseClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    deleteAccount: () -> Unit,
    onNameChanged: (String) -> Unit,
    accessCodeRecovery: () -> Unit,
    onChangeAccessCode: () -> Unit
) {
    if (viewState.closeScreen) {
        navController.popBackStack()
        onCloseClicked()
    }

    Column(modifier = Modifier.Companion.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = viewState.title,
            navigationIcon = {
                HsBackButton(onClick = { navController.popBackStack() })
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.ManageAccount_Save),
                    onClick = onSaveClicked,
                    enabled = viewState.canSave
                )
            )
        )

        Column {
            HeaderText(stringResource(id = R.string.ManageAccount_Name))

            FormsInput(
                modifier = Modifier.Companion.padding(horizontal = 16.dp),
                initial = viewState.title,
                hint = "",
                onValueChange = onNameChanged
            )

            viewState.signedHashes?.let {
                VSpacer(32.dp)
                CellUniversalLawrenceSection(
                    listOf {
                        HsSettingCell(
                            title = R.string.signed,
                            icon = R.drawable.ic_info_20,
                            value = stringResource(
                                R.string.details_row_subtitle_signed_hashes_format,
                                it
                            ),
                        )
                    }
                )
            }

            when (viewState.headerNote) {
                HeaderNote.NonStandardAccount -> {
                    NoteError(
                        modifier = Modifier.Companion.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 32.dp
                        ),
                        text = stringResource(R.string.AccountRecovery_MigrationRequired),
                        onClick = {
                            FaqManager.showFaqPage(
                                navController,
                                FaqManager.faqPathMigrationRequired
                            )
                        }
                    )
                }

                HeaderNote.NonRecommendedAccount -> {
                    NoteWarning(
                        modifier = Modifier.Companion.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 32.dp
                        ),
                        text = stringResource(R.string.AccountRecovery_MigrationRecommended),
                        onClick = {
                            FaqManager.showFaqPage(
                                navController,
                                FaqManager.faqPathMigrationRecommended
                            )
                        },
                        onClose = null
                    )
                }

                HeaderNote.None -> Unit
            }

            KeyActions(
                viewState = viewState,
                account = account,
                deleteAccount = deleteAccount,
                changeAccessCode = onChangeAccessCode,
                onAccessCodeRecoveryClick = accessCodeRecovery,
                navController = navController
            )

            if (viewState.backupActions.isNotEmpty()) {
                BackupActions(
                    viewState.backupActions,
                    account,
                    navController
                )
            }

            VSpacer(32.dp)
            CellUniversalLawrenceSection(
                listOf {
                    RedActionItem(
                        title = stringResource(id = R.string.ManageAccount_Unlink),
                        icon = painterResource(id = R.drawable.ic_delete_20)
                    ) {
                        navController.slideFromBottom(
                            R.id.unlinkConfirmationDialog,
                            account
                        )
                    }
                })
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun KeyActions(
    viewState: ManageAccountModule.ViewState,
    account: Account,
    deleteAccount: () -> Unit,
    onAccessCodeRecoveryClick: () -> Unit,
    changeAccessCode: () -> Unit,
    navController: NavController
) {
    val actionItems = mutableListOf<@Composable () -> Unit>()

    viewState.keyActions.forEach { keyAction ->
        when (keyAction) {
            KeyAction.RecoveryPhrase -> {
                actionItems.add {
                    AccountActionItem(
                        title = stringResource(id = R.string.RecoveryPhrase_Title),
                        icon = painterResource(id = R.drawable.icon_paper_contract_20)
                    ) {
                        navController.authorizedAction {
                            navController.slideFromRight(
                                R.id.recoveryPhraseFragment,
                                account
                            )
                        }
                    }
                }
            }

            KeyAction.PrivateKeys -> {
                actionItems.add {
                    AccountActionItem(
                        title = stringResource(id = R.string.PrivateKeys_Title),
                        icon = painterResource(id = R.drawable.ic_key_20)
                    ) {
                        navController.slideFromRight(
                            R.id.privateKeysFragment,
                            account
                        )
                    }
                }
            }

            KeyAction.PublicKeys -> {
                actionItems.add {
                    AccountActionItem(
                        title = stringResource(id = R.string.PublicKeys_Title),
                        icon = painterResource(id = R.drawable.icon_binocule_20)
                    ) {
                        navController.slideFromRight(
                            R.id.publicKeysFragment,
                            account
                        )
                    }
                }
            }

            KeyAction.ResetToFactorySettings -> {
                actionItems.add {
                    AccountActionItem(
                        title = stringResource(id = R.string.reset_to_factory_settings),
                        icon = painterResource(id = R.drawable.ic_delete_20)
                    ) {
                        navController.slideFromBottomForResult<ResetToFactorySettingsFragment.Result>(
                            resId = R.id.resetToFactorySettingsFragment,
                            input = ResetToFactorySettingsFragment.Input(account)
                        ) {
                            if (it.success) {
                                deleteAccount()
                            }
                        }
                    }
                }
            }

            KeyAction.AccessCodeRecovery -> {
                actionItems.add {
                    AccountActionItem(
                        title = stringResource(id = R.string.card_settings_access_code_recovery_title),
                        icon = painterResource(id = R.drawable.icon_unlocked_48)
                    ) {
                        onAccessCodeRecoveryClick()
                    }
                }
            }

            KeyAction.ChangeAccessCode -> {
                actionItems.add {
                    AccountActionItem(
                        title = stringResource(id = R.string.change_access_code),
                        icon = painterResource(id = R.drawable.ic_key_20)
                    ) {
                        changeAccessCode()
                    }
                }
            }
        }
    }

    if (actionItems.isNotEmpty()) {
        VSpacer(32.dp)
        CellUniversalLawrenceSection(actionItems)
    }
}

@Composable
private fun BackupActions(
    backupActions: List<BackupItem>,
    account: Account,
    navController: NavController
) {
    val actionItems = mutableListOf<@Composable () -> Unit>()
    val infoItems = mutableListOf<@Composable () -> Unit>()

    backupActions.forEach { action ->
        when (action) {
            is BackupItem.ManualBackup -> {
                actionItems.add {
                    YellowActionItem(
                        title = stringResource(id = R.string.ManageAccount_RecoveryPhraseBackup),
                        icon = painterResource(id = R.drawable.ic_edit_24),
                        attention = action.showAttention,
                        completed = action.completed
                    ) {
                        navController.authorizedAction {
                            navController.slideFromBottom(
                                R.id.backupKeyFragment,
                                account
                            )
                        }
                    }
                }
            }

            is BackupItem.LocalBackup -> {
                actionItems.add {
                    YellowActionItem(
                        title = stringResource(id = R.string.ManageAccount_LocalBackup),
                        icon = painterResource(id = R.drawable.ic_file_24),
                        attention = action.showAttention
                    ) {
                        navController.authorizedAction {
                            navController.slideFromBottom(R.id.backupLocalFragment, account)
                        }
                    }
                }
            }

            is BackupItem.InfoText -> {
                infoItems.add {
                    InfoText(text = stringResource(action.textRes))
                }
            }
        }
    }
    if (actionItems.isNotEmpty()) {
        VSpacer(32.dp)
        CellUniversalLawrenceSection(actionItems)
    }
    infoItems.forEach {
        it.invoke()
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
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(24.dp),
            painter = icon,
            contentDescription = null,
            tint = ComposeAppTheme.colors.lucian
        )

        body_lucian(
            text = title,
        )
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
    RowUniversal(
        onClick = onClick
    ) {
        icon?.let {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
                painter = icon,
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }

        if (coinIconUrl != null) {
            HsImage(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(20.dp),
                url = coinIconUrl,
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
            HSpacer(16.dp)
        }
    }
}

@Composable
private fun YellowActionItem(
    title: String,
    icon: Painter? = null,
    attention: Boolean = false,
    completed: Boolean = false,
    onClick: (() -> Unit)? = null
) {

    RowUniversal(
        onClick = onClick
    ) {
        icon?.let {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
                painter = icon,
                contentDescription = null,
                tint = ComposeAppTheme.colors.jacob
            )
        }

        body_jacob(
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
            HSpacer(6.dp)
        } else if (completed) {
            Icon(
                modifier = Modifier.padding(horizontal = 16.dp),
                painter = painterResource(id = R.drawable.ic_checkmark_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.remus
            )
            HSpacer(6.dp)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ManageAccountScreenPreview() {
    ComposeAppTheme {
        ManageAccountScreen(
            navController = rememberNavController(),
            viewState = ManageAccountModule.ViewState(
                title = "Account name",
                newName = "Account name",
                canSave = true,
                closeScreen = false,
                headerNote = HeaderNote.None,
                keyActions = listOf(
                    KeyAction.AccessCodeRecovery,
                    KeyAction.ChangeAccessCode,
                    KeyAction.ResetToFactorySettings
                ),
                backupActions = emptyList(),
                signedHashes = 2
            ),
            account = Account(
                id = "id",
                name = "name",
                type = AccountType.HardwareCard(
                    cardId = "",
                    backupCardsCount = 0,
                    walletPublicKey = "",
                    signedHashes = 2
                ),
                origin = AccountOrigin.Created,
                level = 0
            ),
            onCloseClicked = {},
            onSaveClicked = {},
            onNameChanged = {},
            deleteAccount = {},
            onChangeAccessCode = {},
            accessCodeRecovery = {}
        )
    }
}