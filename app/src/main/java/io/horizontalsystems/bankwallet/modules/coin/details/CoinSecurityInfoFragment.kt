package io.horizontalsystems.bankwallet.modules.coin.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule.SecurityInfoViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController

class CoinSecurityInfoFragment : BaseFragment() {

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
                    CoinSecurityInfo(
                        requireArguments().getInt(TITLE_KEY),
                        requireArguments().getParcelableArrayList(VIEW_ITEMS_KEY)!!,
                        onClickClose = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }


    companion object {
        private const val TITLE_KEY = "title_key"
        private const val VIEW_ITEMS_KEY = "view_items_key"

        fun prepareParams(@StringRes title: Int, viewItems: List<SecurityInfoViewItem>) =
            bundleOf(TITLE_KEY to title, VIEW_ITEMS_KEY to viewItems)
    }

}

@Composable
fun CoinSecurityInfo(
    @StringRes title: Int,
    viewItems: List<SecurityInfoViewItem>,
    onClickClose: () -> Unit
) {
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.CoinPage_SecurityParams_Info),
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = onClickClose
                )
            )
        )
        CellSingleLineClear(borderTop = true) {
            Text(
                text = stringResource(title),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah
            )
        }

        LazyColumn {
            items(viewItems) {
                CoinSecurityInfoItem(it)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

    }
}

@Composable
fun CoinSecurityInfoItem(infoViewItem: SecurityInfoViewItem) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 9.dp)) {
        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = stringResource(infoViewItem.title),
            style = ComposeAppTheme.typography.body,
            color = infoViewItem.grade.securityGradeColor()
        )
        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = stringResource(infoViewItem.description),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran
        )
    }
}
