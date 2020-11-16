package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_tor_control.*


class PrivacySettingsTorAdapter(private val listener: Listener) : RecyclerView.Adapter<PrivacySettingsTorAdapter.TorControlViewHolder>() {

    interface Listener {
        fun onTorSwitchChecked(checked: Boolean)
    }

    private var torStatus: TorStatus = TorStatus.Closed
    private var checked = false

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorControlViewHolder {
        return TorControlViewHolder.create(parent, onSwitch = { isChecked ->
            listener.onTorSwitchChecked(isChecked)
        })
    }

    override fun onBindViewHolder(holder: TorControlViewHolder, position: Int) {
        holder.bind(torStatus, checked)
    }

    fun setTorSwitch(checked: Boolean){
        this.checked = checked
        notifyItemChanged(0)
    }

    fun bind(torStatus: TorStatus) {
        this.torStatus = torStatus
        notifyItemChanged(0)
    }

    class TorControlViewHolder(
            override val containerView: View,
            private val onSwitch: (isChecked: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        init {
            torControlView.setOnClickListener {
                torConnectionSwitch.isChecked = !torConnectionSwitch.isChecked
            }
        }

        fun bind(torStatus: TorStatus, checked: Boolean) {

            torConnectionSwitch.setOnCheckedChangeListener(null)
            torConnectionSwitch.isChecked = checked
            torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
                onSwitch.invoke(isChecked)
            }

            when (torStatus) {
                TorStatus.Connecting -> {
                    connectionSpinner.isVisible = true
                    controlIcon.setImageDrawable(null)
                    subtitleText.text = containerView.context.getString(R.string.TorPage_Connecting)
                }
                TorStatus.Connected -> {
                    connectionSpinner.isVisible = false
                    controlIcon.imageTintList = getTint(R.color.yellow_d)
                    controlIcon.setImageResource(R.drawable.ic_tor_connection_success_24)
                    subtitleText.text = containerView.context.getString(R.string.TorPage_Connected)
                }
                TorStatus.Failed -> {
                    connectionSpinner.isVisible = false
                    controlIcon.imageTintList = getTint(R.color.yellow_d)
                    controlIcon.setImageResource(R.drawable.ic_tor_connection_error_24)
                    subtitleText.text = containerView.context.getString(R.string.TorPage_Failed)
                }
                TorStatus.Closed -> {
                    connectionSpinner.isVisible = false
                    controlIcon.imageTintList = getTint(R.color.yellow_d)
                    controlIcon.setImageResource(R.drawable.ic_tor_connection_24)
                    subtitleText.text = containerView.context.getString(R.string.TorPage_ConnectionClosed)
                }
            }

        }

        private fun getTint(color: Int) = containerView.context?.let { ColorStateList.valueOf(ContextCompat.getColor(it, color)) }


        companion object {
            const val layout = R.layout.view_holder_tor_control

            fun create(parent: ViewGroup, onSwitch: (isChecked: Boolean) -> Unit) = TorControlViewHolder(inflate(parent, layout, false), onSwitch)
        }

    }
}
