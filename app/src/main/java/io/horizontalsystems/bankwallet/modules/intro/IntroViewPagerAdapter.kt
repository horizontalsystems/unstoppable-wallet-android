package io.horizontalsystems.bankwallet.modules.intro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.horizontalsystems.bankwallet.R

class IntroViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val fragments = listOf<Fragment>(
            IntroSlideFragment.newInstance(null, R.string.Intro_Wallet_Screen1Description),
            IntroSlideFragment.newInstance(R.string.Intro_Wallet_Screen2Title, R.string.Intro_Wallet_Screen2Description),
            IntroSlideFragment.newInstance(R.string.Intro_Wallet_Screen3Title, R.string.Intro_Wallet_Screen3Description),
            IntroSlideFragment.newInstance(R.string.Intro_Wallet_Screen4Title, R.string.Intro_Wallet_Screen4Description)
    )

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position]

}
