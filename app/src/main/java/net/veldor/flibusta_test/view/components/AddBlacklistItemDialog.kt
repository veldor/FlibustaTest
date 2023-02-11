package net.veldor.flibusta_test.view.components

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.AddBlacklistItemDialogViewBinding
import net.veldor.flibusta_test.model.selection.filter.*
import java.util.*

class AddBlacklistItemDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val binding = AddBlacklistItemDialogViewBinding.inflate(layoutInflater)
            binding.filterText.setText(value)
            when(type){
                BlacklistItem.TYPE_BOOK ->{
                    binding.typeBookName.isChecked = true
                }
                BlacklistItem.TYPE_AUTHOR ->{
                    binding.typeAuthorName.isChecked = true
                }
                BlacklistItem.TYPE_GENRE ->{
                    binding.typeGenre.isChecked = true
                }
                BlacklistItem.TYPE_SEQUENCE ->{
                    binding.typeSequence.isChecked = true
                }
                BlacklistItem.TYPE_FORMAT ->{
                    binding.typeFormat.isChecked = true
                }
                else ->{
                    binding.typeBookName.isChecked = true
                }
            }
            binding.strictFilter.isChecked = true
            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            builder.setTitle(R.string.add_to_blacklist_title)
            builder.setView(binding.root)
            builder.setPositiveButton(R.string.add_title){_,_->
                var valueForInsert = binding.filterText.text.toString().trim()
                if(valueForInsert.isNotEmpty()){
                    val blacklistItem = when(binding.blacklistTypeGroup.checkedRadioButtonId){
                        R.id.typeBookName -> {
                            BlacklistBooks
                        }
                        R.id.typeAuthorName -> {
                            BlacklistAuthors
                        }
                        R.id.typeGenre -> {
                            BlacklistGenre
                        }
                        R.id.typeSequence -> {
                            BlacklistSequences
                        }
                        else -> {
                            BlacklistFormat
                        }
                    }
                    if(binding.blacklistStrictGroup.checkedRadioButtonId == R.id.softFilter){
                        valueForInsert = "*$valueForInsert"
                    }
                    blacklistItem.addValue(valueForInsert)
                    Toast.makeText(requireContext(), String.format(Locale.ENGLISH, "%s inserted to blacklist", valueForInsert), Toast.LENGTH_SHORT).show()
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
        const val TAG = "add blacklist item dialog"
    }
}