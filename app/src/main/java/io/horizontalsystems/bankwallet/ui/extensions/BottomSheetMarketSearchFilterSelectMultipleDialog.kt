package io.horizontalsystems.bankwallet.ui.extensions

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrence
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper

class BottomSheetMarketSearchFilterSelectMultipleDialog<ItemClass>(
    val titleText: String,
    val subtitleText: String = "",
    val headerIcon: Int,
    val items: List<ViewItemWrapper<ItemClass>>,
    val selectedIndexes: List<Int> = listOf(),
    val onCloseListener: ((List<Int>) -> Unit)
) : BaseComposableBottomSheetFragment() {

    val selected = mutableStateListOf<Int>().apply {
        addAll(selectedIndexes)
    }

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
            iconPainter = painterResource(headerIcon),
            title = titleText,
            subtitle = subtitleText,
            onCloseClick = {
                close()
            },
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob)
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            items.forEachIndexed { index, item ->
                CellMultilineLawrence(
                    borderBottom = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (selected.contains(index)) {
                                    selected.remove(index)
                                } else {
                                    selected.add(index)
                                }
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = ComposeAppTheme.typography.body,
                            color = ComposeAppTheme.colors.leah
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (selected.contains(index)) {
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

    override fun onCancel(dialog: DialogInterface) {
        onCloseListener.invoke(selected)
    }

    override fun close() {
        onCloseListener.invoke(selected)
        super.close()
    }
}
