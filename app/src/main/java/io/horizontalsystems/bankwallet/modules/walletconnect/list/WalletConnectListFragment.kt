package io.horizontalsystems.bankwallet.modules.walletconnect.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel.WalletConnectViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.main.WalletConnectMainModule
import io.horizontalsystems.core.findNavController
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_notifications.toolbar
import kotlinx.android.synthetic.main.fragment_wallet_connect_list.*
import kotlinx.android.synthetic.main.view_holder_wallet_connect_account.*
import kotlinx.android.synthetic.main.view_holder_wallet_connect_session.*

class WalletConnectListFragment : BaseFragment(), SessionViewHolder.Listener {
    private val viewModel by viewModels<WalletConnectListViewModel> { WalletConnectListModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_connect_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        newConnect.setOnSingleClickListener {
            WalletConnectMainModule.start(this, R.id.walletConnectListFragment_to_walletConnectMainFragment, navOptions())
        }

        val walletConnectListAdapter = WalletConnectListAdapter(this)
        sessionsRecyclerView.adapter = walletConnectListAdapter

        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, { viewItems ->
            walletConnectListAdapter.items = viewItems
            walletConnectListAdapter.notifyDataSetChanged()
        })
    }

    override fun onSessionClick(session: WalletConnectViewItem.Session) {
        WalletConnectMainModule.start(this, R.id.walletConnectListFragment_to_walletConnectMainFragment, navOptions(), session.session.remotePeerId)
    }
}

class WalletConnectListAdapter(
        private val listener: SessionViewHolder.Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items = listOf<WalletConnectViewItem>()

    private val accountViewType = 1
    private val sessionViewType = 2

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is WalletConnectViewItem.Account -> accountViewType
            is WalletConnectViewItem.Session -> sessionViewType
        }
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            accountViewType -> AccountViewHolder.create(parent)
            sessionViewType -> SessionViewHolder.create(parent, listener)
            else -> throw Exception("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is WalletConnectViewItem.Account -> (holder as? AccountViewHolder)?.bind(item)
            is WalletConnectViewItem.Session -> (holder as? SessionViewHolder)?.bind(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

}

class AccountViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(account: WalletConnectViewItem.Account) {
        accountTextView.text = account.title
        addressTextView.text = account.address
    }

    companion object {
        fun create(parent: ViewGroup): AccountViewHolder {
            return AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_wallet_connect_account, parent, false))
        }
    }
}

class SessionViewHolder(
        override val containerView: View,
        private val listener: Listener
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    interface Listener {
        fun onSessionClick(session: WalletConnectViewItem.Session)
    }

    fun bind(session: WalletConnectViewItem.Session) {
        titleTextView.text = session.title
        subtitleTextView.text = session.url
        backgroundView.setBackgroundResource(session.position.getBackground())
        iconImageView.loadImage(session.imageUrl)

        containerView.setOnSingleClickListener {
            listener.onSessionClick(session)
        }
    }

    companion object {
        fun create(parent: ViewGroup, listener: Listener): SessionViewHolder {
            return SessionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_wallet_connect_session, parent, false), listener)
        }
    }
}
