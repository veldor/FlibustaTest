package net.veldor.flibusta_test.model.dialog

import android.content.*
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R


class DonationDialog {
    class Builder(private val mContext: Context) {
        fun build(): AlertDialog {
            val donateOptions = arrayOf("BitCoin", "Юmoney", "DonationAlerts")
            // покажу диалог с выбором способа доната
            val builder = AlertDialog.Builder(mContext)
            builder
                .setItems(
                    donateOptions
                ) { _: DialogInterface?, which: Int ->
                    if (which == 0) {
                        val walletNumber = "38Q6K5g4Bcz1YkCscXRjcuZpW4tQn4vuhU"
                        val clipboard =
                            App.instance.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied Text", walletNumber)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            mContext,
                            mContext.getString(R.string.wallet_copied_message),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (which == 1) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://yoomoney.ru/to/41001269882689")
                        mContext.startActivity(intent)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://www.donationalerts.com/r/somedevf33434")
                        mContext.startActivity(intent)
                    }
                }
                .setTitle("Поддержка разработки")
            return builder.create()
        }
    }
}