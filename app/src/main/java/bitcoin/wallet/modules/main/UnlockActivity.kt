package bitcoin.wallet.modules.main

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.core.App
import kotlinx.android.synthetic.main.activity_unlock.*

class UnlockActivity : AppCompatActivity(), NumPadItemsAdapter.Listener {

    private var enteredPin: StringBuilder = StringBuilder("")
    private lateinit var imgPinMask1: ImageView
    private lateinit var imgPinMask2: ImageView
    private lateinit var imgPinMask3: ImageView
    private lateinit var imgPinMask4: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_unlock)

        imgPinMask1 = findViewById(R.id.imgPinMaskOne)
        imgPinMask2 = findViewById(R.id.imgPinMaskTwo)
        imgPinMask3 = findViewById(R.id.imgPinMaskThree)
        imgPinMask4 = findViewById(R.id.imgPinMaskFour)

        numPadItems.adapter = NumPadItemsAdapter(listOf(
                NumPadItem(NumPadItemType.NUMBER, 1, ""),
                NumPadItem(NumPadItemType.NUMBER, 2, "abc"),
                NumPadItem(NumPadItemType.NUMBER, 3, "def"),
                NumPadItem(NumPadItemType.NUMBER, 4, "ghi"),
                NumPadItem(NumPadItemType.NUMBER, 5, "jkl"),
                NumPadItem(NumPadItemType.NUMBER, 6, "mno"),
                NumPadItem(NumPadItemType.NUMBER, 7, "pqrs"),
                NumPadItem(NumPadItemType.NUMBER, 8, "tuv"),
                NumPadItem(NumPadItemType.NUMBER, 9, "wxyz"),
                NumPadItem(NumPadItemType.NONE, 0, ""),
                NumPadItem(NumPadItemType.NUMBER, 0, ""),
                NumPadItem(NumPadItemType.DELETE, 0, "DEL")
        ), this)

        numPadItems.layoutManager = GridLayoutManager(this, 3)
    }

    override fun onItemClick(item: NumPadItem) {

        when (item.type) {
            NumPadItemType.NUMBER -> {
                if (enteredPin.length < 4) {
                    enteredPin.append(item.number)
                    updatePinCircles()

                    if (enteredPin.toString() == "1234") {
                        unlockPage()
                    } else if (enteredPin.length == 4) {
                        Handler().postDelayed({
                            enteredPin.setLength(0)
                            updatePinCircles()
                        }, 200)
                    }
                }
            }
            NumPadItemType.DELETE -> {
                if (enteredPin.isNotEmpty()) {
                    enteredPin.deleteCharAt(enteredPin.lastIndex)
                    updatePinCircles()
                }
            }
            else -> {
            }
        }
    }

    private fun unlockPage() {
        if (enteredPin.toString() == "1234") {
            App.promptPin = false
            finish()
        }
    }

    private fun updatePinCircles() {
        val length = enteredPin.length
        val filledCircle = R.drawable.pin_circle_filled
        val emptyCircle = R.drawable.pin_circle_empty

        imgPinMask1.setImageResource(if (length > 0) filledCircle else emptyCircle)
        imgPinMask2.setImageResource(if (length > 1) filledCircle else emptyCircle)
        imgPinMask3.setImageResource(if (length > 2) filledCircle else emptyCircle)
        imgPinMask4.setImageResource(if (length > 3) filledCircle else emptyCircle)
    }
}

enum class NumPadItemType {
    NUMBER, DELETE, NONE
}

data class NumPadItem(val type: NumPadItemType, val number: Int, val letters: String)

class NumPadItemsAdapter(private val numPadItems: List<NumPadItem>, private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(item: NumPadItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return NumPadItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_numpad_button, parent, false))
    }

    override fun getItemCount() = numPadItems.count()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NumPadItemViewHolder) {
            holder.bind(numPadItems[position], { listener.onItemClick(numPadItems[position]) })
        }
    }
}

class NumPadItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var txtNumber: TextView = itemView.findViewById(R.id.txtNumPadNumber)
    private var txtLetters: TextView = itemView.findViewById(R.id.txtNumPadText)


    fun bind(item: NumPadItem, onClick: () -> (Unit)) {

        itemView.setOnClickListener { onClick.invoke() }

        when (item.type) {
            NumPadItemType.DELETE -> {
                txtNumber.visibility = View.GONE
                txtLetters.visibility = View.VISIBLE
                txtLetters.text = item.letters
            }
            NumPadItemType.NUMBER -> {
                txtNumber.visibility = View.VISIBLE
                txtLetters.visibility = View.VISIBLE
                txtNumber.text = item.number.toString()
                txtLetters.text = item.letters
            }
            NumPadItemType.NONE -> {
                txtNumber.visibility = View.GONE
                txtLetters.visibility = View.GONE
            }
        }
    }
}

