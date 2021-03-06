package com.zellycookies.pineapple.matched

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.utils.CalculateAge
import com.zellycookies.pineapple.utils.User
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class ProfileCheckinMatched : AppCompatActivity() {
    private var user: User? = null
    private val mContext: Context = this@ProfileCheckinMatched
    private val sendSMSButton: Button? = null
    private var sendEmailButton: Button? = null
    private var distance = 0
    private lateinit var image1: ImageView
    private lateinit var image2: ImageView
    private lateinit var image3: ImageView
    private lateinit var image4: ImageView
    private lateinit var userId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_checkin_matched)

        //initialize data
        val intent = intent
        user = intent.getSerializableExtra("classUser") as User?
        distance = intent.getIntExtra("distance", 1)
        Log.d(TAG, "onCreate: user name is " + user!!.username)
        Log.d(TAG, "onCreate: user id is " + user!!.user_id)
        val toolbar = findViewById<View>(R.id.toolbartag) as TextView
        toolbar.text = "Matched"
        val btnSafetyToolkit = findViewById<ImageButton>(R.id.btn_safety_toolkit)
        btnSafetyToolkit.setOnClickListener {
            val intentSafety = Intent(mContext, SafetyToolkitActivity::class.java)
            intentSafety.putExtra("otherName", user!!.username)
            intentSafety.putExtra("otherId", user!!.user_id)
            intentSafety.putExtra("otherSex", user!!.sex)
            startActivity(intentSafety)
        }

        //sendSMSButton = (Button) findViewById(R.id.send_sms);
        //sendEmailButton = findViewById<View>(R.id.send_email) as Button

        //setup display content
        userId = user!!.user_id!!
        val profile_name = findViewById<View>(R.id.profile_name) as TextView
        val profile_distance = findViewById<View>(R.id.profile_distance) as TextView
        val profile_numbers = findViewById<View>(R.id.profile_number) as TextView
        val profile_email = findViewById<View>(R.id.profile_email) as TextView
        val imageView = findViewById<View>(R.id.image_matched) as ImageView
        image1 = findViewById(R.id.image_1)
        image2 = findViewById(R.id.image_2)
        image3 = findViewById(R.id.image_3)
        image4 = findViewById(R.id.image_4)
        val profile_bio = findViewById<View>(R.id.bio_match) as TextView
        val profile_interest = findViewById<View>(R.id.interests_match) as TextView
        val profile_dob = findViewById<TextView>(R.id.profile_dob)

        //load user data to display
        val cal = CalculateAge(user!!.dateOfBirth!!)
        val age = cal.age
        profile_name.text = user!!.username + ", " + age
        profile_email.text = user!!.email
        profile_dob.text = user!!.dateOfBirth
        val append = if (distance == 1) "mile away" else "miles away"
        profile_distance.text = "$distance $append"
        if (user!!.description!!.length != 0) {
            profile_bio.text = user!!.description
        }
        if (user!!.phone_number!!.length != 0) {
            profile_numbers.text = user!!.phone_number
        } else {
            //sendSMSButton.setEnabled(false);
        }

        //append interests
        val interest = StringBuilder()
        if (user!!.isHobby_movies) {
            interest.append("Movies   ")
        }
        if (user!!.isHobby_music) {
            interest.append("Music   ")
        }
        if (user!!.isHobby_art) {
            interest.append("Art   ")
        }
        if (user!!.isHobby_food) {
            interest.append("Food   ")
        }
        profile_interest.text = interest.toString()
        val profileImageUrl = user!!.profileImageUrl
        when (profileImageUrl) {
            "defaultFemale" -> Glide.with(mContext).load(R.drawable.img_ava_female).into(imageView)
            "defaultMale" -> Glide.with(mContext).load(R.drawable.img_ava_male).into(imageView)
            else -> Glide.with(mContext).load(profileImageUrl).into(imageView)
        }
        val back = findViewById<View>(R.id.back) as ImageButton
        back.setOnClickListener { onBackPressed() }

        checkUserSex()
        /*
        sendSMSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                sendSMS(user.getPhone_number().toString(),user.getUsername().toString());
            }
        });
        *//*sendEmailButton!!.setOnClickListener {
            sendEmail(
                user!!.email.toString(),
                user!!.username.toString()
            )
        }*/
    }

    // This method will be called when send sms button in matched profile will be clicked. This open the default sms app.
    fun sendSMS(phoneNumber: String, userName: String) {
        val smsAppOpener = Intent(Intent.ACTION_VIEW)
        smsAppOpener.data = Uri.parse("sms:$phoneNumber")
        smsAppOpener.putExtra("sms_body", "Hi $userName, \nLove to have a coffee with you!!!!")
        startActivity(smsAppOpener)
    }

    // This method will be called when send email button in matched profile will be clicked. This open the default email app.
    private fun sendEmail(email: String, userName: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "plain/text"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding our Pink Moon Match!!!")
        intent.putExtra(Intent.EXTRA_TEXT, "Hi $userName, \nLove to have a coffee with you!!!!")
        startActivity(Intent.createChooser(intent, ""))
    }

    private fun loadImages(userSex : String, userId : String) {
        Log.d(TAG, "Loading images...")
        val userRef = FirebaseDatabase.getInstance().reference.child(userSex).child(userId)
        for (i in 0..3) loadSingleImage(userRef, i + 1)
    }

    private fun loadSingleImage(userRef : DatabaseReference, id: Int) {
        userRef.child("imageUrl_$id").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value != null) {
                    var img : ImageView? = null
                    img = when (id) {
                        1 -> image1
                        2 -> image2
                        3 -> image3
                        4 -> image4
                        else -> null
                    }
                    Log.d(TAG, "imageUrl_$id: ${snapshot.value}")
                    if (img != null)
                        Glide.with(mContext as ProfileCheckinMatched).load(snapshot.value).into(img)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkUserSex() {
        Log.d(TAG, "checkUserSex for $userId")
        var userSex: String
        val maleDb = FirebaseDatabase.getInstance().reference.child("male")
        maleDb.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.key == userId) {
                    userSex = "male"
                    Log.d(TAG, "user is male")
                    loadImages(userSex, userId)
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
                if (dataSnapshot.key == userId) {
                    userSex = "female"
                    Log.d(TAG, "user is female")
                    loadImages(userSex, userId)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    companion object {
        private const val TAG = "ProfileCheckinMatched"
    }
}