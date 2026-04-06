package cash.p.terminal.modules.tonconnect

import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.walletconnect.list.ui.ActionsRow
import com.tonapps.wallet.data.tonconnect.entities.DAppManifestEntity
import cash.p.terminal.ui_compose.components.DraggableCardSimple
import cash.p.terminal.modules.walletconnect.list.ui.getShape
import cash.p.terminal.modules.walletconnect.list.ui.showDivider
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.HsDivider
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.headline2_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import com.tonapps.wallet.data.tonconnect.entities.DAppEntity

@Composable
fun TonConnectSessionList(
    dapps: Map<String, List<DAppEntity>>,
    onDelete: (DAppEntity) -> Unit
) {
    var revealedCardId by remember(dapps) { mutableStateOf<String?>(null) }

    LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)) {
        dapps.forEach { (groupTitle, list) ->
            item {
                HeaderText(text = groupTitle.uppercase())
            }

            TCSection(
                list,
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
                                tint = ComposeAppTheme.colors.grey,
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
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
                painter = rememberDAppIconPainter(dapp.manifest),
                contentDescription = null,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val title = when {
                    dapp.manifest.name.isNotBlank() -> dapp.manifest.name
                    else -> stringResource(id = R.string.TonConnect_Unnamed)
                }

                headline2_leah(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subhead2_grey(text = dapp.manifest.host)
            }
        }
    }
}

private const val ICON_TIMEOUT_MS = 3000L

@Composable
fun rememberDAppIconPainter(manifest: DAppManifestEntity): Painter {
    val fallbackUrl = remember(manifest.host) {
        "https://www.google.com/s2/favicons?sz=256&domain=${manifest.host}"
    }
    var useFallback by remember(manifest.iconUrl, fallbackUrl) {
        mutableStateOf(false)
    }

    val painter = rememberAsyncImagePainter(
        model = if (useFallback) fallbackUrl else manifest.iconUrl,
        error = painterResource(R.drawable.ic_platform_placeholder_24),
        onError = {
            if (!useFallback) {
                useFallback = true
            }
        }
    )
    val state by painter.state.collectAsState()

    if (!useFallback &&
        state !is AsyncImagePainter.State.Success &&
        state !is AsyncImagePainter.State.Error
    ) {
        LaunchedEffect(manifest.iconUrl, state) {
            delay(ICON_TIMEOUT_MS)
            if (painter.state.value !is AsyncImagePainter.State.Success) {
                useFallback = true
            }
        }
    }

    return painter
}
