package cash.p.terminal.navigation

import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions

fun NavController.slideFromRight(@IdRes resId: Int, input: Parcelable? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_right)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_right)
        .build()

    val args = input?.let {
        bundleOf("input" to it)
    }
    navigate(resId, args, navOptions)
}

fun NavController.slideFromRight(@IdRes resId: Int, vararg pairs: Pair<String, Any?>) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_right)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_right)
        .build()

    navigate(resId, bundleOf(*pairs), navOptions)
}