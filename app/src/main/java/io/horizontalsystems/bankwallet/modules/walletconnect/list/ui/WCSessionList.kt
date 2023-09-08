package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey

@Composable
fun WCSessionList(
    viewModel: WalletConnectListViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    var revealedCardId by remember { mutableStateOf<String?>(null) }

    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)) {
        uiState.v2SectionItem?.let { section ->
            WCSection(
                section,
                navController,
                revealedCardId,
                onReveal = { id ->
                    if (revealedCardId != id) {
                        revealedCardId = id
                    }
                },
                onConceal = {
                    revealedCardId = null
                },
                onDelete = { viewModel.onDeleteV2(it) }
            )
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        val pairingsNumber = uiState.pairingsNumber
        if (pairingsNumber > 0) {
            item {
                CellSingleLineLawrenceSection {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                navController.slideFromRight(R.id.wc2PairingsFragment)
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        body_leah(text = stringResource(R.string.WalletConnect_Pairings))
                        Spacer(modifier = Modifier.weight(1f))
                        subhead1_grey(text = pairingsNumber.toString())
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun LazyListScope.WCSection(
    section: WalletConnectListModule.Section,
    navController: NavController,
    revealedCardId: String?,
    onReveal: (String) -> Unit,
    onConceal: () -> Unit,
    onDelete: (String) -> Unit
) {
    item {
        subhead1_grey(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            text = stringResource(section.version.value),
        )
    }
    itemsIndexed(section.sessions, key = { _, item -> item.sessionId }) { index, item ->
        val showDivider = showDivider(section.sessions.size, index)
        val shape = getShape(section.sessions.size, index)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ActionsRow(
                content = {
                    HsIconButton(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp),
                        onClick = { onDelete(item.sessionId) },
                        content = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_circle_minus_24),
                                tint = Color.Gray,
                                contentDescription = "delete",
                            )
                        }
                    )
                },
            )
            DraggableCardSimple(
                key = item.sessionId,
                isRevealed = revealedCardId == item.sessionId,
                cardOffset = 72f,
                onReveal = { onReveal(item.sessionId) },
                onConceal = onConceal,
                content = {
                    WCSessionCell(
                        shape = shape,
                        showDivider = showDivider,
                        session = item,
                        version = section.version,
                        navController = navController
                    )
                }
            )
        }
    }
}

fun getShape(itemsCount: Int, index: Int): Shape = when {
    itemsCount == 1 -> RoundedCornerShape(12.dp)
    itemsCount - 1 == index -> RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp)
    0 == index -> RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp)
    else -> RoundedCornerShape(0.dp)
}

fun showDivider(itemsCount: Int, index: Int): Boolean = when {
    itemsCount == 1 || index == 0 -> false
    else -> true
}
