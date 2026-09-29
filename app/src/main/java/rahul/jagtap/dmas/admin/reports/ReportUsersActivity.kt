package rahul.jagtap.dmas.admin.reports

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import rahul.jagtap.dmas.databinding.ActivityViewBillsBinding
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.BaseActivity
import rahul.jagtap.dmas.R
import rahul.jagtap.dmas.adapter.ReportUserListAdapter
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.ReportInfo
import rahul.jagtap.dmas.model.User
import rahul.jagtap.dmas.utils.Utils
import java.util.*
import kotlin.collections.ArrayList

class ReportUsersActivity : BaseActivity() {
    private lateinit var hashMap: HashMap<String, HashMap<String,  HashMap<String, ReportInfo>>>
    var list = ArrayList<User>()
    var adapter: ReportUserListAdapter? = null

    private lateinit var binding: ActivityViewBillsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewBillsBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)

        hashMap = intent.getSerializableExtra("reportsData") as HashMap<String, HashMap<String,  HashMap<String, ReportInfo>>>

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = getString(R.string.txt_reports)

        binding.recyclerView?.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        adapter = ReportUserListAdapter(mContext, list)
        binding.recyclerView?.adapter = adapter

        setUserList()

        adapter?.dateListListener = object : ReportUserListAdapter.DateListListener {
            override fun onItemClick(position: Int) {
                val userName = list[position].uid
                val reportsData: HashMap<String,  HashMap<String, ReportInfo>>? = hashMap?.get(userName)
                if (reportsData != null) startActivity(Intent(mContext, ReportDatesActivity::class.java).putExtra("reportsData", reportsData))
                else toast("No records found.")
            }
        }
    }

    fun setUserList() {
        hashMap.keys.forEachIndexed { index, s ->
            database.child(Utils.USERS_TABLE).child(s).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("TAG", "getUser:onCancelled", databaseError.toException())
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Get user value
                    val userInfo = dataSnapshot.getValue(User::class.java)
                    if (userInfo != null) {
                        list.add(userInfo)
                        notifyAdapter()
                    }
                }
            })
        }
        if (list.size > 0) {
            notifyAdapter()
            binding.recyclerView?.visible()
            binding.tvError?.gone()
        } else {
            binding.recyclerView?.gone()
            binding.tvError?.visible()
        }
    }

    private fun notifyAdapter() {
        binding.progressBar?.gone()
        adapter?.notifyDataSetChanged()
        if (list.size == 0) {
            binding.recyclerView?.gone()
            binding.tvError?.visible()
        } else {
            binding.recyclerView?.visible()
            binding.tvError?.gone()
        }
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

    companion object {

    }
}
