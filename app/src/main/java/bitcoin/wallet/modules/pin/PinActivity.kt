package bitcoin.wallet.modules.pin

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.viewHelpers.HudHelper
import bitcoin.wallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_pin.*


class PinActivity : AppCompatActivity(), NumPadItemsAdapter.Listener {

    private lateinit var viewModel: PinViewModel

    private lateinit var imgPinMask1: ImageView
    private lateinit var imgPinMask2: ImageView
    private lateinit var imgPinMask3: ImageView
    private lateinit var imgPinMask4: ImageView
    private lateinit var imgPinMask5: ImageView
    private lateinit var imgPinMask6: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(if (Factory.preferencesManager.isLightModeEnabled) R.style.LightModeAppTheme else R.style.DarkModeAppTheme)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.activity_pin)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val interactionType = intent.getSerializableExtra(keyInteractionType) as PinInteractionType
        val enteredPin = intent.getStringExtra(keyEnteredPin)

        viewModel = ViewModelProviders.of(this).get(PinViewModel::class.java)
        viewModel.init(interactionType, enteredPin)

        viewModel.setTitleLiveData.observe(this, Observer { title ->
            title?.let { supportActionBar?.title = getString(it) }
        })

        viewModel.setDescriptionLiveData.observe(this, Observer { description ->
            description?.let { txtPromptPin.setText(it) }
        })

        viewModel.highlightPinMaskLiveData.observe(this, Observer { length ->
            length?.let { updatePinCircles(it) }
        })

        viewModel.showErrorLiveData.observe(this, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(error, this)
            }
        })

        viewModel.goToPinConfirmationLiveEvent.observe(this, Observer { pin ->
            pin?.let {
                PinModule.startForSetPinConfirm(this, pin)
                finish()
            }
        })

        viewModel.showSuccessLiveData.observe(this, Observer { success ->
            success?.let {
                HudHelper.showSuccessMessage(it, this)
                finish()
            }
        })

        imgPinMask1 = findViewById(R.id.imgPinMaskOne)
        imgPinMask2 = findViewById(R.id.imgPinMaskTwo)
        imgPinMask3 = findViewById(R.id.imgPinMaskThree)
        imgPinMask4 = findViewById(R.id.imgPinMaskFour)
        imgPinMask5 = findViewById(R.id.imgPinMaskFive)
        imgPinMask6 = findViewById(R.id.imgPinMaskSix)

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
                NumPadItem(NumPadItemType.FINGER, 0, "FINGER"),
                NumPadItem(NumPadItemType.NUMBER, 0, ""),
                NumPadItem(NumPadItemType.DELETE, 0, "DEL")
        ), this)

        numPadItems.layoutManager = GridLayoutManager(this, 3)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_set_pin, menu)
        LayoutHelper.tintMenuIcons(menu, ContextCompat.getColor(this, R.color.yellow_crypto))

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_done -> {
            viewModel.delegate.onClickDone()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onItemClick(item: NumPadItem) {
        when (item.type) {
            NumPadItemType.NUMBER -> {
                viewModel.delegate.onEnterDigit(item.number)
            }
            NumPadItemType.DELETE -> {
                viewModel.delegate.onClickDelete()
            }
        }
    }

    private fun updatePinCircles(length: Int) {
        val filledCircle = R.drawable.pin_circle_filled
        val emptyCircle = R.drawable.pin_circle_empty

        imgPinMask1.setImageResource(if (length > 0) filledCircle else emptyCircle)
        imgPinMask2.setImageResource(if (length > 1) filledCircle else emptyCircle)
        imgPinMask3.setImageResource(if (length > 2) filledCircle else emptyCircle)
        imgPinMask4.setImageResource(if (length > 3) filledCircle else emptyCircle)
        imgPinMask5.setImageResource(if (length > 4) filledCircle else emptyCircle)
        imgPinMask6.setImageResource(if (length > 5) filledCircle else emptyCircle)
    }

    companion object {

        private const val keyInteractionType = "interaction_type"
        private const val keyEnteredPin = "entered_pin"

        fun start(context: Context, interactionType: PinInteractionType, enteredPin: String = "") {
            val intent = Intent(context, PinActivity::class.java)
            intent.putExtra(keyInteractionType, interactionType)
            intent.putExtra(keyEnteredPin, enteredPin)
            context.startActivity(intent)
        }
    }

}

enum class NumPadItemType {
    NUMBER, DELETE, FINGER
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
            holder.bind(numPadItems[position]) { listener.onItemClick(numPadItems[position]) }
        }
    }
}

class NumPadItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var txtNumber: TextView = itemView.findViewById(R.id.txtNumPadNumber)
    private var txtLetters: TextView = itemView.findViewById(R.id.txtNumPadText)
    private var imgBackSpace: ImageView = itemView.findViewById(R.id.imgBackSpace)
    private var imgFingerprint: ImageView = itemView.findViewById(R.id.imgFingerprint)


    fun bind(item: NumPadItem, onClick: () -> (Unit)) {

        itemView.setOnClickListener { onClick.invoke() }

        txtNumber.visibility = View.GONE
        txtLetters.visibility = View.GONE
        imgBackSpace.visibility = View.GONE
        imgFingerprint.visibility = View.GONE
        itemView.background = null

        when (item.type) {
            NumPadItemType.DELETE -> {
                imgBackSpace.visibility = View.VISIBLE
            }

            NumPadItemType.NUMBER -> {
                txtNumber.visibility = View.VISIBLE
                txtLetters.visibility = if (item.number == 0) View.GONE else View.VISIBLE
                txtNumber.text = item.number.toString()
                txtLetters.text = item.letters
                itemView.setBackgroundResource(R.drawable.numpad_button_background)
            }
        }
    }
}

