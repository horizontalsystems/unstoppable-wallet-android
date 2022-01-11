package io.horizontalsystems.bankwallet.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

open class BaseComposableBottomSheetFragment : BottomSheetDialogFragment() {

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private val disableCloseOnSwipeBehavior = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    var shouldCloseOnSwipe: Boolean = true
        set(shouldClose) {
            field = shouldClose
            setUpSwipeBehavior()
        }

    private fun setUpSwipeBehavior() {
        if (shouldCloseOnSwipe)
            bottomSheetBehavior?.removeBottomSheetCallback(disableCloseOnSwipeBehavior)
        else
            bottomSheetBehavior?.addBottomSheetCallback(disableCloseOnSwipeBehavior)
    }

    override fun getTheme(): Int {
        return R.style.BottomDialog
    }

    @Composable
    open fun BottomContent() {

    }

    open fun close() {
        dismiss()
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
                    BottomContent()
                }
            }
        }
    }

    @Composable
    protected fun BottomSheetHeader(
        icon: Int,
        title: String,
        subtitle: String? = null,
        content: @Composable() (ColumnScope.() -> Unit),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(color = ComposeAppTheme.colors.lawrence)
        ) {
            Row(Modifier.height(64.dp)) {
                Image(
                    modifier = Modifier.padding(top = 12.dp, start = 12.dp).size(24.dp),
                    painter = painterResource(icon),
                    contentDescription = null
                )
                Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp).weight(1f)) {
                    Text(
                        text = title,
                        color = ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.headline2,
                        maxLines = 1,
                    )
                    Text(
                        text = subtitle ?: "",
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = { close() }) {
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

}
