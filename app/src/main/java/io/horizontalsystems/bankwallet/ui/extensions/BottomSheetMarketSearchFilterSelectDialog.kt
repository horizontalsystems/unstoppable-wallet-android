package io.horizontalsystems.bankwallet.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.filters.FilterViewItemWrapper
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrence

class BottomSheetMarketSearchFilterSelectDialog<ItemClass> : BaseComposableBottomSheetFragment() {

    var items: List<FilterViewItemWrapper<ItemClass>>? = null
    var selectedItem: FilterViewItemWrapper<ItemClass>? = null
    var onSelectListener: ((FilterViewItemWrapper<ItemClass>) -> Unit)? = null

    var titleText: String = ""
    var subtitleText: String = ""

    @DrawableRes
    var headerIconResourceId: Int = 0

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
                ComposeAppTheme {
                    BottomSheetScreen()
                }
            }
        }
    }

    @Composable
    private fun BottomSheetScreen() {
        BottomSheetHeader(
            iconPainter = painterResource(headerIconResourceId),
            title = titleText,
            subtitle = subtitleText,
            onCloseClick = { close() },
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            items?.forEach { item ->
                CellMultilineLawrence(
                    borderBottom = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                onSelectListener?.invoke(item)
                                dismiss()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title ?: stringResource(R.string.Any),
                            style = ComposeAppTheme.typography.body,
                            color = ComposeAppTheme.colors.leah
                        )
                        Spacer(modifier = Modifier.weight(1f))
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
            Spacer(Modifier.height(16.dp))
        }
    }

}
