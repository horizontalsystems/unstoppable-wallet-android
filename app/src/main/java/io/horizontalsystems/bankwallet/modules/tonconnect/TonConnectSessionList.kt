package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.tonapps.wallet.data.tonconnect.entities.DAppEntity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.ActionsRow
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.DraggableCardSimple
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.getShape
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.showDivider
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun TonConnectSessionList(
    dapps: Map<String, List<DAppEntity>>,
    navController: NavController,
    onDelete: (DAppEntity) -> Unit
) {
    var revealedCardId by remember { mutableStateOf<String?>(null) }

//    uiState.error?.let { message ->
//        val view = LocalView.current
//        HudHelper.showErrorMessage(view, text = message)
//        viewModel.errorShown()
//    }

    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)) {
        dapps.forEach { (groupTitle, list) ->
            item {
                HeaderText(text = groupTitle.uppercase())
            }

            TCSection(
                list,
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
                onDelete = onDelete
            )

            item {
                VSpacer(24.dp)
            }
        }

    }
}

private fun LazyListScope.TCSection(
    dapps: List<DAppEntity>,
    navController: NavController,
    revealedCardId: String?,
    onReveal: (String) -> Unit,
    onConceal: () -> Unit,
    onDelete: (DAppEntity) -> Unit
) {
    itemsIndexed(dapps, key = { _, item -> item.uniqueId }) { index, dapp ->
        val showDivider = showDivider(dapps.size, index)
        val shape = getShape(dapps.size, index)
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
                        onClick = { onDelete(dapp) },
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
                key = dapp.manifest.name,
                isRevealed = revealedCardId == dapp.uniqueId,
                cardOffset = 72f,
                onReveal = { onReveal(dapp.uniqueId) },
                onConceal = onConceal,
                content = {
                    TCSessionCell(
                        shape = shape,
                        showDivider = showDivider,
                        dapp = dapp,
                        navController = navController
                    )
                }
            )
        }
    }
}


@Composable
fun TCSessionCell(
    shape: Shape,
    showDivider: Boolean = false,
    dapp: DAppEntity,
    navController: NavController,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(ComposeAppTheme.colors.lawrence),
        contentAlignment = Alignment.Center
    ) {
        if (showDivider) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
                painter = rememberAsyncImagePainter(
                    model = dapp.manifest.iconUrl,
                    error = painterResource(R.drawable.ic_platform_placeholder_24)
                ),
                contentDescription = null,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val title = when {
                    dapp.manifest.name.isNotBlank() -> dapp.manifest.name
                    else -> stringResource(id = R.string.TonConnect_Unnamed)
                }

                body_leah(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subhead2_grey(text = dapp.manifest.host)
            }
        }
    }
}
