package io.horizontalsystems.bankwallet.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.HsRadioButton

class BottomSheetWalletSelectDialog : BaseComposableBottomSheetFragment() {

    var items: List<Account>? = null
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
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_wallet_24),
            title = stringResource(R.string.ManageAccount_SwitchWallet_Title),
            subtitle = stringResource(R.string.ManageAccount_SwitchWallet_Subtitle),
            onCloseClick = { close() },
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            val comparator = compareBy<Account> { it.isWatchAccount }.thenBy { it.name.lowercase() }
            items?.sortedWith(comparator)?.forEach { item ->
                CellMultilineLawrence(
                    borderBottom = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                onSelectListener?.invoke(item)
                                dismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                            Text(
                                text = item.name,
                                style = ComposeAppTheme.typography.body,
                                color = ComposeAppTheme.colors.leah
                            )
                            Text(
                                text = item.type.description,
                                style = ComposeAppTheme.typography.subhead2,
                                color = ComposeAppTheme.colors.grey
                            )
                        }
                        if (item.isWatchAccount) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(id = R.drawable.ic_eye_20),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.grey
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

}
