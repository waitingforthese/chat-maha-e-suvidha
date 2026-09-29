package rahul.jagtap.dmas.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import rahul.jagtap.dmas.widget.MaterialSearchView
import rahul.jagtap.dmas.BaseActivity
import rahul.jagtap.dmas.R
import rahul.jagtap.dmas.adapter.UserListAdapter
import rahul.jagtap.dmas.databinding.ActivityUserListBinding
import rahul.jagtap.dmas.extensions.gone
import rahul.jagtap.dmas.extensions.visible
import rahul.jagtap.dmas.model.User
import rahul.jagtap.dmas.utils.Utils
import java.lang.reflect.Type
import java.util.HashMap


class SelectUserActivity : BaseActivity() {
    var adapter: UserListAdapter? = null
    var userList = java.util.ArrayList<User>()
    lateinit var binding: ActivityUserListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        if (Utils.disableScreenshot) this.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle?.text = "Select User"
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
                setupRecycler()
                //                setupSpinner()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun setupRecycler() {
        if (userList != null && userList.size > 0) {
            binding.tvTotalUsers?.text = "Total Users: ${userList.size}"
            adapter = UserListAdapter(mContext, userList)

            binding.recyclerView?.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
            binding.recyclerView?.adapter = adapter
            adapter?.itemClickListener = object : UserListAdapter.ItemClickListener {
                override fun onItemClick(position: Int) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("selectedUser", Gson().toJson(userList[position]))
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
            binding.recyclerView?.visible()
            binding.tvError?.gone()

            binding.searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    //Do some magic
                    if (adapter != null) adapter?.filter(query)
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    //Do some magic
                    if (adapter != null) adapter?.filter(newText)
                    return false
                }
            })
        } else {
            binding.recyclerView?.gone()
            binding.tvError?.visible()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val item = menu.findItem(R.id.action_search)
        binding.searchView.setMenuItem(item)
        return true
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

    companion object
}
