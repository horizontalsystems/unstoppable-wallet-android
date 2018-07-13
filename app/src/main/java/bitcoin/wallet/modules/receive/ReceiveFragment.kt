package bitcoin.wallet.modules.receive

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.viewHelpers.LayoutHelper
import bitcoin.wallet.viewHelpers.TextHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder

class ReceiveFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var viewModel: ReceiveViewModel

    private lateinit var coin: Coin
    private lateinit var address: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)
        viewModel.init()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        val rootView = View.inflate(context, R.layout.fragment_bottom_sheet_receive, null) as ViewGroup

        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        rootView.findViewById<TextView>(R.id.txtTitle)?.let { txtTitle ->
            txtTitle.text = getString(R.string.receive_bottom_sheet_title, coin.code)
        }

        rootView.findViewById<TextView>(R.id.txtAddress)?.let { txtAddress ->
            txtAddress.text = address
        }

        rootView.findViewById<TextView>(R.id.txtCopy)?.let { txtCopy ->
            txtCopy.setOnClickListener {
                context?.let { context ->
                    TextHelper.copyTextToClipboard(context, address)
                    txtCopy.setTextColor(resources.getColor(R.color.black, null))
                    txtCopy.setText(R.string.receive_bottom_sheet_copied)
                }
            }
        }

        rootView.findViewById<ImageView>(R.id.imgQrCode)?.setImageBitmap(getQrCodeBitmapFromAddress(address))

        rootView.findViewById<Button>(R.id.btnCancel)?.let { btnCancel ->
            btnCancel.setOnClickListener { dismiss() }
        }

        rootView.findViewById<Button>(R.id.btnShare)?.let { btnShare ->
            btnShare.setOnClickListener {
                context?.let { context ->
                    TextHelper.shareExternalText(context, address, getString(R.string.receive_bottom_sheet_share_to))
                }
            }
        }

        return mDialog as Dialog
    }

    private fun getQrCodeBitmapFromAddress(address: String): Bitmap? {
        val multiFormatWriter = MultiFormatWriter()
        return try {
            val imageSize = LayoutHelper.dp(150F, context)
            val bitMatrix = multiFormatWriter.encode(address, BarcodeFormat.QR_CODE, imageSize, imageSize, hashMapOf(EncodeHintType.MARGIN to 0))
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        fun show(activity: FragmentActivity, coin: Coin, address: String) {
            val fragment = ReceiveFragment()
            fragment.coin = coin
            fragment.address = address
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }
    }

}
