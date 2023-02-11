package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.AddBlacklistItemDialogViewBinding
import net.veldor.flibusta_test.databinding.DialogLoginBinding
import net.veldor.flibusta_test.model.selection.filter.*
import java.util.*

class LoginDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val binding = DialogLoginBinding.inflate(layoutInflater)
            binding.loginButton.setOnClickListener {
                val login = binding.loginView.text
                val password = binding.passwordView.text
                if(login.isNullOrEmpty()){
                    binding.loginView.requestFocus()
                    Toast.makeText(requireContext(), getString(R.string.enter_login_title), Toast.LENGTH_SHORT).show()
                }
                else if(password.isNullOrEmpty()){
                    binding.passwordView.requestFocus()
                    Toast.makeText(requireContext(), getString(R.string.enter_login_title), Toast.LENGTH_SHORT).show()
                }
                else{
                    callback?.let { it1 -> it1(login.toString(), password.toString()) }
                    dialog?.dismiss()
                }
            }
            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            builder.setTitle(R.string.login_button_title)
            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        var callback: ((login: String, password: String) -> Unit)? = null
        const val TAG = "login dialog"
    }
}