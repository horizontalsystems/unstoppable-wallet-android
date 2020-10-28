package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoViewModel
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider.DataProviderSettingsFragment
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_full_transaction_info.*
import kotlinx.android.synthetic.main.view_holder_full_transaction.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_item.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_link.*
import kotlinx.android.synthetic.main.view_holder_full_transaction_source.*

class FullTransactionInfoFragment : BaseFragment(), FullTransactionInfoErrorFragment.Listener {

    private lateinit var viewModel: FullTransactionInfoViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_full_transaction_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionHash = arguments?.getString(TRANSACTION_HASH_KEY) ?: run {
            parentFragmentManager.popBackStack()
            return
        }
        val wallet = arguments?.getParcelable<Wallet>(WALLET_KEY) ?: run {
            parentFragmentManager.popBackStack()
            return
        }

        val transactionRecordAdapter = SectionViewAdapter()

        viewModel = ViewModelProvider(this).get(FullTransactionInfoViewModel::class.java)
        viewModel.init(transactionHash, wallet)

        transactionIdView.text = transactionHash

        transactionIdView.setOnClickListener {
            viewModel.delegate.onTapId()
        }

        shadowlessToolbar.bind(
                title = getString(R.string.FullInfo_Title),
                rightBtnItem = TopMenuItem(text = R.string.Button_Close, onClick = { parentFragmentManager.popBackStack() })
        )

        //
        // LiveData
        //
        viewModel.shareButtonVisibility.observe(viewLifecycleOwner, Observer { visible ->
            shadowlessToolbar.bindLeftButton(
                    leftBtnItem = if (visible) TopMenuItem(text = R.string.Button_Share, onClick = { viewModel.share() }) else null
            )
        })

        viewModel.reloadEvent.observe(viewLifecycleOwner, Observer {
            transactionRecordAdapter.notifyDataSetChanged()
        })

        viewModel.showTransactionInfoEvent.observe(viewLifecycleOwner, Observer {
            setVisible(recyclerTransactionInfo)
        })

        viewModel.showLoadingEvent.observe(viewLifecycleOwner, Observer {
            setVisible(progressLoading)
        })

        viewModel.showCopiedEvent.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
        })

        viewModel.openLinkEvent.observe(viewLifecycleOwner, Observer { url ->
            url?.let {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        })

        viewModel.openProviderSettingsEvent.observe(viewLifecycleOwner, Observer { coin ->
            val fragment = DataProviderSettingsFragment.instance(coin)
            parentFragmentManager.commit {
                add(R.id.topFragmentContainerView, fragment)
                addToBackStack(null)
            }
        })

        viewModel.showErrorProviderOffline.observe(viewLifecycleOwner, Observer { providerName ->
            setError(providerName, R.string.FullInfo_Error_ProviderOffline, R.drawable.dragon_icon, true)
            setVisible(errorContainer)
        })

        viewModel.showErrorTransactionNotFound.observe(viewLifecycleOwner, Observer { providerName ->
            setError(providerName, R.string.FullInfo_Error_TransactionNotFound, R.drawable.ic_attention, false)
            setVisible(errorContainer)
        })

        viewModel.showShareEvent.observe(viewLifecycleOwner, Observer { url ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            startActivity(sendIntent)
        })

        recyclerTransactionInfo.hasFixedSize()
        recyclerTransactionInfo.adapter = transactionRecordAdapter
        recyclerTransactionInfo.layoutManager = LinearLayoutManager(context)

        transactionRecordAdapter.viewModel = viewModel

        activity?.onBackPressedDispatcher?.addCallback(this) {
            parentFragmentManager.popBackStack()
        }
    }

    //
    // FullTransactionInfoErrorFragment Listener
    //
    override fun onRetry() {
        viewModel.retry()
    }

    override fun onChangeProvider() {
        viewModel.changeProvider()
    }

    private fun setError(providerName: String, errorText: Int, icon: Int, showRetry: Boolean) {
        val errorMessage = getString(errorText)
        val fragment = FullTransactionInfoErrorFragment.newInstance(providerName, errorMessage, icon, showRetry)
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.errorContainer, fragment)
        transaction.commit()
    }

    private fun setVisible(view: View) {
        listOf(errorContainer, progressLoading, recyclerTransactionInfo).forEach {
            it.isInvisible = it != view
        }
    }

    companion object {
        const val TRANSACTION_HASH_KEY = "transaction_hash_key"
        const val WALLET_KEY = "wallet_key"

        fun instance(transactionHash: String, wallet: Wallet): FullTransactionInfoFragment {
            return FullTransactionInfoFragment().apply {
                arguments = Bundle(2).apply {
                    putString(TRANSACTION_HASH_KEY, transactionHash)
                    putParcelable(WALLET_KEY, wallet)
                }
            }
        }
    }
}

class SectionViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    lateinit var viewModel: FullTransactionInfoViewModel

    private val sectionViewSource = 0
    private val sectionView = 1
    private val sectionViewLink = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)

        return if (viewType == sectionViewSource) {
            SectionSourceViewHolder(view.inflate(R.layout.view_holder_full_transaction_source, parent, false))
        } else if (viewType == sectionView) {
            SectionViewHolder(view.inflate(R.layout.view_holder_full_transaction, parent, false))
        } else {
            SectionLinkViewHolder(view.inflate(R.layout.view_holder_full_transaction_link, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            sectionViewSource
        } else if (position == itemCount - 1) {
            sectionViewLink
        } else {
            sectionView
        }
    }

    override fun getItemCount(): Int {
        return viewModel.delegate.sectionCount + 2
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val providerName = viewModel.delegate.providerName

        when (holder) {
            is SectionSourceViewHolder -> {
                holder.transactionSource.bindSourceProvider(holder.containerView.context.getString(R.string.FullInfo_Source), providerName)
                holder.transactionSource.setOnClickListener {
                    viewModel.delegate.onTapProvider()
                }
            }
            is SectionViewHolder -> {
                val posWithoutSource = position - 1

                viewModel.delegate.getSection(posWithoutSource)?.let { section ->
                    holder.sectionRecyclerView.hasFixedSize()
                    holder.sectionRecyclerView.isNestedScrollingEnabled = false

                    holder.sectionRecyclerView.layoutManager = LinearLayoutManager(holder.containerView.context)
                    holder.sectionRecyclerView.adapter = SectionItemViewAdapter(viewModel, section.items)
                }

            }
            is SectionLinkViewHolder -> {
                providerName?.let {
                    holder.transactionLink.isVisible = viewModel.delegate.canShowTransactionInProviderSite

                    if (viewModel.delegate.canShowTransactionInProviderSite) {
                        val changeProviderStyle = SpannableString(providerName)
                        changeProviderStyle.setSpan(UnderlineSpan(), 0, changeProviderStyle.length, 0)

                        holder.transactionLink.text = changeProviderStyle
                        holder.transactionLink.setOnClickListener {
                            viewModel.delegate.onTapResource()
                        }
                    }
                }
            }
        }
    }
}

class SectionItemViewAdapter(val viewModel: FullTransactionInfoViewModel, val items: List<FullTransactionItem>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_holder_full_transaction_item, parent, false)

        return SectionItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionItemViewHolder -> {
                val item = items[position]
                val notLast = items.size != position + 1

                bindTransaction(item, notLast, holder.sectionItem)
            }
        }
    }

    private fun bindTransaction(item: FullTransactionItem, showBorder: Boolean, viewItem: FullTransactionInfoItemView) {
        val title = if (item.titleResId != null) viewItem.context.getString(item.titleResId) else item.title

        viewItem.bind(title, item.value, item.icon, item.dimmed, showBorder)

        if (item.clickable) {
            viewItem.setOnClickListener {
                viewModel.delegate.onTapItem(item)
            }
        }
    }
}

class SectionViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionSourceViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionLinkViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class SectionItemViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
