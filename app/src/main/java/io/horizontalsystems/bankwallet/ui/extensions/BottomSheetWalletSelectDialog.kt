package io.horizontalsystems.bankwallet.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

class BottomSheetWalletSelectDialog : BaseComposableBottomSheetFragment() {

    var wallets: List<Account> = emptyList()
    var watchingAddresses: List<Account> = emptyList()
    var selectedItem: Account? = null
    var onSelectListener: ((Account) -> Unit)? = null

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
                    BottomSheetScreen()
                }
            }
        }
    }

    @Composable
    private fun BottomSheetScreen() {
        val comparator = compareBy<Account> { it.name.lowercase() }

        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.icon_24_lock),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = stringResource(R.string.ManageAccount_SwitchWallet_Title),
            onCloseClick = { close() },
        ) {

            Spacer(Modifier.height(12.dp))

            if (wallets.isNotEmpty()) {
                HeaderText(
                    text = stringResource(R.string.ManageAccount_Wallets)
                )
                Section(wallets.sortedWith(comparator))
            }

            if (watchingAddresses.isNotEmpty()) {
                if (wallets.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                }
                HeaderText(
                    text = stringResource(R.string.ManageAccount_WatchAddresses)
                )
                Section(watchingAddresses.sortedWith(comparator))
            }

            Spacer(Modifier.height(44.dp))
        }
    }

    @Composable
    private fun Section(items: List<Account>) {
        CellUniversalLawrenceSection(items, showFrame = true) { item ->
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    onSelectListener?.invoke(item)
                    dismiss()
                },
            ) {
                HsRadioButton(
                    selected = item == selectedItem,
                    onClick = {
                        onSelectListener?.invoke(item)
                        dismiss()
                    }
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    body_leah(text = item.name)
                    subhead2_grey(text = item.type.detailedDescription)
                }
                if (item.isWatchAccount) {
                    Icon(
                        modifier = Modifier.padding(start = 16.dp),
                        painter = painterResource(id = R.drawable.ic_eye_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            }
        }
    }
}
