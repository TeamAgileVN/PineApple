package com.zellycookies.pineapple.fire

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.conversation.Object.GroupObject
import com.zellycookies.pineapple.login.Login
import com.zellycookies.pineapple.main.ProfileCheckinMain
import com.zellycookies.pineapple.matched.ProfileAdapter
import com.zellycookies.pineapple.utils.FirebaseMethods
import com.zellycookies.pineapple.utils.GPS
import com.zellycookies.pineapple.utils.TopNavigationViewHelper
import com.zellycookies.pineapple.utils.User
import java.util.*

class FireSearchActivity : AppCompatActivity() {

    private var tabLayout : TabLayout? = null
    private var search: EditText? = null

    private var userId: String? = null
    private var userSex: String? = null
    private var lookForSex: String? = null
    private var latitude = 37.349642
    private var longitude = -121.938987
    private var idGroupList: ArrayList<String>? = null
    private var gps: GPS? = null
    private var mAdapter: ProfileAdapter? = null
    private var likeList: MutableList<GroupObject> = ArrayList<GroupObject>()
    private var copyList: MutableList<GroupObject> = ArrayList<GroupObject>()

    //firebase
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var dbRef: DatabaseReference? = null
    private var firebaseMethods: FirebaseMethods? = null
    private var mFirebaseFirestore: FirebaseFirestore? = null

    private val mContext: Context = this@FireSearchActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fire_search)
        setupTabLayout()
        setupTopNavigationView()

        initList()
    }

    private fun initList() {
        firebaseMethods = FirebaseMethods(mContext)

        setupFirebaseAuth()

        searchFunc()
        userId = FirebaseAuth.getInstance().currentUser!!.uid
        gps = GPS(this)
        dbRef = FirebaseDatabase.getInstance().reference
        mFirebaseFirestore = FirebaseFirestore.getInstance()
        checkUserSex()
        mAdapter = ProfileAdapter(this, R.layout.vwyl_item, likeList)
        val listView = findViewById<View>(R.id.likeList) as ListView
        listView.adapter = mAdapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                Log.d(TAG, "onItemClick: The list has been clicked")
                checkClickedItem(position)
            }
    }

    private fun searchFunc() {
        search = findViewById<View>(R.id.searchBar) as EditText
        search!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                searchText()
            }

            override fun afterTextChanged(s: Editable) {
                searchText()
            }
        })
    }

    private fun searchText() {
        val text = search!!.text.toString().lowercase(Locale.getDefault())
        Log.d(TAG, text)
        if (text.isNotEmpty() && text != "") {
            if (likeList.size != 0) {
                likeList.clear()
                for (groupObject in copyList) {
                    if (groupObject.userMatch.username!!.lowercase(Locale.getDefault())
                            .contains(text)
                    ) {
                        likeList.add(groupObject)
                    }
                }
            } else {
                for (groupObject in copyList) {
                    if (groupObject.userMatch.username!!.lowercase(Locale.getDefault())
                            .contains(text)
                    ) {
                        likeList.add(groupObject)
                    }
                }
            }
        } else {
            likeList.clear()
        }
        mAdapter?.notifyDataSetChanged()
    }

    private fun checkClickedItem(position: Int) {
        val groupObject: GroupObject = likeList[position]
        val user: User = groupObject.userMatch
        //calculate distance
        val distance = gps!!.calculateDistance(latitude, longitude, user.latitude, user.longtitude)
        val intent = Intent(this, ProfileCheckinMain::class.java)
        //        intent.putExtra("classUser", user);
//        intent.putExtra("distance", distance);

        val name: String? = user.username
        val dob: String? = user.dateOfBirth
        val bio: String? = user.description
        val isHobby_movies: String = if (user.isHobby_movies) "Movies\t" else " "
        val isHobby_music: String = if (user.isHobby_music) "Music\t" else " "
        val isHobby_art: String = if (user.isHobby_art) "Art\t" else " "
        val isHobby_food: String = if (user.isHobby_food) "Food" else " "
        val interest = "$isHobby_movies$isHobby_music$isHobby_art$isHobby_food."
        val profileImageURL : String? = user.profileImageUrl

        intent.putExtra("name", name)
        intent.putExtra("dob", dob)
        intent.putExtra("bio", bio)
        intent.putExtra("interest", interest)
        intent.putExtra("distance", distance)
        intent.putExtra("photo", profileImageURL)
        intent.putExtra("userId", user.user_id)
        intent.putExtra("userSex", user.sex)

        startActivity(intent)
    }

    private fun checkUserSex() {
        val maleDb = dbRef!!.child("male")
        maleDb.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.key == userId) {
                    Log.d(TAG, "onChildAdded: the sex is male")
                    userSex = "male"
                    //update location
                    latitude = dataSnapshot.getValue(User::class.java)!!.latitude
                    longitude = dataSnapshot.getValue(User::class.java)!!.longtitude
                    lookForSex = dataSnapshot.getValue(User::class.java)!!.preferSex
                    findAllUID()
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        val femaleDb = dbRef!!.child("female")
        femaleDb.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.key == userId) {
                    Log.d(TAG, "onChildAdded: the sex is female")
                    userSex = "female"

                    //update location
                    latitude = dataSnapshot.getValue(User::class.java)!!.latitude
                    longitude = dataSnapshot.getValue(User::class.java)!!.longtitude
                    lookForSex = dataSnapshot.getValue(User::class.java)!!.preferSex
                    findAllUID()
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun findAllUID() {
        idGroupList = ArrayList()

        val potentialMatch = FirebaseDatabase.getInstance().reference.child(lookForSex!!)
        potentialMatch.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.exists() && dataSnapshot.key != userId) {
                    val curUser = dataSnapshot.getValue(User::class.java)
                    val groupObject = GroupObject(curUser!!)
                    //likeList.add(groupObject)
                    copyList.add(groupObject)
                    mAdapter?.notifyDataSetChanged()
                    Log.d(TAG, "onDataChange: List's size is " + likeList.size)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun setupFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // user is signed in
                Log.d(TAG, "onAuthStateChanged: signed_in:" + user.uid)
            } else {
                //user is signed out
                Log.d(TAG, "onAuthStateChanged: signed_out")
                Log.d(TAG, "onAuthStateChanged: navigating back to login screen.")
                val intent = Intent(this, Login::class.java)

                //clear the activity stack， in case when sign out, the back button will bring the user back to the previous activity
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun setupTabLayout() {
        tabLayout = findViewById(R.id.tabLayout)

        tabLayout!!.addTab(tabLayout!!.newTab().setText("Hot Takes"))
        tabLayout!!.addTab(tabLayout!!.newTab().setText("Search"))
        tabLayout!!.tabGravity = TabLayout.GRAVITY_FILL

        tabLayout!!.getTabAt(TAB_NUM)?.select()

        tabLayout!!.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> {
                        val intent = Intent(this@FireSearchActivity, FireHotTakesActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupTopNavigationView() {
        Log.d(TAG, "setupTopNavigationView: setting up TopNavigationView")
        val tvEx = findViewById<View>(R.id.topNavViewBar) as BottomNavigationView
        TopNavigationViewHelper.setupTopNavigationView(tvEx)
        TopNavigationViewHelper.enableNavigation(mContext, tvEx)
        val menu = tvEx.menu
        val menuItem = menu.getItem(ACTIVITY_NUM)
        menuItem.isChecked = true
    }

    companion object {
        private const val TAG = "FireSearchActivity"
        private const val TAB_NUM = 1
        private const val ACTIVITY_NUM = 1
    }
}