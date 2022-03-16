package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v1.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.list.v2.WC2ListViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun WCSessionList(
    viewModelWc2: WC2ListViewModel,
    viewModelWc1: WalletConnectListViewModel,
    navController: NavController
) {
    var revealedCardIds by remember { mutableStateOf(listOf<String>()) }

    LazyColumn {
        viewModelWc2.sectionItem?.let { section ->
            WCSection(
                section,
                navController,
                revealedCardIds,
                onExpand = { id ->
                    if (!revealedCardIds.contains(id)) {
                        revealedCardIds = listOf(id)
                    }
                },
                onCollapse = { id ->
                    revealedCardIds = revealedCardIds.toMutableList().also {
                        it.remove(id)
                    }
                },
                onDelete = { viewModelWc2.onDelete(it) }
            )
        }
        viewModelWc1.sectionItem?.let { section ->
            WCSection(
                section,
                navController,
                revealedCardIds,
                onExpand = { id ->
                    if (!revealedCardIds.contains(id)) {
                        revealedCardIds = listOf(id)
                    }
                },
                onCollapse = { id ->
                    revealedCardIds = revealedCardIds.toMutableList().also {
                        it.remove(id)
                    }
                },
                onDelete = { viewModelWc1.onDelete(it) }
            )
        }
    }
}

private fun LazyListScope.WCSection(
    section: WalletConnectListModule.Section,
    navController: NavController,
    revealedCardIds: List<String>,
    onExpand: (String) -> Unit,
    onCollapse: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    item {
        Text(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            text = stringResource(section.version.value),
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.grey
        )
    }
    section.pendingRequests?.let {
        item {
            PendingRequestsCell(it, navController)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
    itemsIndexed(section.sessions, key = { _, item -> item.sessionId }) { index, item ->
        val showDivider = showDivider(section.sessions.size, index)
        val shape = getShape(section.sessions.size, index)
        Box(Modifier.fillMaxWidth().height(60.dp)) {
            ActionsRow(
                content = {
                    IconButton(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 16.dp)
                            .width(84.dp),
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
                isRevealed = revealedCardIds.contains(item.sessionId),
                cardOffset = 84f,
                onExpand = { onExpand(item.sessionId) },
                onCollapse = { onCollapse(item.sessionId) },
                content = {
                    WCSessionCell(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(shape)
                            .background(ComposeAppTheme.colors.lawrence),
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

private fun getShape(itemsCount: Int, index: Int): Shape = when {
    itemsCount == 1 -> RoundedCornerShape(12.dp)
    itemsCount - 1 == index -> RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp)
    0 == index -> RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp)
    else -> RoundedCornerShape(0.dp)
}

private fun showDivider(itemsCount: Int, index: Int): Boolean = when {
    itemsCount == 1 || index == 0 -> false
    else -> true
}
