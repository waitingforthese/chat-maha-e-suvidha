package rahul.jagtap.dmas.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import rahul.jagtap.dmas.databinding.ActivityViewBillsBinding
import rahul.jagtap.dmas.extensions.toast
import rahul.jagtap.dmas.BaseActivity
import rahul.jagtap.dmas.R
import rahul.jagtap.dmas.adapter.TextMsgListAdapter
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.TextMsg
import rahul.jagtap.dmas.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type


class TextMsgActivity : BaseActivity() {
//    private var uid: String? = ""
    var list = ArrayList<TextMsg>()
    var adapter: TextMsgListAdapter? = null

    private lateinit var binding: ActivityViewBillsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewBillsBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarLayout.toolbarTitle?.text = "Text Messages"

//        uid = app?.preferences?.loggedInUser?.uid
        adapter = TextMsgListAdapter(mContext, list)

        binding.recyclerView?.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        binding.recyclerView?.adapter = adapter
        adapter?.itemClickListener = object : TextMsgListAdapter.ItemClickListener {
            override fun onDeleteClick(position: Int) {
                Utils.showDialog(mContext, "Are you sure want to delete the record?", true) { dialog, which ->
                    run {
                        dialog.dismiss()
                        val selItem = list[position]
                        list?.removeAt(position) // Delete DB Record
                        selItem.pushKey?.let {
                            database.child(Utils.TEXT_MSG_TABLE).child(selItem.pushKey!!).removeValue()
                        }
                        notifyAdapter()
                        toast("Record deleted successfully")
                    }
                }
            }

            override fun onEditClick(position: Int) {
                val selItem = list[position]
                val intent = Intent(mContext, AddTextMsgActivity::class.java)
                intent.putExtra("textMsg", selItem)
                resultLauncher.launch(intent)
//                startActivityForResult(Intent(mContext, AddTextMsgActivity::class.java).putExtra("textMsg", selItem), 2)
            }
        }
        setEntriesData()
    }

    private fun setEntriesData() {
        binding.progressBar?.visible()
        app?.apiRequestHelper?.apiService?.textMessages?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                binding.progressBar?.gone()
                if (response.isSuccessful) {
                    val json = response.body()?.string()
                    if (json == null || json == "null") {
                        notifyAdapter()
                        return
                    }
                    val type: Type = object : TypeToken<HashMap<String, TextMsg>?>() {}.type
                    val map: HashMap<String, TextMsg>? = Gson().fromJson(json, type)
                    val textMessages = map?.values
                    if (!textMessages.isNullOrEmpty()) {
                        list.clear()
                        list.addAll(textMessages)
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
        if (list != null && list.size > 0) {
            adapter?.notifyDataSetChanged()
            binding.recyclerView?.visible()
            binding.tvError?.gone()
        } else {
            binding.recyclerView?.gone()
            binding.tvError?.visible()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_text_msg, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                val intent = Intent(mContext, AddTextMsgActivity::class.java)
                resultLauncher.launch(intent)
                return true
            }

            android.R.id.home -> {
                Utils.hideSoftKeyboard(this)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            setEntriesData()
        }
    }

    companion object {

    }
}
