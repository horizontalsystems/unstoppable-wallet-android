package io.horizontalsystems.bankwallet.modules.addtoken

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.TitleValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Blockchain

class AddTokenFragment : BaseFragment() {

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
                AddTokenScreen(findNavController())
            }
        }
    }
}

@Composable
private fun AddTokenScreen(
    navController: NavController,
    viewModel: AddTokenViewModel = viewModel(factory = AddTokenModule.Factory())
) {
    val uiState = viewModel.uiState

    if (uiState.finished) {
        HudHelper.showSuccessMessage(
            LocalView.current, R.string.Hud_Text_Done, SnackbarDuration.LONG
        )
        navController.popBackStack()
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.AddToken_Title),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                BlockchainSelector(
                    viewModel.blockchains,
                    viewModel.selectedBlockchain
                ) { viewModel.onBlockchainSelect(it) }

                Spacer(modifier = Modifier.height(32.dp))

                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    enabled = false,
                    hint = stringResource(R.string.AddToken_AddressOrSymbol),
                    state = getState(uiState.caution, uiState.loading),
                    qrScannerEnabled = true,
                ) {
                    viewModel.onEnterText(it)
                }

                Spacer(modifier = Modifier.height(32.dp))

                uiState.tokenInfo?.let { tokenInfo ->
                    CellUniversalLawrenceSection(
                        listOf(
                            {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_CoinName),
                                    tokenInfo.token.coin.name
                                )
                            }, {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_CoinCode),
                                    tokenInfo.token.coin.code
                                )
                            }, {
                                TitleValueCell(
                                    stringResource(R.string.AddToken_Decimals),
                                    tokenInfo.token.decimals.toString()
                                )
                            }
                        )
                    )
                }
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(R.string.Button_Add),
                    onClick = { viewModel.onAddClick() },
                    enabled = uiState.addButtonEnabled
                )
            }
        }
    }
}

@Composable
private fun BlockchainSelector(
    blockchains: List<Blockchain>,
    selectedBlockchain: Blockchain,
    onSelected: (Blockchain) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var rowSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart)
    ) {
        CellUniversalLawrenceSection(
            listOf {
                RowUniversal(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { layoutCoordinates ->
                            rowSize = layoutCoordinates.size.toSize()
                        }
                        .padding(horizontal = 16.dp),
                    onClick = { expanded = !expanded }
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_blocks_24),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    body_leah(
                        text = stringResource(R.string.AddToken_Blockchain),
                        modifier = Modifier.weight(1f)
                    )
                    subhead1_grey(
                        text = selectedBlockchain.name,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_down_arrow_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            }
        )
        MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(12.dp))) {
            Box {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(x = (16).dp, y = 0.dp),
                    modifier = Modifier
                        .width(with(LocalDensity.current) { rowSize.width.toDp() })
                        .background(ComposeAppTheme.colors.lawrence)
                ) {
                    blockchains.forEach { item ->
                        DropdownMenuItem(
                            onClick = {
                                onSelected.invoke(item)
                                expanded = false
                            },
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = item.type.imageUrl,
                                    error = painterResource(R.drawable.ic_platform_placeholder_32)
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            body_leah(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .weight(1f),
                                text = item.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item == selectedBlockchain) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_checkmark_20),
                                    tint = ComposeAppTheme.colors.jacob,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getState(caution: Caution?, loading: Boolean) = when (caution?.type) {
    Caution.Type.Error -> DataState.Error(Exception(caution.text))
    Caution.Type.Warning -> DataState.Error(FormsInputStateWarning(caution.text))
    null -> if (loading) DataState.Loading else null
}
