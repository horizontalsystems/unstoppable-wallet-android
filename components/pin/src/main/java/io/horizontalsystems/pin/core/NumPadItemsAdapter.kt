package io.horizontalsystems.pin.core

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.horizontalsystems.pin.R

class NumPadItemsAdapter(private val listener: Listener, bottomLeftButtonType: NumPadItemType, private val showLetters: Boolean = true) : Adapter<ViewHolder>() {

    interface Listener {
        fun onItemClick(item: NumPadItem)
    }

    val numPadItems = listOf(
            NumPadItem(NumPadItemType.NUMBER, 1, ""),
            NumPadItem(NumPadItemType.NUMBER, 2, "abc"),
            NumPadItem(NumPadItemType.NUMBER, 3, "def"),
            NumPadItem(NumPadItemType.NUMBER, 4, "ghi"),
            NumPadItem(NumPadItemType.NUMBER, 5, "jkl"),
            NumPadItem(NumPadItemType.NUMBER, 6, "mno"),
            NumPadItem(NumPadItemType.NUMBER, 7, "pqrs"),
            NumPadItem(NumPadItemType.NUMBER, 8, "tuv"),
            NumPadItem(NumPadItemType.NUMBER, 9, "wxyz"),
            NumPadItem(bottomLeftButtonType, 0, "Bottom Left"),
            NumPadItem(NumPadItemType.NUMBER, 0, ""),
            NumPadItem(NumPadItemType.DELETE, 0, "Bottom Right")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return NumPadItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_numpad_button, parent, false))
    }

    override fun getItemCount() = numPadItems.count()

    var showBiometricAuthButton = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var numpadEnabled = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is NumPadItemViewHolder) {
            holder.bind(numPadItems[position], numpadEnabled, showBiometricAuthButton, showLetters) { listener.onItemClick(numPadItems[position]) }
        }
    }

}


data class NumPadItem(val type: NumPadItemType, val number: Int, val letters: String)

enum class NumPadItemType {
    NUMBER, DELETE, BIOMETRIC
}

class NumPadItemViewHolder(itemView: View) : ViewHolder(itemView) {

    private var txtNumber: TextView = itemView.findViewById(R.id.txtNumPadNumber)
    private var txtLetters: TextView = itemView.findViewById(R.id.txtNumPadText)
    private var imgBackSpace: ImageView = itemView.findViewById(R.id.imgBackSpace)
    private var imgFingerprint: ImageView = itemView.findViewById(R.id.imgFingerprint)


    fun bind(item: NumPadItem, isEnabled: Boolean, isBiometricAuthEnabled: Boolean, showLetters: Boolean, onClick: () -> (Unit)) {

        itemView.isEnabled = isEnabled

        itemView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    true
                }
                MotionEvent.ACTION_UP -> {
                    onClick.invoke()
                    v.isPressed = false
                    true
                }
                else -> false
            }
        }

        txtNumber.visibility = View.GONE
        txtLetters.visibility = View.GONE
        imgBackSpace.visibility = View.GONE
        imgFingerprint.visibility = View.GONE

        when (item.type) {
            NumPadItemType.DELETE -> {
                itemView.background = null
                imgBackSpace.isVisible = true
            }

            NumPadItemType.NUMBER -> {
                txtNumber.isVisible = true
                txtLetters.isGone = item.number == 0 || !showLetters
                txtNumber.text = item.number.toString()
                txtLetters.text = item.letters
                itemView.setBackgroundResource(R.drawable.numpad_circle)
            }

            NumPadItemType.BIOMETRIC -> {
                itemView.background = null
                imgFingerprint.isVisible = isBiometricAuthEnabled
            }

        }
    }
}
