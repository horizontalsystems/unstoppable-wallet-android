package bitcoin.wallet.modules.transactionInfo

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.core.App

class TransactionInfoItemView : ConstraintLayout {

    private var attrTitle: String? = null
    private var attrValue: String? = null
    private var attrShowValueBackground: String? = null
    private var attrValueIcon: String? = null

    private lateinit var titleTextView: TextView
    private lateinit var valueTitleTextView: TextView
    private lateinit var valueSubtitleTextView: TextView
    private lateinit var iconImageView: ImageView
    private lateinit var valueLinearLayout: LinearLayout
    private lateinit var progressBarView: ProgressBar


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

    override fun onFinishInflate() {
        super.onFinishInflate()

        titleTextView = findViewById(R.id.txtTitle)
        valueTitleTextView = findViewById(R.id.txtValueTitle)
        valueSubtitleTextView = findViewById(R.id.txtValueSubtitle)
        iconImageView = findViewById(R.id.valueLeftIcon)
        valueLinearLayout = findViewById(R.id.valueWrapper)
        progressBarView = findViewById(R.id.progressBar)

        titleTextView.text = attrTitle
        valueTitleTextView.text = attrValue
    }

    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_transaction_info_item, this)
    }

    fun bind(title: String? = null, valueTitle: String? = null, valueSubtitle: String? = null, valueIcon: Int? = null, progressValue: Int? = null) {
        titleTextView.text = title
        valueTitleTextView.text = valueTitle
        valueSubtitleTextView.text = valueSubtitle
        valueIcon?.let {
            iconImageView.setImageDrawable(ContextCompat.getDrawable(App.instance, it))
            iconImageView.visibility = View.VISIBLE
        } ?: run {
            iconImageView.visibility = View.GONE
        }

        progressBarView.progress = progressValue ?: 0
        progressBarView.visibility = if (progressValue == null) View.GONE else View.VISIBLE
        valueSubtitleTextView.visibility = if (valueSubtitle == null) View.GONE else View.VISIBLE

        if (valueIcon == null || progressValue != null) {
            valueLinearLayout.background = null
        }

        invalidate()
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

}
