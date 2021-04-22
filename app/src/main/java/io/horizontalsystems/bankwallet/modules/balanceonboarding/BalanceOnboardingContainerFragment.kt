package io.horizontalsystems.bankwallet.modules.balanceonboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.balance.BalanceFragment
import io.horizontalsystems.bankwallet.modules.balance.BalanceNoCoinsFragment
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingViewModel.BalanceViewType
import io.horizontalsystems.bankwallet.modules.onboarding.OnboardingFragment

class BalanceOnboardingContainerFragment : BaseFragment() {

    private val viewModel by viewModels<BalanceOnboardingViewModel> { BalanceOnboardingModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance_onboarding_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.balanceViewTypeLiveData.observe(viewLifecycleOwner, { balancePageType ->
            childFragmentManager.commit {
                var args: Bundle? = null
                val balanceViewFragment = when (balancePageType) {
                    BalanceViewType.NoAccounts -> OnboardingFragment::class.java
                    is BalanceViewType.NoCoins -> {
                        args = bundleOf(BalanceNoCoinsFragment.ACCOUNT_NAME to balancePageType.accountName)
                        BalanceNoCoinsFragment::class.java
                    }
                    BalanceViewType.Balance -> BalanceFragment::class.java
                }
                replace(R.id.fragmentContainerView, balanceViewFragment, args)
                addToBackStack(null)
            }
        })
    }

}
