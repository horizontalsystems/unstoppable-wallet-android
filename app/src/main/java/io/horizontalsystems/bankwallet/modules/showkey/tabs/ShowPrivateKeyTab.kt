package io.horizontalsystems.bankwallet.modules.showkey.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentShowPrivateKeyTabBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderPrivateKeyBinding
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper

class ShowPrivateKeyFragment : BaseFragment() {

    private var _binding: FragmentShowPrivateKeyTabBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowPrivateKeyTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val privateKeys = arguments?.getParcelableArrayList<ShowKeyModule.PrivateKey>(PRIVATE_KEYS)
            ?: listOf()

        binding.recyclerView.adapter = PrivateKeysAdapter(privateKeys, onClick = { key ->
            showPrivateKeyCopyWarning(key)
        })
    }

    private fun showPrivateKeyCopyWarning(key: ShowKeyModule.PrivateKey) {
        ConfirmationDialog.show(
            title = getString(R.string.ShowKey_PrivateKeyCopyWarning_Title),
            subtitle = getString(R.string.ShowKey_PrivateKeyCopyWarning_Subtitle),
            contentText = getString(R.string.ShowKey_PrivateKeyCopyWarning_Text),
            destructiveButtonTitle = getString(R.string.ShowKey_PrivateKeyCopyWarning_Proceed),
            actionButtonTitle = null,
            cancelButtonTitle = null,
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onDestructiveButtonClick() {
                    TextHelper.copyText(key.value)
                    HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
                }
            }
        )
    }

    companion object {
        private const val PRIVATE_KEYS = "privateKeys"

        fun getInstance(privateKeys: List<ShowKeyModule.PrivateKey>): ShowPrivateKeyFragment {
            val fragment = ShowPrivateKeyFragment()

            val arguments = bundleOf()
            arguments.putParcelableArrayList(PRIVATE_KEYS, ArrayList(privateKeys))
            fragment.arguments = arguments
            return fragment
        }
    }
}

class PrivateKeysAdapter(
    private val privateKeys: List<ShowKeyModule.PrivateKey>,
    private val onClick: (ShowKeyModule.PrivateKey) -> Unit
) : RecyclerView.Adapter<PrivateKeyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivateKeyViewHolder {
        return PrivateKeyViewHolder(
            ViewHolderPrivateKeyBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), onClick)
    }

    override fun onBindViewHolder(holder: PrivateKeyViewHolder, position: Int) {
        holder.bind(privateKeys[position])
    }

    override fun getItemCount(): Int {
        return privateKeys.size
    }
}

class PrivateKeyViewHolder(
    private val binding: ViewHolderPrivateKeyBinding,
    private val onClick: (ShowKeyModule.PrivateKey) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(key: ShowKeyModule.PrivateKey) {
        binding.blockchain.text = key.blockchain

        binding.valueCompose.setContent {
            ComposeAppTheme {
                ButtonSecondary(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = ComposeAppTheme.colors.steel20,
                        contentColor = ComposeAppTheme.colors.oz
                    ),
                    content = { Text(key.value, textAlign = TextAlign.Center, maxLines = 2) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    onClick = {
                        onClick(key)
                    }
                )
            }
        }
    }

}
