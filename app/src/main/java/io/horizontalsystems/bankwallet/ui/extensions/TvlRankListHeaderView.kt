package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewTvlrankListHeaderBinding
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent

class TvlRankListHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewTvlrankListHeaderBinding.inflate(LayoutInflater.from(context), this)

    interface Listener {
        fun onFilterClick()
        fun onChangeSortingClick()
    }

    var listener: Listener? = null
    private var leftMenuTitle = ""
    private var sortDesc = false

    init {
        updateMenu()
    }

    fun setLeftField(fieldName: Int) {
        leftMenuTitle = context.getString(fieldName)
        updateMenu()
    }

    fun setSortDescending(sortDesc: Boolean) {
        this.sortDesc = sortDesc
        updateMenu()
    }

    private fun updateMenu() {
        binding.composeView.setContent {
            ComposeAppTheme {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 8.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ButtonSecondaryTransparent(
                            title = leftMenuTitle,
                            iconRight = R.drawable.ic_down_arrow_20,
                            onClick = {
                                listener?.onFilterClick()
                            }
                        )
                        ButtonSecondaryCircle(
                            icon = if (sortDesc) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
                            onClick = {
                                listener?.onChangeSortingClick()
                            }
                        )
                    }
                }
            }
        }
    }

}
