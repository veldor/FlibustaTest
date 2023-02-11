package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.AddSubscribeItemDialogViewBinding
import net.veldor.flibusta_test.model.selection.subscribe.*
import java.util.*

class AddSubscribeItemDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val binding = AddSubscribeItemDialogViewBinding.inflate(layoutInflater)
            binding.subscribeText.setText(value)
            when(type){
                SubscribeItem.TYPE_BOOK ->{
                    binding.typeBookName.isChecked = true
                }
                SubscribeItem.TYPE_AUTHOR ->{
                    binding.typeAuthorName.isChecked = true
                }
                SubscribeItem.TYPE_GENRE ->{
                    binding.typeGenre.isChecked = true
                }
                SubscribeItem.TYPE_SEQUENCE ->{
                    binding.typeSequence.isChecked = true
                }
            }
            binding.strictSubscribe.isChecked = true
            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            builder.setTitle(R.string.add_to_subscribe_title)
            builder.setView(binding.root)
            builder.setPositiveButton(R.string.add_title){_,_->
                var valueForInsert = binding.subscribeText.text.toString().trim()
                if(valueForInsert.isNotEmpty()){
                    val subscribeItem: SubscribeType = when(binding.subscribeTypeGroup.checkedRadioButtonId){
                        R.id.typeBookName -> {
                            SubscribeBooks
                        }
                        R.id.typeAuthorName -> {
                            SubscribeAuthors
                        }
                        R.id.typeGenre -> {
                            SubscribeGenre
                        }
                        else -> {
                            SubscribeSequences
                        }
                    }
                    if(binding.subscribeStrictGroup.checkedRadioButtonId == R.id.softSubscribe){
                        valueForInsert = "*$valueForInsert"
                    }
                    subscribeItem.addValue(valueForInsert)
                    Toast.makeText(requireContext(), String.format(Locale.ENGLISH, "%s inserted to subscribes", valueForInsert), Toast.LENGTH_SHORT).show()
                    callback?.let { action -> action() }
                }
            }
            builder.setNegativeButton(R.string.cancel, null)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        var callback: (() -> Unit)? = null
        var value: String? = null
        var type: String? = null
        const val TAG = "add subscribe item dialog"
    }
}