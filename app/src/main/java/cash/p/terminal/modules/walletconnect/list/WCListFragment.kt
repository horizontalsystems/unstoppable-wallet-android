package cash.p.terminal.modules.walletconnect.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.walletconnect.list.ui.WCSessionsScreen
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class WCListFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val deepLinkUri = activity?.intent?.data?.toString()
        activity?.intent?.data = null

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    WCSessionsScreen(
                        findNavController(),
                        deepLinkUri
                    )
                }
            }
        }
    }
}
