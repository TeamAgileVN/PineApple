package com.zellycookies.pineapple.login

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.home.HomeSwipeActivity
import com.zellycookies.pineapple.profile.Profile_Activity
import java.text.DateFormat
import java.util.*
import kotlin.collections.HashMap

import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v7.app.AppCompatActivity;
class Login : AppCompatActivity() {
    //firebase
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var mContext: Context? = null
    private var mEmail: EditText? = null
    private var mPassword: EditText? = null

    private lateinit var googleSignInButton: Button
    var mGoogleSignInClient: GoogleSignInClient? = null
    val RC_SIGN_IN = 123

    private lateinit var callbackManager: CallbackManager
    var loginButton: LoginButton? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContentView(R.layout.activity_login)
        mEmail = findViewById<View>(R.id.input_email) as EditText?
        mPassword = findViewById<View>(R.id.input_password) as EditText?
        mContext = this@Login
        setupFirebaseAuth()

        init()

        googleSignInButton = findViewById(R.id.btn_login_google)
        googleSignInButton.setOnClickListener {
            googleSignIn();
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("47321728033-0peejdn6qhknti0eof4kmu7vivrjmsg9.apps.googleusercontent.com")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        callbackManager = CallbackManager.Factory.create();

        loginButton = findViewById(R.id.login_button);
        loginButton!!.setReadPermissions("email", "public_profile", "user_friends");

        loginButton!!.setOnClickListener {
            facebookSignIn()
        }

    }

    private fun facebookSignIn() {
        loginButton!!.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onError(error: FacebookException) {
                Log.e("ERROR_SIGNIN", error.message!!)
            }

            override fun onSuccess(result: LoginResult) {
                handleFacebookAccessToken(result.accessToken)
            }

            override fun onCancel() {
                Log.d("CANCEL", "Cancelled")
            }
        })
    }

    private fun handleFacebookAccessToken(accessToken: AccessToken?) {
        val credential = FacebookAuthProvider.getCredential(accessToken!!.token)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener { result ->
                val email = result.user!!.email
                val fbid = result.user!!.uid
                Log.d(ContentValues.TAG, "Error:$fbid")
                val name = result.user!!.displayName
                val pass = accessToken.userId
                val passHash = BCrypt.withDefaults().hashToString(12, pass.toCharArray())
                var flag = 0

                // Save Facebook User to Firestore
                val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
                val documentRef =
                    FirebaseFirestore.getInstance().collection("male").document(currentUserId)
                val dbx =
                    FirebaseFirestore.getInstance().collection("male").document(currentUserId).get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                FirebaseFirestore.getInstance().collection("male").get()
                                    .addOnSuccessListener { result ->
                                        for (documents in result) {
                                            if (documents.id == fbid) {
                                                flag = 1
                                            }
                                        }
                                        if (flag == 0) {
                                            val user: MutableMap<String, Any> = HashMap()
                                            user["dateOfBirth"] = "01-01-2001"
                                            user["description"] = ""
                                            user["email"] = email!!
                                            user["hobby_art"] = false
                                            user["hobby_food"] = false
                                            user["hobby_movies"] = false
                                            user["hobby_music"] = false
                                            user["latitude"] = 10.794374959628545
                                            user["longtitude"] = 106.71585601745473
                                            user["phone_number"] = ""
                                            user["preferDistance"] = 50
                                            user["preferMaxAge"] = 100
                                            user["preferMinAge"] = 16
                                            user["preferSex"] = "female"
                                            user["profileImageUrl"] = "defaultMale"
                                            user["sex"] = "male"
                                            user["showDistance"] = true
                                            user["showDoB"] = true
                                            user["user_id"] = ""
                                            user["username"] = name!!
                                            user["password"] = passHash
                                            user["lastLogin"] = FieldValue.serverTimestamp()

                                            documentRef.set(user)
                                        }

                                        updateRecipientToken()
                                    }
                            } else {
                                Toast.makeText(
                                    this, task.exception!!.message.toString(), Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                // Start Homepage Activity
                val intentToHomePageActivity =
                    Intent(this, HomeSwipeActivity::class.java)
                intentToHomePageActivity.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intentToHomePageActivity)
                finish()
            }
    }

    private fun updateRecipientToken() {
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val dbRef = FirebaseFirestore.getInstance().collection("male")
            .document(currentUserID)
        FirebaseMessaging.getInstance().token.addOnCompleteListener{ task ->
            if(task.isSuccessful){
                val token = task.result.toString()
                dbRef.update("user_id", token)
            }
        }
    }

    private fun googleSignIn() {
        val signInIntent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.d(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(
                this
            ) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val intent = Intent(this@Login, HomeSwipeActivity::class.java)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.d(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this@Login,
                        "Authentication Failed.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
    }


    private fun isStringNull(string: String): Boolean {
        Log.d(TAG, "isStringNull: checking string if null.")
        return string == ""
    }

    //----------------------------------------Firebase----------------------------------------
    private fun init() {
        //initialize the button for logging in
        val btnLogin = findViewById<View>(R.id.btn_login) as Button
        btnLogin.setOnClickListener {
            Log.d(TAG, "onClick: attempting to log in")
            val email: String = mEmail?.getText().toString()
            val password: String = mPassword?.getText().toString()
            if (isStringNull(email) || isStringNull(password)) {
                Toast.makeText(mContext, "You must fill out all the fields", Toast.LENGTH_SHORT)
                    .show()
            } else {
                mAuth?.signInWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener(this@Login, object : OnCompleteListener<AuthResult?> {
                        override fun onComplete(task: Task<AuthResult?>) {
                            val user: FirebaseUser? = mAuth?.currentUser
                            if (task.isSuccessful) {
                                // Sign in success, update UI with the signed-in user's information
                                try {
                                    if (user != null) {
                                        if (user.isEmailVerified) {
                                            checkUserSex()
                                        } else {
                                            Toast.makeText(
                                                mContext,
                                                "Email is not verified \n check your email inbox.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            mAuth?.signOut()
                                        }
                                    }
                                } catch (e: NullPointerException) {
                                    Log.e(
                                        TAG,
                                        "signInWithEmail: onComplete: " + task.isSuccessful()
                                    )
                                }
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException())
                                Toast.makeText(
                                    this@Login, R.string.auth_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            }
        }
        val linkSignUp: TextView = findViewById<View>(R.id.link_signup) as TextView
        linkSignUp.setOnClickListener(View.OnClickListener {
            Log.d(TAG, "onClick: navigating to register screen")
            val intent = Intent(this@Login, RegisterBasicInfo::class.java)
            startActivity(intent)
        })
        /**
         * If the user is logged in then navigate to HomeActivity and call 'finish()'
         */

        val linkForgotPassword = findViewById<View>(R.id.link_forgotPassword)
        linkForgotPassword.setOnClickListener {
            Log.d(TAG, "onClick: navigating to forgot password screen")
            val intent = Intent(this@Login, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
        if (mAuth != null && mAuth?.currentUser != null) {
            val intent = Intent(this@Login, Profile_Activity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Setup the firebase auth object
     */
    private fun setupFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance()
        //mAuth!!.signOut()
        mAuthListener = object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                val user: FirebaseUser? = firebaseAuth.getCurrentUser()
                if (user != null) {
                    // user is signed in
                    Log.d(TAG, "onAuthStateChanged: signed_in:" + user.getUid())
                } else {
                    //user is signed out
                    Log.d(TAG, "onAuthStateChanged: signed_out")
                }
            }
        }
    }

    override fun onBackPressed() {}

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        mAuthListener?.let { mAuth?.addAuthStateListener(it) }
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth?.removeAuthStateListener(mAuthListener!!)
        }
    }

    fun checkUserSex() {
        val user = FirebaseAuth.getInstance().currentUser
        val maleDb = FirebaseDatabase.getInstance().reference.child("male")
        maleDb.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.key == user!!.uid) {
                    Log.d(TAG, "onChildAdded: the sex is male")
                    checkUserDeactivated("male", user.uid)
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
                    Log.d(TAG, "onChildAdded: the sex is female")
                    checkUserDeactivated("female", user.uid)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun checkUserDeactivated(userSex : String, userId : String) {
        FirebaseDatabase.getInstance().reference.child(userSex).child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.child("deactivated").value == null) {
                        Log.d(TAG, "onComplete: success, email is verified.")
                        val intent = Intent(this@Login, HomeSwipeActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(mContext, "User is deactivated", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "User $userId is deactivated")
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}