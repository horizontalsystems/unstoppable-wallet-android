package com.quantum.wallet.bankwallet.modules.contacts

import android.os.Parcelable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import com.quantum.wallet.bankwallet.core.shorten
import com.quantum.wallet.bankwallet.ui.compose.ColoredTextStyle
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.AppBar
import com.quantum.wallet.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import com.quantum.wallet.bankwallet.ui.compose.components.HsBackButton
import com.quantum.wallet.bankwallet.ui.compose.components.ListEmptyView
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.body_grey50
import com.quantum.wallet.bankwallet.ui.compose.components.headline1_leah
import com.quantum.wallet.bankwallet.ui.compose.components.headline2_leah
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class ChooseContactFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<BlockchainType>(navController) { blockchainType ->
            ChooseContactScreen(blockchainType, navController)
        }
    }

    @Parcelize
    data class Result(val address: String) : Parcelable
}

@Composable
fun ChooseContactScreen(
    blockchainType: BlockchainType,
    navController: NavController
) {
    val viewModel = viewModel<ChooseContactViewModel>(factory = ChooseContactViewModel.Factory(blockchainType))

    val items = viewModel.items

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
                            cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
                        )
                        SideEffect {
                            focusRequester.requestFocus()
                        }
                    } else {
                        headline1_leah(text = stringResource(id = R.string.Contacts))
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
            Crossfade(items.isEmpty(), label = "") { empty ->
                if (empty) {
                    ListEmptyView(
                        text = stringResource(R.string.EmptyResults),
                        icon = R.drawable.ic_not_found
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        VSpacer(height = 12.dp)
                        CellUniversalLawrenceSection(items, showFrame = true) { contact ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.setNavigationResultX(ChooseContactFragment.Result(contact.address))
                                        navController.popBackStack()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                headline2_leah(text = contact.name)
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
