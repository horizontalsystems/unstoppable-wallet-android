package cash.p.terminal.core.usecase

import com.m2049r.xmrwallet.util.RestoreHeight
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat

class ValidateMoneroHeightUseCase {

    private val parser = SimpleDateFormat("yyyy-MM-dd").apply {
        isLenient = false
    }

    operator fun invoke(heightStr: String): Long {
        var height: Long = -1

        val restoreHeight = heightStr.trim { it <= ' ' }
        if (restoreHeight.isEmpty()) return -1
        try {
            height = RestoreHeight.getInstance().getHeight(parser.parse(restoreHeight))
        } catch (_: ParseException) {
        }
        if ((height < 0) && (restoreHeight.length == 8)) try {
            // is it a date without dashes?
            height = RestoreHeight.getInstance().getHeight(parser.parse(restoreHeight))
        } catch (_: ParseException) {
        }
        if (height < 0) try {
            // or is it a height?
            height = restoreHeight.toLong()
        } catch (ex: NumberFormatException) {
            return -1
        }
        Timber.d("Using Restore Height = %d", height)
        return height
    }
}