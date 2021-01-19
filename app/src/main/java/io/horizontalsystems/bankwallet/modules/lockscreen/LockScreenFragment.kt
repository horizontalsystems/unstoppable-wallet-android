package io.horizontalsystems.bankwallet.modules.lockscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentResultListener
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.ratelist.RatesListFragment
import io.horizontalsystems.bankwallet.modules.ratelist.RatesTopListFragment
import io.horizontalsystems.pin.PinFragment
import io.horizontalsystems.pin.PinInteractionType
import io.horizontalsystems.pin.PinModule
import kotlinx.android.synthetic.main.fragment_lockscreen.*

class LockScreenFragment : BaseFragment(), FragmentResultListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_lockscreen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragments = listOf(
                PinFragment().apply {
                    arguments = Bundle(1).apply {
                        putBoolean(PinFragment.ATTACHED_TO_LOCKSCREEN, true)
                    }
                },
                RatesListFragment(),
                RatesTopListFragment()
        )

        viewPager.offscreenPageLimit = 1
        viewPager.adapter = LockScreenViewPagerAdapter(fragments, childFragmentManager)

        circleIndicator.setViewPager(viewPager)

        childFragmentManager.setFragmentResultListener(PinModule.requestKey, this, this)
    }

    //  FragmentResultListener

    override fun onFragmentResult(requestKey: String, bundle: Bundle) {
        val resultType = bundle.getParcelable<PinInteractionType>(PinModule.requestType)
        if (resultType == PinInteractionType.UNLOCK) {
            when (bundle.getInt(PinModule.requestResult)) {
                PinModule.RESULT_OK -> activity?.setResult(PinModule.RESULT_OK)
                PinModule.RESULT_CANCELLED -> activity?.setResult(PinModule.RESULT_CANCELLED)
            }

            activity?.finish()
        }
    }
}
