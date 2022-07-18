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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah

open class BaseComposableBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout
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
    onCloseClick: () -> Unit,
    iconTint: ColorFilter? = null,
    content: @Composable() (ColumnScope.() -> Unit),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp))
            .verticalScroll(rememberScrollState())
            .background(color = ComposeAppTheme.colors.lawrence)
    ) {
        Box(Modifier.height(60.dp)){
            Row(
                modifier = Modifier
                    .padding(start = 32.dp, top = 24.dp, end = 32.dp)
                    .height(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = iconPainter,
                    colorFilter = iconTint,
                    contentDescription = null
                )
                headline2_leah(
                    text = title,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                )
                HsIconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = onCloseClick
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                }
            }
        }
        Column(
            content = content
        )
    }
}

@Preview
@Composable
private fun BottomSheetHeader_Preview() {
    val iconPainter = painterResource(R.drawable.icon_24_lock)
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = iconPainter,
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = stringResource(R.string.ManageAccount_SwitchWallet_Title),
            onCloseClick = {  },
        ){
            body_grey(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "Bottom sheet content",
                maxLines = 1,
            )
        }
    }
}
