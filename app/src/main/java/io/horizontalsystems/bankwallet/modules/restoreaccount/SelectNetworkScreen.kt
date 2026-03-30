package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

@Composable
fun SelectNetworkScreen(
    mainViewModel: RestoreViewModel,
    openSelectCoinsScreen: () -> Unit,
    onBackClick: () -> Unit
) {
    HSScaffold(
        title = stringResource(R.string.Restore_SelectNetwork_Title),
        onBack = onBackClick
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            mainViewModel.accountTypes.forEachIndexed { index, type ->
                BoxBordered(
                    top = index != 0
                ) {
                    CellPrimary(
                        middle = {
                            CellMiddleInfo(
                                title = title(type).hs,
                                subtitle = subtitle(type).hs
                            )
                        },
                        right = {
                            CellRightNavigation()
                        },
                        onClick = {
                            mainViewModel.setAccountType(type)
                            openSelectCoinsScreen()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun title(type: AccountType) = when (type) {
    is AccountType.EvmPrivateKey -> stringResource(R.string.Restore_SelectNetwork_EvmPrivateKey_Title)
    is AccountType.TronPrivateKey -> stringResource(R.string.Restore_SelectNetwork_TronPrivateKey_Title)
    else -> throw IllegalArgumentException()
}

@Composable
private fun subtitle(type: AccountType) = when (type) {
    is AccountType.EvmPrivateKey -> stringResource(R.string.Restore_SelectNetwork_EvmPrivateKey_Subtitle)
    is AccountType.TronPrivateKey -> stringResource(R.string.Restore_SelectNetwork_TronPrivateKey_Subtitle)
    else -> throw IllegalArgumentException()
}