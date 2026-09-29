package rahul.jagtap.dmas.admin.reports

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.BaseActivity
import rahul.jagtap.dmas.R
import rahul.jagtap.dmas.adapter.BillDateListAdapter
import rahul.jagtap.dmas.databinding.ActivityViewReportsBinding
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.ReportData
import rahul.jagtap.dmas.model.ReportInfo
import rahul.jagtap.dmas.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type
import java.util.HashMap

class ReportTypesActivity : BaseActivity() {
    var reportData: ReportData? = null
    private var uid: String? = ""
    private var email: String? = ""
    private val TAG = ReportTypesActivity::class.java.simpleName
    var list = ArrayList<String>()
    var adapter: BillDateListAdapter? = null
    lateinit var binding: ActivityViewReportsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewReportsBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)

        email = app?.preferences?.loggedInUser?.email
        uid = app?.preferences?.loggedInUser?.uid

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = getString(R.string.txt_reports)

        binding.recyclerView?.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        adapter = BillDateListAdapter(mContext, list)
        binding.recyclerView?.adapter = adapter
        adapter?.dateListListener = object : BillDateListAdapter.DateListListener {
            override fun onItemClick(position: Int) {
                var strKey = list[position]
                if (strKey == "Accounting Services") {
                    strKey = "bills"
                } else if (strKey == "E-Suvidha") {
                    strKey = "esuvidha"
                }
                val reportsData: HashMap<String, HashMap<String, HashMap<String, ReportInfo>>>? = reportData?.map?.get(strKey)
                if (reportsData != null) startActivity(Intent(mContext, ReportUsersActivity::class.java).putExtra("reportsData", reportsData))
                else toast("No records found.")
            }
        }
        setReportsData()
    }

    private fun setReportsData() {
        binding.progressBar?.visible()
        app?.apiRequestHelper?.apiService?.reports?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                binding.progressBar?.gone()
                if (response.isSuccessful) {
                    val json = response.body()?.string()
                    if (json == null || json == "null") {
                        notifyAdapter()
                        return
                    }
                    reportData = ReportData()
                    val type: Type = object : TypeToken<HashMap<String, HashMap<String, HashMap<String, HashMap<String, ReportInfo>>>>?>() {}.type
                    val map: HashMap<String, HashMap<String, HashMap<String, HashMap<String, ReportInfo>>>> = Gson().fromJson(json, type)
                    reportData?.map = map
                    val dateList = reportData?.map?.keys?.toMutableList()
                    if (dateList != null && dateList.size > 0) {
                        if (dateList.contains("bills")) {
                            list.add("Accounting Services")
                        }
                        if (dateList.contains("esuvidha")) {
                            list.add("E-Suvidha")
                        }
                        notifyAdapter()
                        binding.recyclerView?.visible()
                        binding.tvError?.gone()
                    } else {
                        binding.recyclerView?.gone()
                        binding.tvError?.visible()
                    }
                } else {
                    Log.e("in", "fail response")
                    notifyAdapter()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.progressBar?.gone()
                Log.e("in", "failure")
                notifyAdapter()
            }
        })
    }

    private fun notifyAdapter() {
        binding.progressBar?.gone()
        adapter?.notifyDataSetChanged()
        if (list.size == 0) {
            binding.tvError?.visible()
        } else {
            binding.tvError?.gone()
        }
    }

    //    private fun setBillsData() {
    //        Firebase.database.getReference("Bills").child(uid!!)
    //            .addValueEventListener(object : ValueEventListener {
    //                override fun onDataChange(dataSnapshot: DataSnapshot) {
    //                    progressBar?.visible()
    //                    for (child in dataSnapshot.children) {
    //                        val bill = child.getValue(Bill::class.java)
    //                        bill?.let { billList.add(it) }
    //                    }
    //                    progressBar?.gone()
    //                    billListAdapter?.notifyDataSetChanged()
    //                    if (billList.size == 0) {
    //                        tvError?.visible()
    //                    } else {
    //                        tvError?.gone()
    //                    }
    //                }
    //
    //                override fun onCancelled(dataSnapshot: DatabaseError) {
    //                }
    //            })
    //    }

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

    companion object {

    }
}
