package io.horizontalsystems.bankwallet.modules.settings.guides

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.databinding.ViewHolderErrorBinding
import java.net.UnknownHostException

class ErrorAdapter : RecyclerView.Adapter<ErrorViewHolder>() {
    var error: Throwable? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrorViewHolder {
        return ErrorViewHolder(
            ViewHolderErrorBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
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

class ErrorViewHolder(private val binding: ViewHolderErrorBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(error: Throwable) {
        binding.message.text = when (error) {
            is UnknownHostException -> binding.message.context.getString(R.string.Hud_Text_NoInternet)
            is LocalizedException -> binding.message.context.getString(error.errorTextRes)
            else -> binding.message.context.getString(R.string.Hud_UnknownError, error)
        }
    }
}
