package cash.p.terminal.ui_compose

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

//  Fragment

fun Fragment.findNavController(): NavController {
    return NavHostFragment.findNavController(this)
}
