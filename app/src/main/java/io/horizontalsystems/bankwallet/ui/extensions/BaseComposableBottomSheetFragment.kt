package io.horizontalsystems.bankwallet.ui.extensions

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.views.helpers.LayoutHelper

open class BaseComposableBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout
            bottomSheet.setPadding(0, 0, 0, LayoutHelper.dp(8f, context))
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheetBehavior.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                isHideable = true
                skipCollapsed = true
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.BottomDialog
    }

    open fun close() {
        dismiss()
    }

}

@Composable
fun BottomSheetHeader(
    iconPainter: Painter,
    title: String,
    subtitle: String? = null,
    onCloseClick: () -> Unit,
    iconTint: ColorFilter? = null,
    content: @Composable() (ColumnScope.() -> Unit),
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .verticalScroll(rememberScrollState())
            .background(color = ComposeAppTheme.colors.lawrence)
    ) {
        Row(Modifier.height(64.dp)) {
            Image(
                modifier = Modifier.padding(top = 12.dp, start = 12.dp).size(24.dp),
                painter = iconPainter,
                colorFilter = iconTint,
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    color = ComposeAppTheme.colors.leah,
                    style = ComposeAppTheme.typography.headline2,
                    maxLines = 1,
                )
                subtitle?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HsIconButton(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp),
                onClick = onCloseClick
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )
            }
        }
        Column(
            content = content
        )
    }
}
