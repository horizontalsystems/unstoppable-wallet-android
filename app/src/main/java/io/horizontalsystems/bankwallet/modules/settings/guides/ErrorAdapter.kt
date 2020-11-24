package io.horizontalsystems.bankwallet.modules.settings.guides

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_error.*
import java.net.UnknownHostException

class ErrorAdapter : RecyclerView.Adapter<ErrorViewHolder>() {
    var error: Throwable? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrorViewHolder {
        return ErrorViewHolder(inflate(parent, R.layout.view_holder_error))
    }

    override fun getItemCount(): Int {
        return if (error != null) 1 else 0
    }

    override fun onBindViewHolder(holder: ErrorViewHolder, position: Int) {
        error?.let {
            holder.bind(it)
        }
    }

}

class ErrorViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(error: Throwable) {
        message.text =  when (error) {
            is UnknownHostException -> containerView.context.getString(R.string.Hud_Text_NoInternet)
            is LocalizedException -> containerView.context.getString(error.errorTextRes)
            else -> containerView.context.getString(R.string.Hud_UnknownError, error)
        }
    }
}
