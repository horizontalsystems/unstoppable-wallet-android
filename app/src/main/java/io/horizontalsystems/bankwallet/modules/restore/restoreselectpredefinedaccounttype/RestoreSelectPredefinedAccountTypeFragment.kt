package io.horizontalsystems.bankwallet.modules.restore.restoreselectpredefinedaccounttype

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_restore_select_predefined_account_type.*
import kotlinx.android.synthetic.main.fragment_restore_select_predefined_account_type.toolbar
import kotlinx.android.synthetic.main.view_holder_account_restore.*


class RestoreSelectPredefinedAccountTypeFragment: BaseFragment(), RestoreNavigationAdapter.Listener {

    private lateinit var adapter: RestoreNavigationAdapter
    private lateinit var viewModel: RestoreSelectPredefinedAccountTypeViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_restore_select_predefined_account_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel = ViewModelProvider(this, RestoreSelectPredefinedAccountTypeModule.Factory())
                .get(RestoreSelectPredefinedAccountTypeViewModel::class.java)

        adapter = RestoreNavigationAdapter(this)
        adapter.items = viewModel.viewItems
        recyclerView.adapter = adapter
    }

    override fun onSelect(predefinedAccountType: PredefinedAccountType) {
        setFragmentResult(RestoreFragment.selectPredefinedAccountTypeRequestKey, bundleOf(RestoreFragment.predefinedAccountTypeBundleKey to predefinedAccountType))
    }
}



class RestoreNavigationAdapter(private val listener: Listener)
    : RecyclerView.Adapter<KeysViewHolder>() {

    interface Listener {
        fun onSelect(predefinedAccountType: PredefinedAccountType)
    }

    var items = listOf<RestoreSelectPredefinedAccountTypeViewModel.ViewItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeysViewHolder {
        return KeysViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_account_restore, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: KeysViewHolder, position: Int) {
        val viewItem = items[position]
        holder.bind(viewItem)
        holder.viewHolderRoot.setOnClickListener {
            listener.onSelect(viewItem.predefinedAccountType)
        }
    }
}

class KeysViewHolder(override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(viewItem: RestoreSelectPredefinedAccountTypeViewModel.ViewItem) {
        val accountTypeTitle = containerView.resources.getString(viewItem.title)
        accountName.text = containerView.resources.getString(R.string.Wallet, accountTypeTitle)
        accountCoin.text = containerView.resources.getString(viewItem.coinCodes)
    }
}
