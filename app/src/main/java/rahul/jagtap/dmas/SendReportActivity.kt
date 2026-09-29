package rahul.jagtap.dmas.admin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.rosuh.filepicker.config.FilePickerManager
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.BaseActivity
import rahul.jagtap.dmas.R
import rahul.jagtap.dmas.databinding.ActivitySendReportBinding
import rahul.jagtap.dmas.extensions.*
import rahul.jagtap.dmas.model.User
import rahul.jagtap.dmas.utils.Utils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import androidx.core.widget.doAfterTextChanged


class SendReportActivity : BaseActivity() {
    private var selectedUser: User? = null
    private var reportFileName: String = ""
    private var reportDownloadUrl: String = ""
    private var selectedType: String = ""

    //    private val TAG = SendReportActivity::class.java.simpleName
    var reportUri: Uri? = null
    var userList = ArrayList<User>()
    lateinit var binding: ActivitySendReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendReportBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)
        // input-field migration: clear errors on edit
        listOf(binding.tilReport, binding.tilType, binding.tilUserEmail)
            .forEach { til -> til.editText?.doAfterTextChanged { til.error = null } }
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = getString(R.string.txt_send_report)
        //        email = app?.preferences?.loggedInUser?.email
        //        uid = app?.preferences?.loggedInUser?.uid
        //        username = app?.preferences?.loggedInUser?.username
        //        name = app?.preferences?.loggedInUser?.name

        binding.btnSubmit?.setOnClickListener {
            val strType = binding.etType.text.toString()
            //            val strUserEmail = etUserEmail.text.toString()
            if (reportUri == null) {
                binding.tilReport?.error = "Select Report"
                binding.etReport?.requestFocus()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(strType)) {
                binding.tilType?.error = "Select Type"
                binding.etType?.requestFocus()
                return@setOnClickListener
            }
            //            if (TextUtils.isEmpty(strUserEmail)) {
            //                etUserEmail?.error = "Select email/name"
            //                etUserEmail?.requestFocus()
            //                return@setOnClickListener
            //            }
            uploadReport(reportUri!!)
        }
        binding.etReport?.setOnClickListener {
            //            choosePhotoWithPermissions()
            openFile()
        }
        binding.etType?.setOnClickListener {
            val list = java.util.ArrayList<String>()
            list.add("Accounting Services")
            list.add("E-Suvidha")
            MaterialDialog.Builder(mContext!!).items(list).itemsCallback { dialog: MaterialDialog?, itemView: View?, position: Int, text: CharSequence ->
                run {
                    dialog?.dismiss()
                    binding.etType.setText(list[position])
                    selectedType = if (position == 0) "bills"
                    else "esuvidha"
                }
            }.show()
        }
        binding.etUserEmail?.setOnClickListener {
            // SelectUserActivity loads the user list itself from Firebase, so we must NOT pass it via the
            // Intent — a large user base makes that JSON exceed the Binder limit (TransactionTooLargeException).
            startActivityForResult(Intent(mContext, SelectUserActivity::class.java), SELECT_USER_INTENT)
        }
        getUserList()
    }

    private fun getUserList() {
        database.child(Utils.USERS_TABLE).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("firebase", "Got value ${snapshot.value}")
                // Get user value
                val json = Gson().toJson(snapshot.value)
                val type: Type = object : TypeToken<HashMap<String, User>?>() {}.type
                val map: HashMap<String, User> = Gson().fromJson(json, type)
                userList.addAll(map.values.toMutableList())
                //                setupSpinner()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    //    private fun setupSpinner() {
    //        if (userList != null && userList.size > 0) {
    //            val list = java.util.ArrayList<String>()
    //            for (user in userList) {
    //                list.add("${user.email}" + "\n" + "(${user.name})${user.contactNo}")
    //            }
    //            etUserEmail?.item = list
    //            etUserEmail?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    //                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
    //                    toast(list[p2])
    //                    selectedUser = userList[p2]
    //                }
    //
    //                override fun onNothingSelected(p0: AdapterView<*>?) {
    //                }
    //            }
    //            //                MaterialDialog.Builder(mContext!!).items(list).itemsCallback { dialog: MaterialDialog?, itemView: View?, position: Int, text: CharSequence ->
    //            //                    run {
    //            //                        dialog?.dismiss()
    //            //                        etUserEmail.setText(list[position])
    //            //                        selectedUser = userList[position]
    //            //                    }
    //            //                }.show()
    //        } else {
    //            //            toast("No users found.")
    //        }
    //    }

    fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            //            type = "application/pdf,application/xls,application/csv,image/*,video/*"
            type = "*/*"
            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            //            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            //            }
        }

        startActivityForResult(intent, PICK_PDF_FILE)
    }

    @SuppressLint("CheckResult")
    private fun choosePhotoWithPermissions() {
//        rxPermissions?.requestEachCombined(Manifest.permission.READ_EXTERNAL_STORAGE)?.subscribe {
//            when {
//                it.granted -> {
//                    // get file
//                    doBrowseFile()
//                }
//                it.shouldShowRequestPermissionRationale -> {
//                    // At least one denied permission without ask never again
//                }
//                else -> {
//                    // At least one denied permission with ask never again
//                    // Need to go to the settings
//                }
//            }
//        }
    }

    private fun doBrowseFile() {
        FilePickerManager.from(this).enableSingleChoice().forResult(FilePickerManager.REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FilePickerManager.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val list = FilePickerManager.obtainData()
                //                val filePath: String? = data.getStringExtra(RESULT_FILE_PATH)
                if (list != null && list.size > 0) {
                    Log.e("filepath is", "Uri: $list")
                    //                    selectedFilePath = list[0]
                    binding.etReport.setText(list[0])
                    reportUri = Uri.fromFile(File(list[0]))
                } else {
                    toast("failed to choose file")
                }
            }
        }
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            data?.data?.also { uri ->
                // Perform operations on the document using its URI.
                Log.e("uti", uri.toString())
                val path = mContext?.let { createCopyAndReturnRealPath(it, uri) }
                Log.e("path", path.toString())
                binding.etReport.setText(uri.toString())
                reportUri = uri
            }
        }
        if (requestCode == SELECT_USER_INTENT && resultCode == Activity.RESULT_OK) {
            val returnString: String? = data?.getStringExtra("selectedUser")
            selectedUser = Gson().fromJson<User>(returnString, User::class.java)
            binding.etUserEmail?.setText(selectedUser?.name)
        }
    }

    private fun uploadReport(fileUri: Uri) {
        val cpd: ProgressDialog? = ProgressDialog(mContext)
        cpd?.setCancelable(false)
        cpd?.show()
        reportFileName = "report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}"
        val filepath = storageRef.child(Utils.REPORTS_TABLE).child(selectedType).child(selectedUser?.email!!).child(reportFileName)
        filepath.putFile(fileUri).addOnSuccessListener {
            try {
                if (cpd != null && cpd.isShowing) cpd.dismiss()
                filepath.downloadUrl.addOnSuccessListener { uri: Uri ->
                    reportDownloadUrl = uri.toString()
                    createDbRecord()
                }.addOnFailureListener {
                    it.printStackTrace()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }.addOnFailureListener {
            if (cpd?.isShowing == true) cpd.dismiss()
            it.message?.let { it1 -> toast(it1) }
        }.addOnProgressListener {
            //displaying the upload progress
            val progress: Double = 100.0 * it.bytesTransferred / it.totalByteCount
            cpd?.setMessage("Please wait.. ")
        }
    }

    private fun createDbRecord() {
        val createdDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(Date())
        val billMap = HashMap<String, Any?>()
        billMap["createdBy"] = "admin"
        billMap["createdDateTime"] = createdDateTime
        billMap["reportToUser"] = selectedUser.toString()
        billMap["reportFileName"] = reportFileName
        billMap["reportDownloadUrl"] = reportDownloadUrl
        billMap["reportType"] = selectedType
        billMap["email"] = selectedUser?.email
        billMap["uid"] = selectedUser?.uid
        billMap["timeStamp"] = System.currentTimeMillis()

        val pushKey = database.child(Utils.REPORTS_TABLE).child(selectedType).child(selectedUser?.uid!!).child(getTodayDate()).push().key
        billMap["pushKey"] = pushKey

        val messageUserMap = HashMap<String, Any?>()
        messageUserMap["${Utils.REPORTS_TABLE}/$selectedType/${selectedUser?.uid!!}/${getTodayDate()}/$pushKey"] = billMap
        database.updateChildren(messageUserMap) { databaseError: DatabaseError?, databaseReference: DatabaseReference? ->
            if (databaseError != null) {
                Log.e("db error", databaseError.message)
            }
        }

        val backupMap = HashMap<String, Any?>()
        backupMap["${Utils.REPORTS_TABLE}_backup/$selectedType/${selectedUser?.uid!!}/${getTodayDate()}/$pushKey"] = billMap
        database.updateChildren(backupMap)
        toast("Report sent successfully")
        finish()
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("dd-MM-yy", Locale.ENGLISH).format(Date())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                Utils.hideSoftKeyboard(this)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Nullable
    fun createCopyAndReturnRealPath(@NonNull context: Context, @NonNull uri: Uri?): String? {
        val contentResolver = context.contentResolver ?: return null

        // Create file path inside app's data dir
        val filePath = (context.applicationInfo.dataDir + File.separator + System.currentTimeMillis())
        val file = File(filePath)
        try {
            val inputStream = contentResolver.openInputStream(uri!!) ?: return null
            val outputStream: OutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
            outputStream.close()
            inputStream.close()
        } catch (ignore: IOException) {
            return null
        }
        return file.absolutePath
    }

    companion object {
        // Request code for selecting a PDF document.
        const val PICK_PDF_FILE = 2
        const val SELECT_USER_INTENT = 3
    }
}
