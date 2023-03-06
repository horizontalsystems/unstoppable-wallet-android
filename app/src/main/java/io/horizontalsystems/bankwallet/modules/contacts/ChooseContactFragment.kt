package cash.p.terminal.modules.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.shorten
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.marketkit.models.BlockchainType

class ChooseContactFragment : BaseFragment() {
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
                ChooseContactScreen(arguments?.getParcelable(blockchainTypeKey), findNavController())
            }
        }
    }

    companion object {
        const val resultKey = "chooseContactResult"

        private const val blockchainTypeKey = "blockchainTypeKey"
        fun prepareParams(blockchainType: BlockchainType): Bundle {
            return bundleOf(blockchainTypeKey to blockchainType)
        }
    }

}
@Composable
fun ChooseContactScreen(
    blockchainType: BlockchainType?,
    navController: NavController
) {
    val blockchainTypeNonNull = blockchainType ?: return
    val viewModel = viewModel<ChooseContactViewModel>(factory = ChooseContactViewModel.Factory(blockchainTypeNonNull))

    val items = viewModel.items

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Contacts),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Search),
                            icon = R.drawable.icon_search,
                            onClick = { navController.popBackStack() }
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                VSpacer(height = 12.dp)
                CellUniversalLawrenceSection(
                    items = items, showFrame = true
                ) { contact ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.setNavigationResult(ChooseContactFragment.resultKey, bundleOf("contact" to contact.address))
                                navController.popBackStack()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        body_leah(text = contact.name)
                        VSpacer(height = 1.dp)
                        subhead2_grey(text = contact.address.shorten())
                    }
                }
            }
        }
    }
}
