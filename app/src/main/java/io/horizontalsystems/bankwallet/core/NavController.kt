package io.horizontalsystems.bankwallet.core

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R

fun NavController.slideFromRight(@IdRes resId: Int, args: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_right)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_right)
        .build()

    navigate(resId, args, navOptions)
}

fun NavController.slideFromBottom(@IdRes resId: Int, args: Bundle? = null) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_bottom)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(R.anim.slide_to_bottom)
        .build()

    navigate(resId, args, navOptions)
}
