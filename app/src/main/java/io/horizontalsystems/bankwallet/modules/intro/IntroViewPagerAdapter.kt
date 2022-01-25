package io.horizontalsystems.bankwallet.modules.intro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.horizontalsystems.bankwallet.R

class IntroViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val fragments = listOf<Fragment>(
            IntroSlideFragment.newInstance(R.drawable.ic_independence),
            IntroSlideFragment.newInstance(R.drawable.ic_knowledge),
            IntroSlideFragment.newInstance(R.drawable.ic_privacy),
    )

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position]

}
