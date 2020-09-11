package io.horizontalsystems.views

import android.content.Context
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import kotlinx.android.synthetic.main.view_coin_search.view.*

class CoinSearchView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_coin_search, this)

        closeButton.setOnClickListener {
            searchInput.setText("")
        }

        searchInput.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                Handler().postDelayed({
                    performClick()
                }, 100)
            }
            false
        }

        bindTextChangeListener { closeButton.isInvisible = it.isBlank() }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    fun bind(hint: String, onTextChanged: ((String) -> Unit)) {
        searchInput.hint = hint
        bindTextChangeListener(onTextChanged)
    }

    private fun bindTextChangeListener(onTextChanged: ((String) -> Unit)) {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val trimmedText = s.toString().trim()
                onTextChanged.invoke(trimmedText)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

}
