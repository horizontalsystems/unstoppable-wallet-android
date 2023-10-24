package io.horizontalsystems.bankwallet.modules.hardwarewallet

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.journeyapps.barcodescanner.CompoundBarcodeView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.managers.toSignature
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList
import kotlinx.coroutines.rx2.await
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun HardwareWalletSignFragment(ownAddress: String,
    sendViewModel: SendEvmTransactionViewModel? = null, feeModel: EvmFeeCellViewModel? = null,
                              /* txUnsignedHex: Single<String>? = null,*/ signMessageViewModel: WCSignMessageRequestViewModel? = null) {

    val logger = AppLogger("sign-evm-hardware")

    val txUnsignedHex = sendViewModel?.service?.getUnsignedTransactionHex()

    var isSending by remember { mutableStateOf<Boolean>(false) }

    var scanToTransmit by remember { mutableStateOf<Boolean>(false) }
    var txData by remember { mutableStateOf<String?>(null) }
    var messageHex by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(signMessageViewModel) {
        if (signMessageViewModel != null) {
            messageHex = signMessageViewModel?.message?.data?.toByteArray().toHexString()
        } else {
            messageHex = null
        }
    }

    LaunchedEffect(txUnsignedHex) {
        try {
            txData = txUnsignedHex?.await()
            feeModel?.pauseSync()
            sendViewModel?.pauseSync()
        } catch (e: Throwable) {
            // TODO: add error handling
        }
    }

    // context
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var nfcAdapter : NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    var pendingIntent: PendingIntent? = PendingIntent.getActivity(
        context, 0,
        Intent(context, context.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_MUTABLE
    )

    DisposableEffect(context) {

        val intentListener = Consumer<Intent> {
            if (ownAddress != null) {
                if (handleNfcIntent(it, ownAddress!!, txData, messageHex)) {
                    scanToTransmit = true
                } else {
                    sendViewModel?.service?.setFailed(IOException("NFC Connection Error"))
                    signMessageViewModel?.showSignError = true
                }
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                nfcAdapter?.enableForegroundDispatch(context, pendingIntent, null, null)
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                nfcAdapter?.disableForegroundDispatch(context)
            }
        }
        activity.addOnNewIntentListener(intentListener)
        context.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            activity.removeOnNewIntentListener(intentListener)
            context.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    // we need this stupid shit for animated vector to work
    var atEnd: Boolean by remember { mutableStateOf(false) }
    val painter = rememberAnimatedVectorPainter(
        AnimatedImageVector.animatedVectorResource(id = R.drawable.icon_nfc_animated),
        atEnd
    )

    if (!isSending) {
        Box(
            modifier = Modifier
                //.defaultMinSize(minHeight = 384.dp)
                //.border(width = 2.dp, color = Color.Green)
                .fillMaxHeight()
                .fillMaxSize(), // This fills the parent's size.
            contentAlignment = Alignment.Center // This will center the child composable.
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally // Centers the items horizontally within the Column.
            ) {

                LaunchedEffect(painter) {
                    atEnd = !atEnd
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (scanToTransmit) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .aspectRatio(1.6f),
                        contentAlignment = Alignment.Center,
                    ) {
                        ScannerView(onScan = {

                            if (it.startsWith("https://app.hito.dev/eth/tx/#!")) {
                                val txHex = it.removePrefix("https://app.hito.dev/eth/tx/#!")
                                val signature = decodeRawTransactionSignature(txHex)
                                val signatureHex = signature?.toHex()
                                isSending = true
                                sendViewModel?.service?.send(logger, signatureHex)
                            } else if(it.startsWith("evm.sig:")) {
                                val signatureHex = it.removePrefix("evm.sig:")
                                isSending = true
                                if (messageHex == null) {
                                    sendViewModel?.service?.send(logger, signatureHex)
                                } else {
                                    val signature = signatureHex.toSignature()
                                    val shex = signature?.toByteArray().toHexString()
                                    signMessageViewModel?.acceptWithSignature(shex!!)
                                }
                            } else {
                                sendViewModel?.service?.setFailed(IOException("Signature Scan Error"))
                                signMessageViewModel?.showSignError = true
                            }
                        })
                    }
                    Text(
                        "Scan to Transmit",
                        modifier = Modifier.padding(top = 16.dp),
                        color = ComposeAppTheme.colors.jacob,
                        fontSize = 18.sp
                    )

                } else {
                    Icon(
                        modifier = Modifier.size(96.dp),//.background(Color.Gray),
                        painter = painter,
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.jacob,
                    )
                    Text(
                        text = "Confirm by tapping Hito Wallet",
                        modifier = Modifier.padding(top = 16.dp),
                        color = ComposeAppTheme.colors.jacob,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }

}

fun handleNfcIntent(intent: Intent?, ownAddress: String, txUnsignedHex: String?, messageHex: String?) : Boolean {

    if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent?.action) {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

        if (rawMessages != null && rawMessages!!.size == 1) {
            val message = rawMessages[0] as NdefMessage
            if (message.records.size == 1) {

                var record = message.records[0]
                val payload = record.payload

                // Get the rest of the URI
                val fullUriBytes = ByteArray(payload.size - 1)
                System.arraycopy(payload, 1, fullUriBytes, 0, fullUriBytes.size)
                val uriBody = String(fullUriBytes, Charsets.UTF_8)
                if (uriBody.startsWith("app.hito.dev/#eth/send")) {
                    val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                    if (tag != null) {
                        val messageText = if (messageHex != null) {
                            "evm.msg:$ownAddress:$messageHex"
                        } else {
                            "evm.sign:$ownAddress:$txUnsignedHex"
                        }

                        writeTextToTag(tag, messageText)
                        return true
                    } else {
                        // TODO: add error handling
                    }
                }
            }
        }
        // TODO: add error handling
    }
    return false
}

private fun writeTextToTag(tag: Tag, text: String): Boolean {
    val textRecord = createTextRecord(text, Locale.ENGLISH, true)
    val ndefMessage = NdefMessage(arrayOf(textRecord))

    return writeTag(tag, ndefMessage)
}

private fun writeTag(tag: Tag, ndefMessage: NdefMessage): Boolean {
    val size = ndefMessage.toByteArray().size
    try {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            if (!ndef.isWritable) {
                return false
            }
            if (ndef.maxSize < size) {
                return false
            }
            ndef.writeNdefMessage(ndefMessage)
            return true
        }
    } catch (e: Exception) {
        // Handle error
    }
    return false
}

fun createTextRecord(text: String, locale: Locale = Locale("en", "US"), encodeInUtf8: Boolean = false): NdefRecord {
    val langBytes = locale.language.toByteArray(Charset.forName("US-ASCII"))

    val utfEncoding: Charset = if (encodeInUtf8) {
        Charset.forName("UTF-8")
    } else {
        Charset.forName("UTF-16")
    }

    val textBytes = text.toByteArray(utfEncoding)
    val utfBit = if (encodeInUtf8) 0 else 1
    val status = (utfBit shl 7) or langBytes.size

    val data = ByteArray(1 + langBytes.size + textBytes.size)
    data[0] = status.toByte()
    System.arraycopy(langBytes, 0, data, 1, langBytes.size)
    System.arraycopy(textBytes, 0, data, 1 + langBytes.size, textBytes.size)

    return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), data)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ScannerView(onScan: (String) -> Unit) {
    val context = LocalContext.current
    val barcodeView = remember {
        CompoundBarcodeView(context).apply {
            this.initializeFromIntent((context as Activity).intent)
            this.setStatusText("")
            this.decodeSingle { result ->
                result.text?.let { barCodeOrQr ->
                    onScan.invoke(barCodeOrQr)
                }
            }
        }
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showPermissionNeededDialog by remember { mutableStateOf(cameraPermissionState.status != PermissionStatus.Granted) }

    if (showPermissionNeededDialog) {
        ButtonPrimaryYellow(title = "Request Camera Permission", onClick = {
            cameraPermissionState.launchPermissionRequest()
            showPermissionNeededDialog = false
        })
    } else {
        AndroidView(factory = { barcodeView })
    }

    DisposableLifecycleCallbacks(
        onResume = barcodeView::resume,
        onPause = barcodeView::pause
    )
}

fun Signature.toHex(): String {
    return "0x" + r.toByteArrayToHex() + s.toByteArrayToHex() + v.toHex()
}

fun Signature.toByteArray(): ByteArray {
    return r + s + v.toByte()
}

fun ByteArray.toByteArrayToHex(): String {
    return joinToString(separator = "") { byte -> "%02x".format(byte) }
}

fun Int.toHex(): String {
    return this.toString(16)
}

// TODO: move to EvmKitManager
fun decodeRawTransactionSignature(txhex: String): Signature? {
    val tx = txhex.removePrefix("0x").hexStringToByteArray()
    val rlp =
    if (tx[0] == 0x02.toByte()) {
        RLP.decode2(tx.copyOfRange(1, tx.size)).get(0) as RLPList
    } else {
        RLP.decode2(tx).get(0) as RLPList
    }
    if (rlp.size > 3) {
        return Signature(
            rlp.get(rlp.size - 3).toInt(),
            rlp.get(rlp.size - 2).toBigInteger().toByteArray(),
            rlp.get(rlp.size - 1).toBigInteger().toByteArray()
        )
    } else {
        return null
    }
}




