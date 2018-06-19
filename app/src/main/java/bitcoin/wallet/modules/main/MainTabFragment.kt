package bitcoin.wallet.modules.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import bitcoin.wallet.R
import kotlinx.android.synthetic.main.collapsing_toolbar.*

abstract class MainTabFragment : Fragment() {

    abstract val toolbarTitle: String?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collapsingToolbar.setCollapsedTitleTypeface(ResourcesCompat.getFont(view.context, R.font.noto_sans_bold))
        collapsingToolbar.setExpandedTitleTypeface(ResourcesCompat.getFont(view.context, R.font.noto_sans_bold))

        collapsingToolbar.title = toolbarTitle
    }

}
