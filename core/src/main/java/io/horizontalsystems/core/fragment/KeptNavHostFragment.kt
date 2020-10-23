package io.horizontalsystems.core.fragment

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigator

class KeptNavHostFragment : NavHostFragment() {
    override fun createFragmentNavigator(): Navigator<out FragmentNavigator.Destination> {
        return FragmentNavigator(requireContext(), childFragmentManager, id)
    }

    companion object {
        fun findNavController(fragment: Fragment): NavController =
                NavHostFragment.findNavController(fragment)
    }
}
