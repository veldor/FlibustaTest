package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.selection.RequestItem
import net.veldor.tor_client.model.connection.WebResponse
import java.util.*

class ConnectionErrorDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(requireContext(), R.style.dialogTheme)
                .setTitle(getString(R.string.connection_error_message))
                .setMessage(
                    String.format(
                        Locale.ENGLISH,
                        "Error when connecting: %s.",
                        response?.errorText
                    )
                )
                .setPositiveButton(getString(R.string.reload_title)) { _, _ ->
                    callback?.let { it1 -> it1() }
                }
                .setNeutralButton(R.string.setup_bridges_title) { _, _ ->
                    dialog?.dismiss()
                    val dialog = TorBridgesSetupDialog()
                    TorBridgesSetupDialog.callback = callback
                    dialog.showNow(
                        requireActivity().supportFragmentManager,
                        TorBridgesSetupDialog.TAG
                    )

                }
                .setNegativeButton(R.string.close_message, null)
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        var request: RequestItem? = null
        var response: WebResponse? = null
        var callback: (() -> Unit)? = null
        const val TAG = "add blacklist item dialog"
    }
}