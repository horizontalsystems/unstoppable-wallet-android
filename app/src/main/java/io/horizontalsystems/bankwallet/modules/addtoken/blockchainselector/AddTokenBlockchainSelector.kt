package io.horizontalsystems.bankwallet.modules.addtoken.blockchainselector

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.marketkit.models.Blockchain

const val BlockchainSelectorResult = "blockchain_selector_result_key"

@Composable
fun AddTokenBlockchainSelectorScreen(
    blockchains: List<Blockchain>,
    selectedBlockchains: List<Blockchain>,
    navController: NavController,
) {
    val menuItems = emptyList<MenuItem>()
    val selectedItems =
        remember { mutableStateListOf<Blockchain>().apply { addAll(selectedBlockchains) } }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Market_Filter_Blockchains),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = menuItems
            )

            Column(
                Modifier.verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                HSSectionRounded {
                    blockchains.forEach { item ->
                        BlockchainCell(
                            item = item,
                            selected = selectedItems.contains(item),
                            onCheck = {
                                selectedItems.clear()
                                selectedItems.add(item)
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(BlockchainSelectorResult, selectedItems.toList())
                                navController.popBackStack()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BlockchainCell(
    item: Blockchain,
    selected: Boolean,
    onCheck: (Blockchain) -> Unit,
) {
    CellBorderedRowUniversal(
        borderTop = true,
        modifier = Modifier
            .clickable {
                onCheck(item)
            }
            .fillMaxWidth(),
        backgroundColor = ComposeAppTheme.colors.lawrence
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
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_checkmark_20),
                tint = ComposeAppTheme.colors.jacob,
                contentDescription = null,
            )
        }
    }
}
