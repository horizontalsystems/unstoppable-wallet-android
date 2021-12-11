package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import kotlinx.android.synthetic.main.view_market_list_header.view.*

class MarketListHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onSortingClick()
        fun onToggleButtonClick()
    }

    var listener: Listener? = null

    init {
        inflate(context, R.layout.view_market_list_header, this)
    }

    fun setMenu(sortMenu: SortMenu, toggleButton: ToggleButton) {
        composeView.setContent {
            ComposeAppTheme {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            sortMenu(sortMenu)
                        }
                        Box(modifier = Modifier.padding(start = 16.dp)) {
                            toggleMenu(toggleButton)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun toggleMenu(toggleButton: ToggleButton) {
        ButtonSecondaryToggle(
            toggleIndicators = toggleButton.indicators,
            title = toggleButton.title,
            onClick = { listener?.onToggleButtonClick() }
        )
    }

    @Composable
    private fun sortMenu(sortMenu: SortMenu) {
        when (sortMenu) {
            is SortMenu.DuoOption -> {
                ButtonSecondaryCircle(
                    modifier = Modifier.padding(start = 16.dp),
                    icon = if (sortMenu.direction == Direction.Down) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
                    onClick = {
                        listener?.onSortingClick()
                    }
                )
            }
            is SortMenu.MultiOption -> {
                ButtonSecondaryTransparent(
                    title = sortMenu.title,
                    iconRight = R.drawable.ic_down_arrow_20,
                    onClick = {
                        listener?.onSortingClick()
                    }
                )
            }
        }
    }

    data class ToggleButton(val title: String, val indicators: List<ToggleIndicator>)

    sealed class SortMenu {
        class DuoOption(val direction: Direction) : SortMenu()
        class MultiOption(val title: String) : SortMenu()
    }

    enum class Direction {
        Up, Down
    }
}
