package net.veldor.flibusta_test.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import net.veldor.flibusta_test.App
import net.veldor.flibusta_test.R
import net.veldor.flibusta_test.model.adapter.FolderAdapter
import net.veldor.flibusta_test.model.selection.FilePojo
import java.io.File
import java.util.*


class SelectDirActivity : Activity() {
    //Folders and Files have separate lists because we show all folders first then files
    private var folderAndFileList: ArrayList<FilePojo>? = null
    private var foldersList: ArrayList<FilePojo>? = null
    private var filesList: ArrayList<FilePojo>? = null
    private var tvTitle: TextView? = null
    private var tvLocation: TextView? = null
    private var location: String = Environment.getExternalStorageDirectory().absolutePath
    private var pickFiles = false
    private var receivedIntent: Intent? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fp_main_layout)
        if (!permissionsGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    100
                )
            }
        }
        if (!isExternalStorageReadable) {
            Toast.makeText(this, "Storage access permission not given", Toast.LENGTH_LONG).show()
            finish()
        }
        tvTitle = findViewById<View>(R.id.fp_tv_title) as TextView
        tvLocation = findViewById<View>(R.id.fp_tv_location) as TextView
        try {
            receivedIntent = intent
            if (receivedIntent?.hasExtra("title") == true) {
                val receivedTitle = receivedIntent?.extras!!.getString("title")
                if (receivedTitle != null) {
                    tvTitle!!.text = receivedTitle
                }
            }
            if (receivedIntent?.hasExtra("location") == true) {
                val reqLocation = receivedIntent?.extras!!.getString("location")
                if (reqLocation != null) {
                    val requestedFolder = File(reqLocation)
                    if (requestedFolder.exists()) location = reqLocation
                }
            }
            if (receivedIntent?.hasExtra("pickFiles") == true) {
                pickFiles = receivedIntent?.extras!!.getBoolean("pickFiles")
                if (pickFiles) {
                    findViewById<View>(R.id.fp_btn_select).visibility = View.GONE
                    findViewById<View>(R.id.fp_btn_new).visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        loadLists(location)
    }

    /* Checks if external storage is available to at least read */
    private val isExternalStorageReadable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    private fun loadLists(location: String) {
        try {
            val folder = File(location)
            if (!folder.isDirectory) exit()
            tvLocation!!.text =
                String.format(Locale.ENGLISH, "Location is %s", folder.absolutePath)
            val files = folder.listFiles()
            foldersList = ArrayList<FilePojo>()
            filesList = ArrayList<FilePojo>()
            files?.forEach {
                if (it.isDirectory) {
                    val filePojo = FilePojo(it.name, true)
                    foldersList!!.add(filePojo)
                } else {
                    val filePojo = FilePojo(it.name, false)
                    filesList!!.add(filePojo)
                }
            }

            // sort & add to final List - as we show folders first add folders first to the final list
            Collections.sort(foldersList, comparatorAscending)
            folderAndFileList = ArrayList<FilePojo>()
            folderAndFileList!!.addAll(foldersList!!)

            //if we have to show files, then add files also to the final list
            if (pickFiles) {
                Collections.sort(filesList, comparatorAscending)
                folderAndFileList!!.addAll(filesList!!)
            }
            showList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } // load List

    var comparatorAscending: Comparator<FilePojo> =
        Comparator<FilePojo> { f1, f2 -> f1.name.compareTo(f2.name) }

    private fun showList() {
        try {
            val FolderAdapter = FolderAdapter(this, folderAndFileList)
            val listView = findViewById<View>(R.id.fp_listView) as ListView
            listView.adapter = FolderAdapter
            listView.onItemClickListener =
                AdapterView.OnItemClickListener { parent, view, position, id -> listClick(position) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listClick(position: Int) {
        if (pickFiles && !folderAndFileList!![position].isFolder) {
            val data = location + File.separator + folderAndFileList!![position].getName()
            receivedIntent!!.putExtra("data", data)
            setResult(RESULT_OK, receivedIntent)
            finish()
        } else {
            location = location + File.separator + folderAndFileList!![position].getName()
            loadLists(location)
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("goBack(null)"))
    override fun onBackPressed() {
        goBack(null)
    }

    fun goBack(v: View?) {
        if (location != "" && location != "/") {
            val start = location.lastIndexOf('/')
            val newLocation = location.substring(0, start)
            location = newLocation
            loadLists(location)
        } else {
            exit()
        }
    }

    private fun exit() {
        setResult(RESULT_CANCELED, receivedIntent)
        finish()
    }

    private fun createNewFolder(filename: String) {
        try {
            val file = File(location + File.separator + filename)
            Log.d("surprise", "createNewFolder 158:  create dir ${file.absolutePath}")
            file.mkdirs()
            loadLists(location)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error:$e", Toast.LENGTH_LONG)
                .show()
        }
    }

    fun newFolderDialog(v: View?) {
        val dialog = AlertDialog.Builder(this).create()
        dialog.setTitle(getString(R.string.enter_new_dir_name_title))
        val et = EditText(this)
        dialog.setView(et)
        dialog.setButton(
            DialogInterface.BUTTON_POSITIVE, getString(R.string.create_title)
        ) { _, _ -> createNewFolder(et.text.toString()) }
        dialog.setButton(
            DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel_title)
        ) { _, _ -> }
        dialog.show()
    }

    fun select(v: View) {
        if (pickFiles) {
            Toast.makeText(this, "You have to select a file", Toast.LENGTH_LONG).show()
        } else if (receivedIntent != null) {
            receivedIntent!!.putExtra("data", location)
            setResult(RESULT_OK, receivedIntent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun cancel(v: View?) {
        exit()
    }

    fun permissionsGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val writeResult =
                App.instance.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val readResult =
                App.instance.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val writeResult = PermissionChecker.checkSelfPermission(
                App.instance.applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val readResult =
                PermissionChecker.checkSelfPermission(
                    App.instance.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
} // class
