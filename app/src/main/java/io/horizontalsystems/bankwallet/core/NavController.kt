package io.horizontalsystems.bankwallet.core

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R

fun NavController.slideFromRight(@IdRes resId: Int, args: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_right)
        .setExitAnim(R.anim.slide_to_left)
        .setPopEnterAnim(R.anim.slide_from_left)
        .setPopExitAnim(R.anim.slide_to_right)
        .build()

    navigate(resId, args, navOptions)
}

fun NavController.slideFromBottom(@IdRes resId: Int, args: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_bottom)
        .setExitAnim(R.anim.slide_to_top)
        .setPopEnterAnim(R.anim.slide_from_top)
        .setPopExitAnim(R.anim.slide_to_bottom)
        .build()

    navigate(resId, args, navOptions)
}
