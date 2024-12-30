package io.horizontalsystems.bankwallet.modules.market.filters

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.body_remus
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

@Composable
fun <ItemClass> SingleSelectBottomSheetContent(
    title: Int,
    headerIcon: Int,
    items: List<FilterViewItemWrapper<ItemClass>>,
    selectedItem: FilterViewItemWrapper<ItemClass>? = null,
    onSelect: (FilterViewItemWrapper<ItemClass>) -> Unit,
    onClose: (() -> Unit),
) {
    BottomSheetHeader(
        iconPainter = painterResource(headerIcon),
        title = stringResource(title),
        onCloseClick = onClose,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(
            items = items,
            showFrame = true
        ) { itemWrapper ->
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    onSelect(itemWrapper)
                    onClose()
                }
            ) {
                if (itemWrapper.title != null && itemWrapper.item is PriceChange) {
                    when (itemWrapper.item.color) {
                        TextColor.Lucian -> body_lucian(text = itemWrapper.title)
                        TextColor.Remus -> body_remus(text = itemWrapper.title)
                        TextColor.Grey -> body_grey(text = itemWrapper.title)
                        TextColor.Leah -> body_leah(text = itemWrapper.title)
                    }
                } else {
                    if (itemWrapper.title != null) {
                        body_leah(text = itemWrapper.title)
                    } else {
                        body_grey(text = stringResource(R.string.Any))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (itemWrapper == selectedItem) {
                    Image(
                        modifier = Modifier.padding(start = 5.dp),
                        painter = painterResource(id = R.drawable.ic_checkmark_20),
                        colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                        contentDescription = null
                    )
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

@Composable
fun PriceCloseToBottomSheetContent(
    items: List<PriceCloseTo>,
    selectedItem: PriceCloseTo? = null,
    onSelect: (PriceCloseTo?) -> Unit,
    onClose: (() -> Unit),
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_usd_24),
        title = stringResource(R.string.Market_Filter_PriceCloseTo),
        onCloseClick = onClose,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        VSpacer(12.dp)
        CellUniversalLawrenceSection(showFrame = true) {
            Column {
                RowUniversal(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = {
                        onSelect(null)
                        onClose()
                    }
                ) {
                    body_grey(
                        text = stringResource(R.string.Any),
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedItem == null) {
                        Image(
                            modifier = Modifier.padding(start = 5.dp),
                            painter = painterResource(id = R.drawable.ic_checkmark_20),
                            colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                            contentDescription = null
                        )
                    }
                }
                items.forEach { item ->
                    Divider(
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10,
                    )
                    RowUniversal(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            onSelect(item)
                            onClose()
                        }
                    ) {
                        body_leah(
                            text = stringResource(item.titleResId) + " " + stringResource(item.descriptionResId),
                            modifier = Modifier.weight(1f)
                        )
                        if (item == selectedItem) {
                            Image(
                                modifier = Modifier.padding(start = 5.dp),
                                painter = painterResource(id = R.drawable.ic_checkmark_20),
                                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }

        VSpacer(44.dp)
    }
}

@Composable
fun CoinSetBottomSheetContent(
    items: List<CoinList>,
    selectedItem: CoinList,
    onSelect: (CoinList) -> Unit,
    onClose: (() -> Unit),
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_circle_coin_24),
        title = stringResource(R.string.Market_Filter_ChooseSet),
        onCloseClick = onClose,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
    ) {
        VSpacer(12.dp)
        CellUniversalLawrenceSection(
            items = items,
            showFrame = true
        ) { item ->
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    onSelect(item)
                    onClose()
                }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    body_leah(text = stringResource(item.titleResId))
                    caption_grey(text = stringResource(item.descriptionResId))
                }

                if (item == selectedItem) {
                    Image(
                        modifier = Modifier.padding(start = 5.dp),
                        painter = painterResource(id = R.drawable.ic_checkmark_20),
                        colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                        contentDescription = null
                    )
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}