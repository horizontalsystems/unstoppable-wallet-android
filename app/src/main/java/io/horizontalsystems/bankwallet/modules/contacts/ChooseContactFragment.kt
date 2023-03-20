package io.horizontalsystems.bankwallet.modules.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
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
                var searchMode by remember { mutableStateOf(false) }

                AppBar(
                    title = {
                        if (searchMode) {
                            var searchText by rememberSaveable { mutableStateOf("") }
                            val focusRequester = remember { FocusRequester() }

                            BasicTextField(
                                modifier = Modifier
                                    .focusRequester(focusRequester),
                                value = searchText,
                                onValueChange = { value ->
                                    searchText = value
                                    viewModel.onEnterQuery(value)
                                },
                                singleLine = true,
                                textStyle = ColoredTextStyle(
                                    color = ComposeAppTheme.colors.leah,
                                    textStyle = ComposeAppTheme.typography.body
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchText.isEmpty()) {
                                        body_grey50(stringResource(R.string.Market_Search_Hint))
                                    }
                                    innerTextField()
                                },
                                cursorBrush = SolidColor(ComposeAppTheme.colors.jacob),
                            )
                            SideEffect {
                                focusRequester.requestFocus()
                            }
                        } else {
                            title3_leah(text = stringResource(id = R.string.Contacts))
                        }
                    },
                    navigationIcon = {
                        HsBackButton(onClick = {
                            if (searchMode) {
                                viewModel.onEnterQuery(null)
                                searchMode = false
                            } else {
                                navController.popBackStack()
                            }
                        })
                    },
                    menuItems = if (searchMode) {
                        listOf()
                    } else {
                        listOf(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.Button_Search),
                                icon = R.drawable.icon_search,
                                onClick = {
                                    searchMode = true
                                }
                            )
                        )
                    }
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                Crossfade(items.isEmpty()) {empty ->
                    if (empty) {
                        ListEmptyView(
                            text = stringResource(R.string.EmptyResults),
                            icon = R.drawable.ic_not_found
                        )
                    } else {
                        VSpacer(height = 12.dp)
                        CellUniversalLawrenceSection(items, showFrame = true) { contact ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.setNavigationResult(
                                            ChooseContactFragment.resultKey,
                                            bundleOf("contact" to contact.address)
                                        )
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
    }
}
