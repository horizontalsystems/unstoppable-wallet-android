package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.databinding.ViewHolderTorControlBinding

class PrivacySettingsTorAdapter(private val listener: Listener) :
    RecyclerView.Adapter<PrivacySettingsTorAdapter.TorControlViewHolder>() {

    interface Listener {
        fun onTorSwitchChecked(checked: Boolean)
    }

    private var torStatus: TorStatus = TorStatus.Closed
    private var checked = false

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorControlViewHolder {
        return TorControlViewHolder(
            ViewHolderTorControlBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        ) { isChecked ->
            listener.onTorSwitchChecked(isChecked)
        }
    }

    override fun onBindViewHolder(holder: TorControlViewHolder, position: Int) {
        holder.bind(torStatus, checked)
    }

    fun setTorSwitch(checked: Boolean) {
        this.checked = checked
        notifyItemChanged(0)
    }

    fun bind(torStatus: TorStatus) {
        this.torStatus = torStatus
        notifyItemChanged(0)
    }

    class TorControlViewHolder(
        private val binding: ViewHolderTorControlBinding,
        private val onSwitch: (isChecked: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.torControlView.setOnClickListener {
                binding.torConnectionSwitch.isChecked = !binding.torConnectionSwitch.isChecked
            }
        }

        fun bind(torStatus: TorStatus, checked: Boolean) {

            binding.torConnectionSwitch.setOnCheckedChangeListener(null)
            binding.torConnectionSwitch.isChecked = checked
            binding.torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
                onSwitch.invoke(isChecked)
            }

            when (torStatus) {
                TorStatus.Connecting -> {
                    binding.connectionSpinner.isVisible = true
                    binding.controlIcon.setImageDrawable(null)
                    binding.subtitleText.text =
                        binding.wrapper.context.getString(R.string.TorPage_Connecting)
                }
                TorStatus.Connected -> {
                    binding.connectionSpinner.isVisible = false
                    binding.controlIcon.imageTintList = getTint(R.color.yellow_d)
                    binding.controlIcon.setImageResource(R.drawable.ic_tor_connection_success_24)
                    binding.subtitleText.text =
                        binding.wrapper.context.getString(R.string.TorPage_Connected)
                }
                TorStatus.Failed -> {
                    binding.connectionSpinner.isVisible = false
                    binding.controlIcon.imageTintList = getTint(R.color.yellow_d)
                    binding.controlIcon.setImageResource(R.drawable.ic_tor_connection_error_24)
                    binding.subtitleText.text =
                        binding.wrapper.context.getString(R.string.TorPage_Failed)
                }
                TorStatus.Closed -> {
                    binding.connectionSpinner.isVisible = false
                    binding.controlIcon.imageTintList = getTint(R.color.yellow_d)
                    binding.controlIcon.setImageResource(R.drawable.ic_tor_connection_24)
                    binding.subtitleText.text =
                        binding.wrapper.context.getString(R.string.TorPage_ConnectionClosed)
                }
            }

        }

        private fun getTint(color: Int) = binding.wrapper.context?.let {
            ColorStateList.valueOf(ContextCompat.getColor(it, color))
        }

    }
}
