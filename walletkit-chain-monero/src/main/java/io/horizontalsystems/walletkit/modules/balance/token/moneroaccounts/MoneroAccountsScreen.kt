package io.horizontalsystems.walletkit.modules.balance.token.moneroaccounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IMoneroAccountsAdapter
import io.horizontalsystems.walletkit.core.MoneroAccountInfo
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.FormsInput
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.InfoText
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.walletkit.ui.compose.components.headline2_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class MoneroAccountsPage(val input: Input) : HSPage() {

    @Serializable
    data class Input(val wallet: Wallet)

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        MoneroAccountsScreen(
            wallet = input.wallet,
            onBack = { navigation.removeLastOrNull() }
        )
    }
}

private sealed class AccountNameSheetMode {
    object Create : AccountNameSheetMode()
    data class Rename(val account: MoneroAccountInfo) : AccountNameSheetMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneroAccountsScreen(
    wallet: Wallet,
    onBack: () -> Unit
) {
    val viewModel: MoneroAccountsViewModel = viewModel(
        factory = MoneroAccountsViewModel.Factory(wallet)
    )
    val uiState = viewModel.uiState
    val coinCode = wallet.coin.code
    val coinDecimals = wallet.token.decimals

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sheetMode by remember { mutableStateOf<AccountNameSheetMode?>(null) }

    HSScaffold(
        title = stringResource(R.string.Monero_Accounts),
        onBack = onBack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Monero_NewAccount),
                icon = R.drawable.ic_plus,
                onClick = { sheetMode = AccountNameSheetMode.Create }
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)
            CellUniversalLawrenceSection(uiState.accounts) { account ->
                AccountCell(
                    account = account,
                    active = account.index == uiState.activeAccountIndex,
                    balanceText = App.numberFormatter.formatCoinFull(account.unlocked, coinCode, coinDecimals),
                    onClick = { viewModel.onSelectAccount(account.index) },
                    onClickEdit = { sheetMode = AccountNameSheetMode.Rename(account) }
                )
            }
            InfoText(
                text = stringResource(R.string.Monero_Accounts_Description),
            )
        }
    }

    sheetMode?.let { mode ->
        BottomSheetContent(
            onDismissRequest = { sheetMode = null },
            sheetState = sheetState
        ) {
            key(mode) {
                AccountNameSheet(
                    mode = mode,
                    onConfirm = { name ->
                        when (mode) {
                            is AccountNameSheetMode.Create -> viewModel.onCreateAccount(name.ifBlank { null })
                            is AccountNameSheetMode.Rename -> viewModel.onRenameAccount(mode.account.index, name)
                        }
                        scope.launch {
                            sheetState.hide()
                            sheetMode = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountNameSheet(
    mode: AccountNameSheetMode,
    onConfirm: (String) -> Unit
) {
    val titleRes = when (mode) {
        is AccountNameSheetMode.Create -> R.string.Monero_NewAccount
        is AccountNameSheetMode.Rename -> R.string.Monero_RenameAccount
    }
    var name by rememberSaveable {
        mutableStateOf((mode as? AccountNameSheetMode.Rename)?.account?.label ?: "")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        VSpacer(12.dp)
        headline2_leah(
            text = stringResource(titleRes),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        VSpacer(12.dp)
        FormsInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            initial = (mode as? AccountNameSheetMode.Rename)?.account?.label,
            hint = stringResource(R.string.Monero_AccountName),
            singleLine = true,
            pasteEnabled = false,
            maxLength = 40,
            onValueChange = { name = it }
        )
        VSpacer(24.dp)
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = when (mode) {
                is AccountNameSheetMode.Create -> stringResource(R.string.Button_Create)
                is AccountNameSheetMode.Rename -> stringResource(R.string.Button_Save)
            },
            enabled = mode is AccountNameSheetMode.Create || name.isNotBlank(),
            onClick = { onConfirm(name) }
        )
        VSpacer(32.dp)
    }
}

@Composable
private fun AccountCell(
    account: MoneroAccountInfo,
    active: Boolean,
    balanceText: String,
    onClick: () -> Unit,
    onClickEdit: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        onClick = onClick
    ) {
        Column(modifier = Modifier.weight(1f)) {
            body_leah(text = "${account.index}. ${account.label}")
            subhead2_grey(text = balanceText)
        }
        if (active) {
            HSpacer(8.dp)
            Icon(
                painter = painterResource(R.drawable.ic_checkmark_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.jacob,
                modifier = Modifier.size(20.dp)
            )
        }
        HSpacer(16.dp)
        ButtonSecondaryCircle(
            icon = R.drawable.ic_edit_20,
            onClick = onClickEdit
        )
    }
}

class MoneroAccountsViewModel(
    private val adapter: IMoneroAccountsAdapter,
) : ViewModelUiState<MoneroAccountsUiState>() {

    private var accounts = adapter.accountsFlow.value
    private var activeAccountIndex = adapter.activeAccountFlow.value

    init {
        viewModelScope.launch {
            adapter.accountsFlow.collect {
                accounts = it

                emitState()
            }
        }
        viewModelScope.launch {
            adapter.activeAccountFlow.collect {
                activeAccountIndex = it

                emitState()
            }
        }
    }

    override fun createState() = MoneroAccountsUiState(
        accounts = accounts,
        activeAccountIndex = activeAccountIndex,
    )

    fun onSelectAccount(index: Int) {
        adapter.activeAccount = index
    }

    fun onCreateAccount(label: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { adapter.createAccount(label) }
        }
    }

    fun onRenameAccount(index: Int, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { adapter.renameAccount(index, label) }
        }
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet<IMoneroAccountsAdapter>(wallet)
                ?: throw IllegalStateException("IMoneroAccountsAdapter is null")
            return MoneroAccountsViewModel(adapter) as T
        }
    }
}

data class MoneroAccountsUiState(
    val accounts: List<MoneroAccountInfo>,
    val activeAccountIndex: Int,
)
