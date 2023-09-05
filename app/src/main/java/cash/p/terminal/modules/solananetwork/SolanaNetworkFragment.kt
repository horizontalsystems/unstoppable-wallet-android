package cash.p.terminal.modules.solananetwork

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HeaderText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController

class SolanaNetworkFragment : BaseComposeFragment() {

    private val viewModel by viewModels<SolanaNetworkViewModel> {
        SolanaNetworkModule.Factory()
    }

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            SolanaNetworkScreen(
                viewModel,
                findNavController()
            )
        }
    }

}

@Composable
private fun SolanaNetworkScreen(
    viewModel: SolanaNetworkViewModel,
    navController: NavController
) {

    if (viewModel.closeScreen) {
        navController.popBackStack()
    }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.PlainString(viewModel.title),
                navigationIcon = {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = viewModel.blockchainType.imageUrl,
                            error = painterResource(R.drawable.ic_platform_placeholder_32)
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .size(24.dp)
                    )
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {

                item {
                    HeaderText(stringResource(R.string.AddEvmSyncSource_RpcSource)) {
                        navController.slideFromBottom(R.id.evmBlockchainSyncModeInfoFragment)
                    }
                }

                item {
                    CellUniversalLawrenceSection(viewModel.viewItems) { item ->
                        NetworkSettingCell(item.name, item.url, item.selected) {
                            viewModel.onSelectViewItem(item)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }

            }
        }

    }
}

@Composable
private fun NetworkSettingCell(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    RowUniversal(
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            body_leah(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            subhead2_grey(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }
    }
}
