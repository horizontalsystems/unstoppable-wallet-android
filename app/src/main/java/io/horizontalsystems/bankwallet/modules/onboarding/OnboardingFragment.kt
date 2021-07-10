package io.horizontalsystems.bankwallet.modules.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController

class OnboardingFragment: BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_no_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnCreate).setOnClickListener {
            findNavController().navigate(R.id.createAccountFragment, null, navOptions())
        }
        view.findViewById<Button>(R.id.btnRestore).setOnClickListener {
            findNavController().navigate(R.id.restoreMnemonicFragment, null, navOptions())
        }
    }
}
