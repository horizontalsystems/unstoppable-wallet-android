package io.horizontalsystems.bankwallet.modules.intro

import android.content.res.Configuration
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

class IntroViewPagerAdapter(fragmentManager: FragmentManager) :
    FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val nightMode by lazy {
        val uiMode =
            App.instance.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
        uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    private val fragments = listOf<Fragment>(
        IntroSlideFragment.newInstance(if (nightMode) R.drawable.ic_independence else R.drawable.ic_independence_light),
        IntroSlideFragment.newInstance(if (nightMode) R.drawable.ic_knowledge else R.drawable.ic_knowledge_light),
        IntroSlideFragment.newInstance(if (nightMode) R.drawable.ic_privacy else R.drawable.ic_privacy_light),
    )

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position]

}
