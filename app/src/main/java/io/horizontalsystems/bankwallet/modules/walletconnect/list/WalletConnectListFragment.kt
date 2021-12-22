package io.horizontalsystems.bankwallet.modules.walletconnect.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.FragmentWalletConnectListBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderWalletConnectAccountBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderWalletConnectSessionBinding
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListViewModel.WalletConnectViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.main.WalletConnectMainModule
import io.horizontalsystems.core.dp
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration

class WalletConnectListFragment : BaseFragment(), SessionViewHolder.Listener {
    private val viewModel by viewModels<WalletConnectListViewModel> { WalletConnectListModule.Factory() }

    private var snackbarInProcess: CustomSnackbar? = null

    private var _binding: FragmentWalletConnectListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentWalletConnectListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.newConnect.setOnSingleClickListener {
            startNewConnection()
        }

        val walletConnectListAdapter = WalletConnectListAdapter(this)
        binding.sessionsRecyclerView.adapter = walletConnectListAdapter

        val swipeHelper = SwipeHelper(binding.sessionsRecyclerView) { position ->
            val sessionViewItem =
                walletConnectListAdapter.items.getOrNull(position) as? WalletConnectViewItem.Session
            if (sessionViewItem != null) {
                listOf(
                    SwipeHelper.UnderlayButton(
                        requireContext(),
                        R.drawable.ic_trash_24,
                        16.dp,
                        32.dp,
                        17.dp
                    ) {
                        viewModel.onClickDelete(sessionViewItem)
                    })
            } else {
                listOf()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHelper)
        itemTouchHelper.attachToRecyclerView(binding.sessionsRecyclerView)

        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, { viewItems ->
            walletConnectListAdapter.items = viewItems
            walletConnectListAdapter.notifyDataSetChanged()
        })

        viewModel.startNewConnectionEvent.observe(viewLifecycleOwner, {
            startNewConnection()
        })

        viewModel.killingSessionInProcessLiveEvent.observe(viewLifecycleOwner, {
            snackbarInProcess = HudHelper.showInProcessMessage(
                requireView(),
                R.string.WalletConnect_Disconnecting,
                SnackbarDuration.INDEFINITE
            )
        })

        viewModel.killingSessionFailedLiveEvent.observe(viewLifecycleOwner, {
            snackbarInProcess?.dismiss()

            HudHelper.showErrorMessage(
                requireActivity().findViewById(android.R.id.content),
                it
            )
        })

        viewModel.killingSessionCompletedLiveEvent.observe(viewLifecycleOwner, {
            snackbarInProcess?.dismiss()

            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
        })
    }

    private fun startNewConnection() {
        WalletConnectMainModule.start(
            this,
            R.id.walletConnectListFragment_to_walletConnectMainFragment,
            navOptions(),
            viewModel.getSessionsCount()
        )
    }

    override fun onSessionClick(session: WalletConnectViewItem.Session) {
        WalletConnectMainModule.start(
            this,
            R.id.walletConnectListFragment_to_walletConnectMainFragment,
            navOptions(),
            remotePeerId = session.session.remotePeerId
        )
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
            accountViewType -> {
                AccountViewHolder(
                    ViewHolderWalletConnectAccountBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            }
            else -> {
                SessionViewHolder(
                    ViewHolderWalletConnectSessionBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ), listener)
            }
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

class AccountViewHolder(private val binding: ViewHolderWalletConnectAccountBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(account: WalletConnectViewItem.Account) {
        binding.accountTextView.text = account.title
    }

}

class SessionViewHolder(
    private val binding: ViewHolderWalletConnectSessionBinding,
    private val listener: Listener
) : RecyclerView.ViewHolder(binding.root) {

    interface Listener {
        fun onSessionClick(session: WalletConnectViewItem.Session)
    }

    fun bind(session: WalletConnectViewItem.Session) {
        binding.titleTextView.text = session.title
        binding.subtitleTextView.text = session.url
        binding.backgroundView.setBackgroundResource(session.position.getBackground())
        binding.iconImageView.loadImage(session.imageUrl)

        binding.wrapper.setOnSingleClickListener {
            listener.onSessionClick(session)
        }
    }

}
