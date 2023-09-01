package cash.p.terminal.modules.receivemain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.bitcoinCashCoinType
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.modules.receive.address.ReceiveAddressFragment
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.SectionUniversalItem
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.findNavController

private val testItems = listOf(
    BipViewItem(
        title = "Native SegWit (recommended)",
        subtitle = "BIP 84",
    ),
    BipViewItem(
        title = "Newest",
        subtitle = "BIP 86",
    ),
)

class AddressFormatFragment : BaseFragment() {

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
                AddressFormatScreen(findNavController())
            }
        }
    }
}

@Composable
fun AddressFormatScreen(
    navController: NavController,
) {
    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Balance_Receive_AddressFormat),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                navController.popBackStack(R.id.receiveTokenSelectFragment, true)
                            }
                        )
                    )
                )
            }
        ) {
            Column(Modifier.padding(it)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    InfoText(
                        text = stringResource(R.string.Balance_Receive_AddressFormatDescription)
                    )
                    VSpacer(20.dp)
                    CellUniversalLawrenceSection(testItems) { item ->
                        SectionUniversalItem {
                            AddressFormatCell(
                                title = item.title,
                                subtitle = item.subtitle,
                                onClick = {
                                    navController.slideFromBottom(
                                        R.id.receiveFragment,
                                        bundleOf(ReceiveAddressFragment.WALLET_KEY to wallet)
                                    )
                                }
                            )
                        }
                    }
                    VSpacer(32.dp)
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.Balance_Receive_AddressFormat_WarningText)
                    )
                }
            }
        }
    }
}

@Composable
fun AddressFormatCell(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    RowUniversal(
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            body_leah(text = title)
            subhead2_grey(text = subtitle)
        }
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}

data class BipViewItem(
    val title: String,
    val subtitle: String,
)

@Preview
@Composable
fun Preview_BipSelectScreen() {
    val navController = rememberNavController()
    ComposeAppTheme {
        AddressFormatScreen(navController)
    }
}