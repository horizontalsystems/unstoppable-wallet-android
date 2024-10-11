package io.horizontalsystems.bankwallet.modules.tonconnect

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.DropDownCell
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.TitleValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorDialogCompose
import io.horizontalsystems.bankwallet.ui.compose.components.SelectorItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

@Composable
fun TonConnectNewScreen(navController: NavController, requestEntity: DAppRequestEntity) {
    val viewModel = viewModel<TonConnectNewViewModel>(initializer = {
        TonConnectNewViewModel(requestEntity)
    })

    val context = LocalContext.current
    val uiState = viewModel.uiState

    LaunchedEffect(uiState.finish) {
        if (uiState.finish) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.toast) {
        uiState.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onToastShow()
        }
    }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.TonConnect_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
        },
        bottomBar = {
            ButtonsGroupWithShade {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Connect),
                        onClick = viewModel::connect,
                        enabled = uiState.connectEnabled
                    )
                    VSpacer(16.dp)
                    ButtonPrimaryDefault(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Cancel),
                        onClick = viewModel::reject
                    )
                }
            }
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Row(
                modifier = Modifier.padding(
                    top = 12.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(15.dp)),
                    painter = rememberAsyncImagePainter(
                        model = uiState.manifest?.iconUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_24)
                    ),
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = uiState.manifest?.name ?: "",
                    style = ComposeAppTheme.typography.headline1,
                    color = ComposeAppTheme.colors.leah
                )
            }

            var showSortTypeSelectorDialog by remember { mutableStateOf(false) }
            if (showSortTypeSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.TonConnect_ChooseWallet),
                    items = uiState.accounts.map { account ->
                        SelectorItem(
                            title = account.name,
                            selected = account == uiState.account,
                            item = account,
                        )
                    },
                    onDismissRequest = {
                        showSortTypeSelectorDialog = false
                    },
                    onSelectItem = viewModel::onSelectAccount
                )
            }


            CellUniversalLawrenceSection(
                buildList<@Composable () -> Unit> {
                    add {
                        val url = uiState.manifest?.host ?: ""
                        TitleValueCell(stringResource(R.string.TonConnect_Url), url)
                    }
                    add {
                        DropDownCell(
                            stringResource(R.string.TonConnect_Wallet),
                            uiState.account?.name ?: stringResource(R.string.TonConnect_ChooseWallet),
                            enabled = true,
                            onSelect = {
                                showSortTypeSelectorDialog = true
                            }
                        )
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            if (uiState.error != null) {
                TextImportantError(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = uiState.error.message?.nullIfBlank() ?: uiState.error.javaClass.simpleName
                )
            } else {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.WalletConnect_Approve_Hint)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

fun String.nullIfBlank(): String? {
    if (this.isBlank()) return null
    return this
}
