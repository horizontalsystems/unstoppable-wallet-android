package io.horizontalsystems.bankwallet.modules.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_no_wallet.*

class OnboardingFragment: BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_no_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnCreate.setOnClickListener {
            val arguments = Bundle(1).apply {
                putParcelable("predefinedAccountType", null)
            }
            findNavController().navigate(R.id.mainFragment_to_createWalletFragment, arguments, navOptions())
        }
        btnRestore.setOnClickListener {
            val arguments = Bundle(2).apply {
                putParcelable(RestoreFragment.PREDEFINED_ACCOUNT_TYPE_KEY, null)
                putBoolean(RestoreFragment.SELECT_COINS_KEY, true)
            }
            findNavController().navigate(R.id.mainFragment_to_restoreFragment, arguments, navOptions())
        }
    }
}
