package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import io.horizontalsystems.views.databinding.ViewBackupWordTextBinding

class BackupWordView : FrameLayout {

    private val binding = ViewBackupWordTextBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun bind(number: String, word: String) {
        binding.numberText.text = number
        binding.wordText.text = word
    }

}
