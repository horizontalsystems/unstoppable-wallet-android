package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_backup_word_text.view.*

class BackupWordView : FrameLayout {

    init {
        inflate(context, R.layout.view_backup_word_text, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    fun bind(number: String, word: String) {
        numberText.text = number
        wordText.text = word
    }

}
