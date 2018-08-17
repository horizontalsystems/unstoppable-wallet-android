package bitcoin.wallet.modules.transactionInfo

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.core.App

class TransactionInfoItemView : ConstraintLayout {

    private var attrTitle: String? = null
    private var attrValue: String? = null
    private var attrShowValueBackground: String? = null
    private var attrValueIcon: String? = null

    private lateinit var titleTextView: TextView
    private lateinit var valueTextView: TextView
    private lateinit var iconImageView: ImageView

    var title: String? = null
        set(value) {
            titleTextView.text = value
            invalidate()
        }

    var value: String? = null
        set(value) {
            valueTextView.text = value
            invalidate()
        }

    var showValueBackground: Boolean = false
        set(value) {
            valueTextView.background = if (value) ContextCompat.getDrawable(App.instance, R.drawable.text_grey_background) else null
            valueTextView.setTextColor(ContextCompat.getColor(App.instance, if (value) R.color.dark else R.color.grey))
            invalidate()
        }

    var valueIcon: Int? = null
        set(value) {
            value?.let {
                iconImageView.setImageDrawable(ContextCompat.getDrawable(App.instance, it))
                iconImageView.visibility = View.VISIBLE
            } ?: run {
                iconImageView.visibility = View.GONE
            }

            invalidate()
        }

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
        loadAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
        loadAttributes(attrs)
    }

    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_transaction_info_item, this)
    }

    private fun loadAttributes(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TransactionInfoItemView, 0, 0)
        try {
            attrTitle = ta.getString(R.styleable.TransactionInfoItemView_title)
            attrValue = ta.getString(R.styleable.TransactionInfoItemView_value)
            attrShowValueBackground = ta.getString(R.styleable.TransactionInfoItemView_showValueBackground)
            attrValueIcon = ta.getString(R.styleable.TransactionInfoItemView_valueIcon)
        } finally {
            ta.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        titleTextView = findViewById(R.id.txtTitle)
        valueTextView = findViewById(R.id.txtValue)
        iconImageView = findViewById(R.id.valueLeftIcon)

        titleTextView.text = attrTitle
        valueTextView.text = attrValue
    }

}