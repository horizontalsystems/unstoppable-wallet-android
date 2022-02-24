package io.horizontalsystems.bankwallet.modules.walletconnect.requestlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.HsRadioButton
import io.horizontalsystems.core.findNavController

class WC2RequestListFragment : BaseFragment() {

    private val viewModel by viewModels<WC2RequestListViewModel> {
        WC2RequestListModule.Factory()
    }

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
                RequestListPage(
                    findNavController(),
                    viewModel,
                )
            }
        }
    }

}

@Composable
private fun RequestListPage(
    navController: NavController,
    viewModel: WC2RequestListViewModel,
) {
    val sections by viewModel.sectionItems.observeAsState()

    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                TranslatableString.ResString(R.string.WalletConnect_PendingRequests),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            sections?.let {
                WCRequestList(it) { accountId -> viewModel.onWalletSwitch(accountId) }
            }
        }
    }
}

@Composable
private fun WCRequestList(
    sectionItems: List<WC2RequestListModule.SectionViewItem>,
    onWalletSwitch: (String) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(12.dp))
        sectionItems.forEach { section ->
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                RequestsSectionHeaderCell(
                    accountId = section.accountId,
                    selected = section.active,
                    walletName = section.walletName,
                    onWalletSwitch = onWalletSwitch
                )
                section.requests.forEach { request ->
                    RequestCell(request.title, request.subtitle, section.active)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RequestsSectionHeaderCell(
    accountId: String,
    selected: Boolean,
    walletName: String,
    onWalletSwitch: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(ComposeAppTheme.colors.lawrence)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HsRadioButton(
            selected = selected,
            onClick = { onWalletSwitch.invoke(accountId) }
        )
        Spacer(Modifier.width(16.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = walletName,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah
        )
        if (!selected) {
            ButtonSecondaryDefault(
                title = stringResource(R.string.Button_Switch),
                onClick = { onWalletSwitch.invoke(accountId) },
            )
        }
    }
}

@Composable
private fun RequestCell(
    title: String,
    subtitle: String,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.background(ComposeAppTheme.colors.lawrence.copy(alpha = if (enabled) 1f else 0.5f))
    ) {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable(
                    onClick = { },
                    enabled = enabled
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ComposeAppTheme.typography.body,
                    color = if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey50,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = ComposeAppTheme.typography.subhead2,
                    color = if (enabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.grey50,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Image(
                modifier = Modifier.padding(start = 5.dp),
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (enabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.grey50)
            )
        }
    }
}

@Preview
@Composable
fun PreviewRequestList() {
    val items1 = listOf(
        WC2RequestListModule.RequestViewItem(2L, "Title 2", "Subtitle"),
        WC2RequestListModule.RequestViewItem(3L, "Title 3", "Subtitle"),
    )
    val sections = listOf(
        WC2RequestListModule.SectionViewItem("1", "Wallet 1", true, items1),
        WC2RequestListModule.SectionViewItem("2", "Wallet 1", false, items1),
    )

    ComposeAppTheme {
        WCRequestList(sections) { }
    }
}
