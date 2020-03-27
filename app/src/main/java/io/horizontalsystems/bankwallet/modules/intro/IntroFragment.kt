package io.horizontalsystems.bankwallet.modules.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.fragment_intro.*

class IntroFragment(private val titleResId: Int, private val descriptionResId: Int) : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_intro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title.text = getString(titleResId)
        description.text = getString(descriptionResId)
    }
}
