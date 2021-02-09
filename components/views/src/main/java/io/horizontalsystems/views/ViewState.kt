package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.view.View

class ViewState @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    private var state = listOf<Int>()

    var hasError: Boolean = false
        set(value) {
            field = value
            state = if (value) {
                listOf(R.attr.state_error)
            } else {
                listOf()
            }

            refreshDrawableState()
        }

    var hasWarning: Boolean = false
        set(value) {
            field = value
            state = if (value) {
                listOf(R.attr.state_warning)
            } else {
                listOf()
            }

            refreshDrawableState()
        }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        if (state.isNullOrEmpty()) {
            return super.onCreateDrawableState(extraSpace)
        }

        val state = super.onCreateDrawableState(extraSpace + state.size)
        mergeDrawableStates(state, this.state.toIntArray())

        return state
    }

    fun clearStates() {
        state = listOf()
        refreshDrawableState()
    }
}
