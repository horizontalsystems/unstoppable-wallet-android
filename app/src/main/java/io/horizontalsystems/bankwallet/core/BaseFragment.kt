package io.horizontalsystems.bankwallet.core

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import io.horizontalsystems.bankwallet.R

abstract class BaseFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.slide_right)
    }
}
