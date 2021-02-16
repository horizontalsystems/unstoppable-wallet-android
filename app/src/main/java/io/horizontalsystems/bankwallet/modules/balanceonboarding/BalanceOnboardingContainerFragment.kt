package io.horizontalsystems.bankwallet.modules.balanceonboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.balance.BalanceFragment
import io.horizontalsystems.bankwallet.modules.onboarding.OnboardingFragment

class BalanceOnboardingContainerFragment: BaseFragment() {

    private val viewModel by viewModels<BalanceOnboardingViewModel> { BalanceOnboardingModule.Factory() }
    private val onboardingFragment by lazy { OnboardingFragment() }
    private val balanceFragment by lazy { BalanceFragment() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance_onboarding_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.hasAccountsLiveData.observe(viewLifecycleOwner, { hasAccounts ->
            childFragmentManager.commit {
                replace(R.id.fragmentContainerView, if (hasAccounts) balanceFragment else onboardingFragment)
                addToBackStack(null)
            }
        })
    }
}
