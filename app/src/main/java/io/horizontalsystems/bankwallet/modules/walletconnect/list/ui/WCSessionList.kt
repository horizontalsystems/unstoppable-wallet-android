package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListUiState
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCRequestViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun WCSessionList(
    viewModel: WalletConnectListViewModel,
    onSessionDeleteClick: (WalletConnectListModule.SessionViewItem) -> Unit,
    onRequestClick:(WCRequestViewItem) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState(initial = WalletConnectListUiState())
    var revealedCardId by remember { mutableStateOf<String?>(null) }

    uiState.showError?.let { message ->
        val view = LocalView.current
        HudHelper.showErrorMessage(view, text = message)
        viewModel.errorShown()
    }

    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)) {
        WCSection(
            uiState.sessionViewItems,
            onRequestClick = onRequestClick,
            onDelete = onSessionDeleteClick
        )
        item {
            if (uiState.sessionViewItems.isNotEmpty()) {
                VSpacer(32.dp)
            }
        }

        //todo remove it from viewmodel too
//        val pairingsNumber = uiState.pairingsNumber
//        if (pairingsNumber > 0) {
//            item {
//                CellSingleLineLawrenceSection {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .clickable {
//                                navController.slideFromRight(R.id.wcPairingsFragment)
//                            }
//                            .padding(horizontal = 16.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        body_leah(text = stringResource(R.string.WalletConnect_Pairings))
//                        Spacer(modifier = Modifier.weight(1f))
//                        subhead1_grey(text = pairingsNumber.toString())
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Image(
//                            painter = painterResource(id = R.drawable.ic_arrow_right),
//                            contentDescription = null
//                        )
//                    }
//                }
//                Spacer(modifier = Modifier.height(32.dp))
//            }
//        }
    }
}

private fun LazyListScope.WCSection(
    viewItems: List<WalletConnectListModule.SessionViewItem>,
    onDelete: (WalletConnectListModule.SessionViewItem) -> Unit,
    onRequestClick: (WCRequestViewItem) -> Unit
) {
    itemsIndexed(viewItems, key = { _, item -> item.sessionTopic }) { index, item ->
        val showDivider = showDivider(viewItems.size, index)
        val shape = getShape(viewItems.size, index)
        DappCell(
            session = item,
            onDeleteClick = onDelete,
            onRequestsClick = onRequestClick
        )
//        Box(
//            modifier = Modifier.fillMaxWidth(),
//            contentAlignment = Alignment.Center
//        ) {
//            ActionsRow(
//                content = {
//                    HsIconButton(
//                        modifier = Modifier
//                            .fillMaxHeight()
//                            .width(88.dp),
//                        onClick = { onDelete(item.sessionTopic) },
//                        content = {
//                            Icon(
//                                painter = painterResource(id = R.drawable.ic_circle_minus_24),
//                                tint = ComposeAppTheme.colors.grey,
//                                contentDescription = "delete",
//                            )
//                        }
//                    )
//                },
//            )
//            DraggableCardSimple(
//                key = item.sessionTopic,
//                isRevealed = revealedCardId == item.sessionTopic,
//                cardOffset = 72f,
//                onReveal = { onReveal(item.sessionTopic) },
//                onConceal = onConceal,
//                content = {
//                    WCSessionCell(
//                        shape = shape,
//                        showDivider = showDivider,
//                        session = item,
//                        navController = navController
//                    )
//                }
//            )
//        }
    }
}

@Composable
fun DappCell(
    session: WalletConnectListModule.SessionViewItem,
    onDeleteClick: (WalletConnectListModule.SessionViewItem) -> Unit,
    onRequestsClick: (WCRequestViewItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        CellPrimary(
            left = {
                Image(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    painter = rememberAsyncImagePainter(
                        model = session.imageUrl,
                        error = painterResource(R.drawable.ic_platform_placeholder_24)
                    ),
                    contentDescription = null,
                )
            },
            middle = {
                CellMiddleInfo(
                    title = if (session.title.isNotBlank()) session.title.hs else stringResource(R.string.WalletConnect_Unnamed).hs,
                    subtitle = session.subtitle.hs
                )
            },
            right = {
                HSIconButton(
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Small,
                    icon = painterResource(R.drawable.trash_24)
                ) {
                    onDeleteClick(session)
                }
            }
        )
        session.requests.forEach { item ->
            HsDivider()
            CellPrimary(
                middle = {
                    CellMiddleInfo(
                        subtitle = stringResource(R.string.DAppConnection_Requests).hs,
                    )
                },
                right = {
                    CellRightNavigation(
                        subtitle = item.title.hs
                    )
                },
                onClick = {
                    onRequestsClick.invoke(item)
                }
            )
        }
    }
}

fun getShape(itemsCount: Int, index: Int): Shape = when {
    itemsCount == 1 -> RoundedCornerShape(16.dp)
    itemsCount - 1 == index -> RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
    0 == index -> RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)
    else -> RoundedCornerShape(0.dp)
}

fun showDivider(itemsCount: Int, index: Int): Boolean = when {
    itemsCount == 1 || index == 0 -> false
    else -> true
}
