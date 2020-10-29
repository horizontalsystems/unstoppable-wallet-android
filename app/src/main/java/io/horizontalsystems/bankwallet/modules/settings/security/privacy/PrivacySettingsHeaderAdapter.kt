package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer

class PrivacySettingsHeaderAdapter : RecyclerView.Adapter<PrivacySettingsHeaderAdapter.DescriptionViewHolder>() {

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DescriptionViewHolder {
        return DescriptionViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: DescriptionViewHolder, position: Int) { }

    class DescriptionViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        companion object {
            const val layout = R.layout.view_holder_privacy_top_header

            fun create(parent: ViewGroup) = DescriptionViewHolder(inflate(parent, layout, false))
        }

    }
}
