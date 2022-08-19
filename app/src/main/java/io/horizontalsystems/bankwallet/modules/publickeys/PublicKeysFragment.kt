package io.horizontalsystems.bankwallet.modules.publickeys

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.modules.recoveryphrase.RecoveryPhraseModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class PublicKeysFragment : BaseFragment() {

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
                PublicKeysScreen(
                    account = arguments?.getParcelable(RecoveryPhraseModule.ACCOUNT)!!,
                    onBackPress = { findNavController().popBackStack() },
                )
            }
        }
    }

}

@Composable
private fun PublicKeysScreen(
    account: Account,
    onBackPress: () -> Unit,
    viewModel: PublicKeysViewModel = viewModel(
        factory = PublicKeysModule.Factory(account)
    )
) {
    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.PublicKeys_Title),
                navigationIcon = {
                    HsIconButton(onClick = onBackPress) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
            )

            Column(Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(24.dp))
                PublicKeys(viewModel)
            }
        }
    }

}

@Composable
private fun PublicKeys(viewModel: PublicKeysViewModel) {
    val localView = LocalView.current
    HeaderText(text = "BITCOIN")
    CellSingleLineLawrenceSection(
        listOf(
            {
                KeyCell(stringResource(R.string.ShowKey_Bip44)) {
                    copy(viewModel.bitcoinPublicKeys(AccountType.Derivation.bip44), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.ShowKey_Bip49)) {
                    copy(viewModel.bitcoinPublicKeys(AccountType.Derivation.bip49), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.ShowKey_Bip84)) {
                    copy(viewModel.bitcoinPublicKeys(AccountType.Derivation.bip84), localView)
                }
            },
        )
    )
    Spacer(Modifier.height(24.dp))
    HeaderText(text = "BITCOIN CASH")
    CellSingleLineLawrenceSection(
        listOf(
            {
                KeyCell(stringResource(R.string.CoinSettings_BitcoinCashCoinType_Type0_Title)) {
                    copy(viewModel.bitcoinCashPublicKeys(BitcoinCashCoinType.type0), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.CoinSettings_BitcoinCashCoinType_Type145_Title)) {
                    copy(viewModel.bitcoinCashPublicKeys(BitcoinCashCoinType.type145), localView)
                }
            },
        )
    )
    Spacer(Modifier.height(24.dp))
    HeaderText(text = "LITECOIN")
    CellSingleLineLawrenceSection(
        listOf(
            {
                KeyCell(stringResource(R.string.ShowKey_Bip44)) {
                    copy(viewModel.litecoinPublicKeys(AccountType.Derivation.bip44), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.ShowKey_Bip49)) {
                    copy(viewModel.litecoinPublicKeys(AccountType.Derivation.bip49), localView)
                }
            },
            {
                KeyCell(stringResource(R.string.ShowKey_Bip84)) {
                    copy(viewModel.litecoinPublicKeys(AccountType.Derivation.bip84), localView)
                }
            },
        )
    )
    Spacer(Modifier.height(24.dp))
    HeaderText(text = "DASH")
    CellSingleLineLawrenceSection(
        listOf {
            KeyCell(stringResource(R.string.ShowKey_TabPublicKeys)) {
                copy(viewModel.dashKeys(), localView)
            }
        }
    )
    Spacer(Modifier.height(32.dp))
}

private fun copy(publicKeys: String?, localView: View) {
    if (publicKeys != null) {
        TextHelper.copyText(publicKeys)
        HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
    } else {
        HudHelper.showErrorMessage(localView, R.string.Error)
    }
}

@Composable
private fun KeyCell(title: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        B2(
            text = title,
            modifier = Modifier.padding(end = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        ButtonSecondaryCircle(
            icon = R.drawable.ic_copy_20,
            onClick = onCopy
        )
    }
}

@Preview
@Composable
private fun PublicKeys_Preview() {
    ComposeAppTheme {
        Column {
            HeaderText(text = "LITECOIN")
            CellSingleLineLawrenceSection(
                listOf(
                    { KeyCell(stringResource(R.string.ShowKey_Bip44)) {} },
                    { KeyCell(stringResource(R.string.ShowKey_Bip49)) {} },
                    { KeyCell(stringResource(R.string.ShowKey_Bip84)) {} },
                )
            )
        }
    }
}
