package io.horizontalsystems.bankwallet.ui.compose

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView

enum class Keyboard {
    Opened, Closed
}

@Composable
fun observeKeyboardState(): State<Keyboard> {
    val keyboardState = remember { mutableStateOf(Keyboard.Closed) }
    val view = LocalView.current
    DisposableEffect(view) {
        val onGlobalListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            var currentView: View? = null

            override fun onGlobalLayout() {
                val viewTmp = currentView?: return

                val rect = Rect()
                viewTmp.getWindowVisibleDisplayFrame(rect)
                val screenHeight = viewTmp.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                keyboardState.value = if (keypadHeight > screenHeight * 0.15) {
                    Keyboard.Opened
                } else {
                    Keyboard.Closed
                }
            }
        }
        onGlobalListener.currentView = view
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            onGlobalListener.currentView = null
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return keyboardState
}
