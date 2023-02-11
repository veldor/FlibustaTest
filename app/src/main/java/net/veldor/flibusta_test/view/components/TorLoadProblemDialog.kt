package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import net.veldor.flibusta_test.R

class TorLoadProblemDialog() : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            builder.setTitle(R.string.tor_load_error_title)
            builder.setMessage(getString(R.string.tor_load_error_body))
            builder.setPositiveButton(getString(R.string.try_again_message)){_,_->
                cb?.let { it1 -> it1(true) }
                dialog?.dismiss()
            }
            builder.setNegativeButton(getString(R.string.setup_bridges_title)){_,_->
                val dialog = TorBridgesSetupDialog()
                TorBridgesSetupDialog.callback = {
                    dialog.dismiss()
                    cb?.let { it1 -> it1(false) }
                }
                dialog.showNow(requireActivity().supportFragmentManager, TorBridgesSetupDialog.TAG)
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


    companion object {
        const val TAG = "tor load problem dialog"
        var cb: ((Boolean) -> Unit?)? = null
    }
}