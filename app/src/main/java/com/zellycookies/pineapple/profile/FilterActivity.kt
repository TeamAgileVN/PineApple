package com.zellycookies.pineapple.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.home.HomeSwipeActivity
import com.zellycookies.pineapple.login.Login
import com.zellycookies.pineapple.main.ProfileCheckinMain
import com.zellycookies.pineapple.matched.SafetyToolkitActivity
import com.zellycookies.pineapple.utility.UtilityHistoryActivity
import com.zellycookies.pineapple.utils.User


class FilterActivity : AppCompatActivity() {
    private val TAG = "SettingsActivity"
    private lateinit var distance: SeekBar
    private lateinit var rangeSeekBar: RangeSeekBar<*>
    private lateinit var genderText : TextView
    private lateinit var distanceText :TextView
    private lateinit var ageRange :TextView
    private lateinit var genderSpinner : Spinner
    private lateinit var submitButton : Button
    private var gender : String = "male"
    private var distanceInSeekBar : Int = 50
    private var maxAge : Int = 100
    private var minAge : Int = 16

    private var user : FirebaseUser? = null
    private var userId : String? = null
    private var userSex : String? = null
    private var userRef : DatabaseReference? = null
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)
        val toolbar = findViewById<TextView>(R.id.toolbartag)
        toolbar.text = "Filter"
        val back = findViewById<ImageButton>(R.id.back)
        genderText = findViewById(R.id.gender_text)
        distance = findViewById(R.id.distance)
        distanceText = findViewById<TextView>(R.id.distance_text)
        ageRange = findViewById<TextView>(R.id.age_range)
        rangeSeekBar = findViewById(R.id.rangeSeekbar)
        genderSpinner = findViewById(R.id.gender_spinner)
        submitButton = findViewById(R.id.submitButton)

        setupFirebaseAuth()
        init()

        val genderPreferences = resources.getStringArray(R.array.gender_preferences)
        if (genderSpinner != null) {
            val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, genderPreferences)
            genderSpinner.adapter = adapter

            genderSpinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    genderText.text = genderPreferences[position].toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }
        }

        distance.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                distanceText.text = "$progress Km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        rangeSeekBar.setOnRangeSeekBarChangeListener { bar, number, number2 ->
            ageRange.text = "$number - $number2"
            maxAge = number2.toInt()
            minAge = number.toInt()
        }

        submitButton.setOnClickListener {
            adjustFilter()
            onBackPressed()
        }

        back.setOnClickListener { onBackPressed() }
    }

    private fun init() {
        user = FirebaseAuth.getInstance().currentUser
        userId = user!!.uid

        checkUserSex()
    }

    private fun adjustFilter() {
        userRef!!.child("preferSex").setValue(genderText.text.toString().lowercase())
        userRef!!.child("preferDistance").setValue(distance.progress)
        userRef!!.child("preferMinAge").setValue(minAge)
        userRef!!.child("preferMaxAge").setValue(maxAge)

        UtilityHistoryActivity.uploadActivity(userSex!!, userId!!, "You adjusted Filter list!")
    }

    private fun setupFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
            } else {
                val intent = Intent(this@FilterActivity, Login::class.java)

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun initUserRef() {
        userRef = FirebaseDatabase.getInstance().reference.child(userSex!!).child(userId!!)
    }

    private fun findFilter(dataSnapshot: DataSnapshot) {
        gender = dataSnapshot.getValue(User::class.java)!!.preferSex!!
        distanceInSeekBar = dataSnapshot.getValue(User::class.java)!!.preferDistance!!
        minAge = dataSnapshot.getValue(User::class.java)!!.preferMinAge!!
        maxAge = dataSnapshot.getValue(User::class.java)!!.preferMaxAge!!
    }

    private fun initFilter() {
        if (gender.compareTo("male") == 0) {
            genderSpinner.setSelection(0)
            distance.progress = distanceInSeekBar
            rangeSeekBar.selectedMaxValue = maxAge
            rangeSeekBar.selectedMinValue = minAge
            ageRange.text = "$minAge - $maxAge"
        }
        else if (gender.compareTo("female") == 0) {
            genderSpinner.setSelection(1)
            distance.progress = distanceInSeekBar
            rangeSeekBar.selectedMaxValue = maxAge
            rangeSeekBar.selectedMinValue = minAge
            ageRange.text = "$minAge - $maxAge"
        }
    }

    private fun checkUserSex() {
        if (user != null) {
            val maleDb = FirebaseDatabase.getInstance().reference.child("male")
            maleDb.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    if (dataSnapshot.key == userId) {
                        userSex = "male"
                        initUserRef()
                        findFilter(dataSnapshot)

                        initFilter()
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
                        initUserRef()
                        findFilter(dataSnapshot)

                        initFilter()
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }
}