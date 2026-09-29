package rahul.jagtap.dmas.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.people.v1.PeopleService
import com.google.api.services.people.v1.model.EmailAddress
import com.google.api.services.people.v1.model.Name
import com.google.api.services.people.v1.model.Person
import com.google.api.services.people.v1.model.PhoneNumber
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import rahul.jagtap.dmas.BaseActivity
import rahul.jagtap.dmas.BuildConfig
import rahul.jagtap.dmas.R
import rahul.jagtap.dmas.adapter.UserAdapter
import rahul.jagtap.dmas.databinding.ActivityUsersBinding
import rahul.jagtap.dmas.extensions.addToCommaSeparatedString
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.User
import rahul.jagtap.dmas.utils.Utils
import rahul.jagtap.dmas.widget.MaterialSearchView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale


class UsersActivity : BaseActivity() {
    private var signInEmail: String? = ""
    private var resultFileUri: Uri? = null
    private lateinit var resultFile: File
    private var workbook: Workbook? = null
    private var userList: java.util.ArrayList<User>? = null
    var TAG = UsersActivity::class.java.canonicalName
    private var sheet: Sheet? = null
    private var cell: Cell? = null

    var adapter: UserAdapter? = null
    private var googleSignInClient: GoogleSignInClient? = null
    private val RC_SIGN_IN = 9001
    var peopleService: PeopleService? = null
    lateinit var binding: ActivityUsersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle?.text = "Users"

        userList = ArrayList() //        val userListString = intent.getStringExtra("userList")
        //        val type: Type = object : TypeToken<ArrayList<User>?>() {}.type
        //        userList = Gson().fromJson<ArrayList<User>>(userListString, type)
        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope(
                    "https://www.googleapis.com/auth/contacts"
                )
            ).build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        getUserList()
    }

    private fun signIn() {
        val signInIntent: Intent? = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java) ?: return

            val credential = GoogleAccountCredential.usingOAuth2(this, listOf("https://www.googleapis.com/auth/contacts"))
            credential.selectedAccount = account.account

            val httpRequestInitializer = HttpRequestInitializer { httpRequest: HttpRequest ->
                credential.initialize(httpRequest)
                // Increase the timeout settings
                httpRequest.connectTimeout = 600000 // 600 seconds connect timeout
                httpRequest.readTimeout = 600000    // 600 seconds read timeout
            }

            peopleService = PeopleService.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), httpRequestInitializer).setApplicationName(mContext?.getString(R.string.app_name)).build()
            if (!TextUtils.isEmpty(account.email)) {
                signInEmail = account.email
                account.email?.let { adapter?.setSavedByEmail(it) }
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            e.printStackTrace()
            adapter?.setGmailSignedIn(false)
        }
    }

    fun saveContactAvoidingDuplicates(accountName: String, phoneNumber: String, email: String?) {
        val existingContact = findExistingContact(phoneNumber)
        if (existingContact == null) {
            Log.e(TAG, "No duplicate found")
            // No duplicate found, proceed with creation
            saveContactToGoogle(accountName, phoneNumber, email)
        } else {
            Log.e(TAG, "duplicate found")
            // Duplicate found, you can update the existing contact or skip
            // Example: updateExistingContact(peopleService, existingContact, contact)
        }
    }

    fun findExistingContact(phoneNumber: String): Person? {
        var person: Person? = null
        Thread {
            val response = peopleService?.people()?.connections()
                ?.list("people/me")
                ?.setPageSize(200)
                ?.setPersonFields("names,emailAddresses,phoneNumbers")
                ?.execute()

            person = response?.connections?.firstOrNull { person ->
                person.phoneNumbers?.any { it.value == phoneNumber } == true
            }
        }.start()
//        launchCoroutine({
//
//        }, { coroutineContext, throwable ->
//            throwable.printStackTrace()
//        })
        return person
    }

    fun saveContactToGoogle(accountName: String, phoneNumber: String, email: String?) {
        val person = Person().apply {
            names = listOf(Name().apply {
                givenName = accountName
            })
            phoneNumbers = listOf(PhoneNumber().apply {
                value = phoneNumber
            })
            emailAddresses = listOf(EmailAddress().apply {
                value = email
            })
        }
        Thread {
            try {
                peopleService?.people()?.createContact(person)?.execute()
                Log.e(TAG, "saveContactToGoogle: success")
            } catch (e: Exception) {
                Log.e(TAG, "saveContactToGoogle: exception")
                e.printStackTrace()
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val item = menu.findItem(R.id.action_search)
//        val action_save = menu.findItem(R.id.action_save)
        val action_download = menu.findItem(R.id.action_download)
        binding.searchView.setMenuItem(item)
        action_download?.isVisible = true
//        action_save?.isVisible = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                Utils.hideSoftKeyboard(this)
                finish()
                return true
            }

//            R.id.action_save -> {
//                saveContactsToGoogleAccount()
//                return true
//            }

            R.id.action_download -> {
                toast("Downloading...")
                createExcelWorkbook()
                fillDataIntoExcel()
                val isExcelGenerated = storeExcelInStorage()
                if (isExcelGenerated) {
                    toast("User data Excel file downloaded successfully")
                    try { //                        val pdfReader = PdfReader(path)
                        //                        val stringParse = PdfTextExtractor.getTextFromPage(pdfReader, 1).trim { it <= ' ' }
                        //                        pdfReader.close()
                        val uriForFile = FileProvider.getUriForFile(baseContext, "${BuildConfig.APPLICATION_ID}.fileprovider", resultFile)
                        val intent = Intent(Intent.ACTION_VIEW, uriForFile)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.setDataAndType(uriForFile, "application/vnd.ms-excel")
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

//    private fun saveContactsToGoogleAccount() {
//        if (userList == null || userList?.size == 0) {
//            toast("No Records found")
//            return
//        }
//        toast("Started saving contacts to Google contacts")
//        val cpd: ProgressDialog? = ProgressDialog(mContext)
//        cpd?.setCancelable(false)
//        cpd?.show()
//        CoroutineScope(Dispatchers.IO).launch {
//            for (user in userList!!) {
//                launch {
//                    val person = Person().apply {
//                        names = listOf(Name().apply {
//                            givenName = user.name
//                        })
//                        phoneNumbers = listOf(PhoneNumber().apply {
//                            value = user.contactNo
//                        })
//                        emailAddresses = listOf(EmailAddress().apply {
//                            value = user.email
//                        })
//                    }
//                    try {
//                        peopleService?.people()?.createContact(person)?.execute()
//                        Log.e(TAG, "saveContactToGoogle: success")
//                    } catch (e: Exception) {
//                        Log.e(TAG, "saveContactToGoogle: exception")
//                        e.printStackTrace()
//                    }
//                }
//            }
//            if (cpd != null && cpd.isShowing) cpd.dismiss()
//            toast("Finished saving contacts")
//        }
//    }

    /**
     * Method: Generate Excel Workbook
     */
    fun createExcelWorkbook() { // New Workbook
        workbook = HSSFWorkbook()
        cell = null

        // Cell style for header row
        val cellStyle = workbook?.createCellStyle()
        cellStyle?.fillForegroundColor = HSSFColor.AQUA.index
        cellStyle?.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
        cellStyle?.alignment = CellStyle.ALIGN_CENTER

        // New Sheet
        sheet = null
        sheet = workbook?.createSheet(EXCEL_SHEET_NAME)

        // Generate column headings
        val row = sheet?.createRow(0)
        cell = row?.createCell(0)
        cell?.setCellValue("Name")
        cell?.cellStyle = cellStyle
        cell = row?.createCell(1)
        cell?.setCellValue("Shop Name")
        cell?.cellStyle = cellStyle
        cell = row?.createCell(2)
        cell?.setCellValue("Phone Number")
        cell?.cellStyle = cellStyle
        cell = row?.createCell(3)
        cell?.setCellValue("Mail ID")
        cell = row?.createCell(4)
        cell?.setCellValue("Address")
        cell?.cellStyle = cellStyle
        cell = row?.createCell(5)
        cell?.setCellValue("Daily Entries Count")
        cell?.cellStyle = cellStyle
        cell = row?.createCell(6)
        cell?.setCellValue("Referrer")
        cell?.cellStyle = cellStyle
        cell = row?.createCell(7)
        cell?.setCellValue("User Type")
        cell?.cellStyle = cellStyle
        cell = row?.createCell(8)
        cell?.setCellValue("Created On")
        cell?.cellStyle = cellStyle
        cell = row?.createCell(8)
        cell?.setCellValue("Uid")
        cell?.cellStyle = cellStyle
    }

    private fun fillDataIntoExcel() {
        for (i in userList?.indices!!) { // Create a New Row for every new entry in list
            val rowData = sheet!!.createRow(i + 1)

            // Create Cells for each row
            cell = rowData.createCell(0)
            cell?.setCellValue(userList!![i].name)
            cell = rowData.createCell(1)
            cell?.setCellValue(userList!![i].shopName)
            cell = rowData.createCell(2)
            cell?.setCellValue(userList!![i].contactNo)
            cell = rowData.createCell(3)
            cell?.setCellValue(userList!![i].email)
            cell = rowData.createCell(4)
            cell?.setCellValue(userList!![i].address)
            cell = rowData.createCell(5)
            cell?.setCellValue(userList!![i].dailyEntriesCount.toString())
            cell = rowData.createCell(6)
            cell?.setCellValue(userList!![i].referrer)
            cell = rowData.createCell(7)
            cell?.setCellValue(userList!![i].userType)
            cell = rowData.createCell(8)
            cell?.setCellValue(userList!![i].createdAt)
            cell = rowData.createCell(8)
            cell?.setCellValue(userList!![i].uid)
        }
    }

    private fun storeExcelInStorage(): Boolean {
        var isSuccess: Boolean
        val fileDirectory = File(Environment.getExternalStorageDirectory(), "Download")
        if (!fileDirectory.exists()) {
            fileDirectory.mkdirs()
        }
        resultFile = File(fileDirectory.absolutePath + File.separator.toString() + "users_data_${System.currentTimeMillis()}.xls") //            simplyPdfDocument = SimplyPdf.with(baseContext, resultFile).colorMode(DocumentInfo.ColorMode.COLOR).paperSize(PrintAttributes.MediaSize.ISO_A4) //                .margin(Margin(20U, 20U, 20U, 20U)) //                .pageModifier(PageHeader(headerList)).firstPageBackgroundColor(Color.WHITE).paperOrientation(DocumentInfo.Orientation.PORTRAIT).build()
        resultFileUri = Uri.fromFile(resultFile) //        val file = File(this.getExternalFilesDir(null), fileName)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(resultFile)
            workbook?.write(fileOutputStream)
            Log.e(TAG, "Writing file $resultFile")
            isSuccess = true
        } catch (e: IOException) {
            Log.e(TAG, "Error writing Exception: ", e)
            isSuccess = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file due to Exception: ", e)
            isSuccess = false
        } finally {
            try {
                fileOutputStream?.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return isSuccess
    }

    private fun getUserList() {
        launchCoroutine({
            app?.apiRequestHelper?.apiService?.users?.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>, response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        val json = response.body()?.string()
                        if (json == null || json == "null") {
                            return
                        }
                        val type: Type = object : TypeToken<HashMap<String, User>?>() {}.type
                        val map: HashMap<String, User> = Gson().fromJson(json, type)
                        userList?.clear()
                        userList?.addAll(map.values.toMutableList())
                        try {
                            val df: DateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                            Collections.sort(userList, Comparator { o1, o2 -> //                                if (o1.createdAt.isNullOrEmpty() || o2.createdAt.isNullOrEmpty()) return@Comparator 1
                                if (o1.createdAt == null || o2.createdAt == null || o1.createdAt!!.isEmpty() || o2.createdAt!!.isEmpty()) {
                                    return@Comparator 1
                                }
                                return@Comparator try { // Try parsing as English date
                                    df.parse(o2.createdAt.toString())!!.compareTo(df.parse(o1.createdAt.toString()))
                                } catch (e: ParseException) { // If parsing as English date fails, try parsing as Marathi date
                                    Log.e(TAG, "ParseException: " + o2.createdAt + "||" + o1.createdAt)
                                    val marathiDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale("mr"))
                                    marathiDateFormat.parse(o2.createdAt.toString())!!.compareTo(marathiDateFormat.parse(o1.createdAt.toString()))
                                } catch (e: Exception) { // If parsing as English date fails, try parsing as Marathi date
                                    Log.e(TAG, "Exception: " + o2.createdAt + "||" + o1.createdAt)
                                    val marathiDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale("mr"))
                                    marathiDateFormat.parse(o2.createdAt.toString())!!.compareTo(marathiDateFormat.parse(o1.createdAt.toString()))
                                } //                            return@Comparator df.parse(o2.createdAt.toString())!!.compareTo(df.parse(o1.createdAt.toString()))
                            })
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (userList != null && userList?.size!! > 0) {
                            binding.toolbarTitle?.text = "Users(${userList?.size})"
                            adapter = UserAdapter(mContext, userList)

                            binding.recyclerView?.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
                            binding.recyclerView?.adapter = adapter
                            adapter?.itemClickListener = object : UserAdapter.ItemClickListener {
                                override fun onItemClick(position: Int) {
                                }

                                override fun onCallOrSms(phoneNo: String) {
                                    callOrSms(phoneNo)
                                }

                                override fun saveContactToGoogleContact(position: Int) {
                                    val user = userList?.get(position)
                                    updateContactSavedByLocalRemote(position)
                                    saveContactAvoidingDuplicates(user?.name.toString(), user?.contactNo.toString(), user?.email)
                                }
                            }
                            binding.recyclerView?.visible()
                            binding.tvError?.gone()

                            binding.searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
                                override fun onQueryTextSubmit(
                                    query: String
                                ): Boolean { //Do some magic
                                    if (adapter != null) adapter?.filter(query)
                                    return false
                                }

                                override fun onQueryTextChange(
                                    newText: String
                                ): Boolean { //Do some magic
                                    if (adapter != null) adapter?.filter(newText)
                                    return false
                                }
                            })
                        } else {
                            binding.recyclerView?.gone()
                            binding.tvError?.visible()
                        }
                        signIn()
                    } else {
                        binding.recyclerView?.gone()
                        binding.tvError?.visible()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                }
            }) //            database.child(Utils.USERS_TABLE).addValueEventListener(object : ValueEventListener {
            //                override fun onDataChange(snapshot: DataSnapshot) {
            //                    Log.i("firebase", "Got value ${snapshot.value}") // Get user value
            //                    val json = Gson().toJson(snapshot.value)
            //                    val type: Type = object : TypeToken<HashMap<String, User>?>() {}.type
            //                    val map: HashMap<String, User> = Gson().fromJson(json, type)
            //                    userList?.clear()
            //                    userList?.addAll(map.values.toMutableList())
            ////                    try {
            //                        val df: DateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
            //                        Collections.sort(userList, Comparator { o1, o2 ->
            //                            if (o1.createdAt.isNullOrEmpty() || o2.createdAt.isNullOrEmpty()) return@Comparator 0
            //                            return@Comparator try { // Try parsing as English date
            //                                df.parse(o2.createdAt.toString())!!.compareTo(df.parse(o1.createdAt.toString()))
            //                            } catch (e: ParseException) { // If parsing as English date fails, try parsing as Marathi date
            //                                val marathiDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("mr"))
            //                                marathiDateFormat.parse(o2.createdAt.toString())!!.compareTo(df.parse(o1.createdAt.toString()))
            //                            }
            ////                            return@Comparator df.parse(o2.createdAt.toString())!!.compareTo(df.parse(o1.createdAt.toString()))
            //                        })
            ////                    } catch (e: ParseException) {
            ////                        e.printStackTrace()
            ////                    } catch (e: Exception) {
            ////                        e.printStackTrace()
            ////                    }
            //                    if (userList != null && userList?.size!! > 0) {
            //                        toolbar_title?.text = "Users(${userList?.size})"
            //                        adapter = UserAdapter(mContext, userList)
            //
            //                        recyclerView?.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
            //                        recyclerView?.adapter = adapter
            //                        adapter?.itemClickListener = object : UserAdapter.ItemClickListener {
            //                            override fun onItemClick(position: Int) {
            //                            }
            //
            //                            override fun onCallOrSms(phoneNo: String) {
            //                                callOrSms(phoneNo)
            //                            }
            //                        }
            //                        recyclerView?.visible()
            //                        tvError?.gone()
            //
            //                        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            //                            override fun onQueryTextSubmit(query: String): Boolean { //Do some magic
            //                                if (adapter != null) adapter?.filter(query)
            //                                return false
            //                            }
            //
            //                            override fun onQueryTextChange(newText: String): Boolean { //Do some magic
            //                                if (adapter != null) adapter?.filter(newText)
            //                                return false
            //                            }
            //                        })
            //                    } else {
            //                        recyclerView?.gone()
            //                        tvError?.visible()
            //                    }
            //                }
            //
            //                override fun onCancelled(error: DatabaseError) {
            //                }
            //            })
            //            app?.apiRequestHelper?.apiService?.dailyEntries?.enqueue(object : Callback<ResponseBody> {
            //                //        app?.apiRequestHelper?.apiService?.dailyEntries?.enqueue(object : Callback<ResponseBody> {
            //                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) { //                Log.e("TAG", "onResponse: after")
            //                    if (response.isSuccessful) {
            //                        val json = response.body()?.string()
            //                        if (json == null || json == "null") {
            //                            return
            //                        }
            //                        val type: Type = object : TypeToken<HashMap<String, HashMap<String, DailyEntry>>?>() {}.type
            //                        val hashMap: HashMap<String, HashMap<String, DailyEntry>> = Gson().fromJson(json, type) //                                            val map: HashMap<String, DailyEntry>? = hashMap[uid!!]
            //                        //                        val type: Type = object : TypeToken<java.util.HashMap<String, DailyEntry>?>() {}.type
            //                        //                        val map: java.util.HashMap<String, DailyEntry>? = Gson().fromJson(json, type)
            //                        userList?.forEach { user ->
            //                            hashMap.keys.forEach {uid ->
            //                                if (uid.equals(user.uid)) {
            //                                    user.dailyEntriesCount = hashMap[uid]?.values?.size?.toLong() ?: 0
            //                                    database.child(Utils.USERS_TABLE).child(user.uid!!).setValue(user)
            //                                }
            //                            }
            //                        }
            //                        Log.e("in", "fail response")
            //                    }
            //                }
            //
            //                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            //                    Log.e("in", "failure")
            //                }
            //            })
        }, { coroutineContext, throwable ->
            throwable.printStackTrace()
        })
    }

    private fun updateContactSavedByLocalRemote(position: Int) {
        val contactSavedBy = userList?.get(position)?.contactSavedBy
        val commaSeparatedString = contactSavedBy?.addToCommaSeparatedString(signInEmail!!)
        userList?.get(position)?.contactSavedBy = commaSeparatedString
        val user = userList?.get(position)
        user?.contactSavedBy = commaSeparatedString
        user?.uid?.let { database.child(Utils.USERS_TABLE).child(it).setValue(user) }
    }

    override fun onDestroy() {
        googleSignInClient?.signOut()
        super.onDestroy()
    }

    companion object {
        private var EXCEL_SHEET_NAME = "Sheet1"
        private var EXCEL_FILE_NAME = "users.xls"
    }
}
