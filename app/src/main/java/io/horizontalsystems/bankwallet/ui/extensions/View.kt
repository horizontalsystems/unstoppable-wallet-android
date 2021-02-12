package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R

fun createImageButton(context: Context, @StyleRes resId: Int, @DrawableRes drawable: Int, params: LayoutParams, onClick: View.OnClickListener? = null) =
        ImageButton(ContextThemeWrapper(context, resId), null, resId).apply {
            layoutParams = params
            setImageDrawable(ContextCompat.getDrawable(context, drawable))
            setOnClickListener(onClick)
        }

fun createButton(context: Context, @StyleRes resId: Int, textResId: Int, params: LayoutParams, onClick: View.OnClickListener? = null) =
        Button(ContextThemeWrapper(context, resId), null, resId).apply {
            layoutParams = params
            setText(textResId)
            setOnClickListener(onClick)
        }

fun createProgressBar(context: Context) =
        ProgressBar(context).apply {
            indeterminateTintList = ColorStateList.valueOf(context.getColor(R.color.grey))
        }

fun createTextView(context: Context, @StyleRes resId: Int) =
        TextView(ContextThemeWrapper(context, resId))