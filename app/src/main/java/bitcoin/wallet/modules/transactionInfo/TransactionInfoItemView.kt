package bitcoin.wallet.modules.transactionInfo

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.widget.TextView
import bitcoin.wallet.R

class TransactionInfoItemView : ConstraintLayout {

    private var attrTitle: String? = null
    private var attrValue: String? = null

    private lateinit var titleTextView: TextView
    private lateinit var valueTextView: TextView

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
        } finally {
            ta.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        titleTextView = findViewById(R.id.txtTitle)
        valueTextView = findViewById(R.id.txtValue)

        titleTextView.text = attrTitle
        valueTextView.text = attrValue
    }

}