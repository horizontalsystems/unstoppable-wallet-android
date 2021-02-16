package io.horizontalsystems.bankwallet.modules.intro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.horizontalsystems.bankwallet.R

class IntroViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val fragments = listOf<Fragment>(
            IntroSlideFragment.newInstance(null, R.string.Intro_Wallet_Description),
            IntroSlideFragment.newInstance(R.string.Intro_Independence_Title, R.string.Intro_Independence_Description),
            IntroSlideFragment.newInstance(R.string.Intro_Privacy_Title, R.string.Intro_Privacy_Description),
            IntroSlideFragment.newInstance(R.string.Intro_Invest_Title, R.string.Intro_Invest_Description)
    )

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position]

}
