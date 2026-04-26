package cash.p.terminal.ui_compose

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Year

fun currentYear(): Int = Year.now().value

//  Fragment

fun Fragment.findNavController(): NavController {
    return NavHostFragment.findNavController(this)
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Parcelable> Bundle.getInputX(): T? {
    return parcelable("input")
}

inline fun <reified T : Parcelable> NavController.getInput(): T? {
    return currentBackStackEntry?.arguments?.getInputX()
}

inline fun <reified T : Parcelable> NavController.requireInput(): T {
    return requireNotNull(getInput()) { "Navigation input of type ${T::class.simpleName} is required but was null" }
}

internal inline fun <reified T> getKoinInstance(): T {
    return object : KoinComponent {
        val value: T by inject()
    }.value
}

@Composable
fun Modifier.blockClicksBehind() = this.clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() }
) { /* Do nothing */ }

@Composable
fun Modifier.oneLineHeight(textStyle: TextStyle) =
    this
        .height(with(LocalDensity.current) {
            textStyle.lineHeight.toDp()
        })
        .wrapContentHeight(Alignment.CenterVertically)

@Composable
fun <T> rememberDebouncedAction(
    timeoutMs: Long = 500,
    action: (T) -> Unit
): (T) -> Unit {
    val actionState by rememberUpdatedState(action)
    val lastClickTime = remember { mutableLongStateOf(0L) }

    return remember(timeoutMs) {
        { param: T ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastClickTime.longValue > timeoutMs) {
                lastClickTime.longValue = now
                actionState(param)
            }
        }
    }
}

fun TextFieldValue.withLeadingZeroIfDecimal(): TextFieldValue {
    val text = this.text
    return if (text.startsWith(",") || text.startsWith(".")) {
        copy(
            text = "0$text",
            selection = TextRange(selection.start + 1, selection.end + 1)
        )
    } else {
        this
    }
}