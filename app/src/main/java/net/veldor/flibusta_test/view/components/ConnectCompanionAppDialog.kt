package net.veldor.flibusta_test.view.components

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.zxing.integration.android.IntentIntegrator
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.databinding.DialogConnectCompanionAppBinding
import net.veldor.flibusta_test.model.connection.CompanionAppSocketClient
import net.veldor.flibusta_test.model.handler.PreferencesHandler
import net.veldor.flibusta_test.model.listener.SocketClientListener
import net.veldor.flibusta_test.view.QrScanActivity
import java.net.URI

class ConnectCompanionAppDialog(val callback: (String) -> Unit?) : DialogFragment(), SocketClientListener {

    private var socketAddress: URI? = null
    private lateinit var binding: DialogConnectCompanionAppBinding
    private var qrRead =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
                // looks like result for login fragment qr read, notify it
                val scannedValue = data.contents
                val preferencesArray = scannedValue.split(":")
                if(preferencesArray.size == 2){
                    val ip = preferencesArray[0]
                    val port = preferencesArray[1]
                    binding.ipView.setText(ip)
                    binding.portView.setText(port)
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            binding = DialogConnectCompanionAppBinding.inflate(layoutInflater)
            binding.readQrBtn.setOnClickListener {
                val ii = IntentIntegrator(requireActivity())
                ii.captureActivity = QrScanActivity::class.java
                ii.setPrompt(requireContext().getString(R.string.scan_qr_message))
                ii.setBeepEnabled(false)
                ii.setOrientationLocked(true)
                val intent = ii.createScanIntent()
                qrRead.launch(intent)
            }

            binding.testConnectionBtn.setOnClickListener {
                val ip = binding.ipView.text.toString()
                val port = binding.portView.text.toString()
                if(ip.isNotEmpty() && port.isNotEmpty()){
                    checkConnection(ip, port)
                }
            }

            val builder = AlertDialog.Builder(it, R.style.dialogTheme)
            builder.setTitle(R.string.connect_companion_app_title)
            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun checkConnection(ip: String, port: String) {
        socketAddress = URI("ws://$ip:$port/")
        val client = CompanionAppSocketClient(socketAddress!!)
        client.listener = this
        client.establishConnection()
    }

    companion object {
        const val TAG = "connect companion dialog"
    }

    override fun connectionEstablished() {
        activity?.runOnUiThread {
            PreferencesHandler.companionAppCoordinates = socketAddress.toString()
            Toast.makeText(requireContext(), getString(R.string.connection_established_title), Toast.LENGTH_SHORT).show()
            dialog?.dismiss()
            callback(socketAddress.toString())
        }
    }

    override fun connectionClosed() {

    }

    override fun messageReceived(message: String?) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun connectionError(reason: String?) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), "Connection error! $reason", Toast.LENGTH_SHORT).show()
        }
    }
}