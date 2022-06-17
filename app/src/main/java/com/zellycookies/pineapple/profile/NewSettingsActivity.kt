package com.zellycookies.pineapple.profile

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.conversation.Object.GroupObject
import com.zellycookies.pineapple.login.Login
import com.zellycookies.pineapple.matched.Matched_Activity
import com.zellycookies.pineapple.matched.ProfileAdapter
import com.zellycookies.pineapple.utils.FirebaseMethods
import com.zellycookies.pineapple.utils.GPS
import com.zellycookies.pineapple.utils.TopNavigationViewHelper
import im.crisp.client.ChatActivity
import im.crisp.client.Crisp
import java.util.ArrayList

class NewSettingsActivity : AppCompatActivity() {
    private val mContext: Context = this@NewSettingsActivity

    //firebase
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_settings)
        setupFirebaseAuth()
        setupTopNavigationView()

        addButtonListener()
    }

    private fun addButtonListener() {
        val btnEditProfile : Button = findViewById(R.id.btn_settings_profile)
        val btnFilter : Button = findViewById(R.id.btn_settings_filter)
        val btnInfo : Button = findViewById(R.id.btn_settings_info)
       // val btnNotification : Button = findViewById(R.id.btn_settings_noti)
        val btnBlock : Button = findViewById(R.id.btn_blocked_users)
       // val btnNoti : Button = findViewById(R.id.btn_settings_noti)
        val btnChat : Button = findViewById(R.id.btn_chatSupport)

        btnEditProfile.setOnClickListener {
            val intent = Intent(mContext, EditProfileActivity::class.java)
            startActivity(intent)
        }

        btnInfo.setOnClickListener {
            val intent = Intent(mContext, AccountActivity::class.java)
            startActivity(intent)
        }

        btnBlock.setOnClickListener {
            val intent = Intent(mContext, BlockedList::class.java)
            startActivity(intent)
        }

        btnFilter.setOnClickListener {
            val intent = Intent(mContext, FilterActivity::class.java)
            startActivity(intent)
        }

        btnChat.setOnClickListener {
            Crisp.configure(getApplicationContext(), "942b95e5-7079-4736-9381-ae51bea55428");
            val crispIntent = Intent(this, ChatActivity::class.java)
            startActivity(crispIntent)
        }
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

    /**
     * Setup the firebase auth object
     */
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
                val intent = Intent(this@NewSettingsActivity, Login::class.java)

                //clear the activity stack， in case when sign out, the back button will bring the user back to the previous activity
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
    // onBackPressed
    override fun onBackPressed() {}
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        mAuth!!.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    companion object {
        private const val TAG = "NewSettingsActivity"
        private const val ACTIVITY_NUM = 4
    }
}