package com.zellycookies.pineapple.profile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.introduction.IntroductionMain
import com.zellycookies.pineapple.utility.UtilityHistoryActivity

class ChangePasswordActivity : AppCompatActivity() {
    //firebase
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var user : FirebaseUser? = null
    private var userSex : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        setupFirebaseAuth()
        checkUserSex()

    }

    // Change password
    private fun changePassword() {
        val inputPasswordOld : EditText = findViewById(R.id.input_password_old)
        val inputPasswordNew : EditText = findViewById(R.id.input_password_new)
        val inputPasswordConfirm : EditText = findViewById(R.id.input_password_confirm)

        Log.d(TAG, "Getting users Credentials")
        val credential : AuthCredential = EmailAuthProvider.getCredential(
            user!!.email!!, inputPasswordOld.text.toString())
        Log.d(TAG, user!!.toString())
        Log.d(TAG, "Changing password")
        user!!.reauthenticate(credential).addOnCompleteListener { taskAuth ->
            if (taskAuth.isSuccessful) {
                Log.d(TAG, "User re-authenticated")
                if (inputPasswordNew.text.toString() == inputPasswordConfirm.text.toString()) {
                    user!!.updatePassword(inputPasswordNew.text.toString())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "Changed password successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.d(TAG, "Changed password successfully")
                                UtilityHistoryActivity.uploadActivity(userSex!!, user!!.uid, "You changed your password")
                            } else {
                                Toast.makeText(
                                    this@ChangePasswordActivity,
                                    "Failed to change password",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.d(TAG, "Failed to change password")
                            }
                        }
                } else {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "New password doesn't match",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(TAG, "New password doesn't match")
                }
            } else {
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "Failed to re-authenticate",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Failed to re-authenticate user")
            }
        }
    }

    private fun checkCurrentUser(user: FirebaseUser?) {
        Log.d(TAG, "checkCurrentUser: checking if user is logged in")
        if (user == null) {
            val intent = Intent(this, IntroductionMain::class.java)
            startActivity(intent)
        }
    }

    private fun setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: check user")
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser

            //check if the user is logged in
            checkCurrentUser(user)
            if (user != null) {
                // user is signed in
                Log.d(TAG, "onAuthStateChanged: signed_in:" + user.uid)
            } else {
                //user is signed out
                Log.d(TAG, "onAuthStateChanged: signed_out")
            }
        }
        user = FirebaseAuth.getInstance().currentUser
    }

    private fun checkUserSex() {
        if (user != null) {
            val maleDb = FirebaseDatabase.getInstance().reference.child("male")
            maleDb.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    if (dataSnapshot.key == user!!.uid) {
                        userSex = "male"
                        Log.d(TAG,"onChildAdded: the sex is $userSex")
                        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                            changePassword()
                        }
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {}
            })
            val femaleDb = FirebaseDatabase.getInstance().reference.child("female")
            femaleDb.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    if (dataSnapshot.key == user!!.uid) {
                        userSex = "female"
                        Log.d(TAG, "onChildAdded: the sex is $userSex")
                        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                            changePassword()
                        }
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    companion object {
        private const val TAG = "ChangePasswordActivity"
    }
}