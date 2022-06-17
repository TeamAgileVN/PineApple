package com.zellycookies.pineapple.utility

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lorentzos.flingswipe.SwipeFlingAdapterView
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.introduction.IntroductionMain
import com.zellycookies.pineapple.main.*
import com.zellycookies.pineapple.utility.adapter.RankList
import com.zellycookies.pineapple.utility.adapter.RankObject
import com.zellycookies.pineapple.utils.CalculateAge
import com.zellycookies.pineapple.utils.GPS
import com.zellycookies.pineapple.utils.User

class UtilityTopPicksKOL : AppCompatActivity() {
    private val myPermissionRequestLocation = 123
    private val mContext: Context = this@UtilityTopPicksKOL
    private var userSex: String? = null
    private var lookForSex: String? = null
    private var latitude = 37.349642
    private var longitude = -121.938987
    private var currentUID: String? = null
    private var movies = false
    private var music = false
    private var art = false
    private var food = false
    private var mNotificationHelper: NotificationHelper? = null
    private var arrayAdapter: PhotoAdapter? = null
    var listView: ListView? = null
    var rowItems: MutableList<Cards>? = null
    var cardCache: MutableList<Cards>? = null
    var gps: GPS? = null
    private var thisUserId : String? = null
    private var flingContainer : SwipeFlingAdapterView? = null

    //firebase
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var usersDb: DatabaseReference? = null
    private var mFirebaseFirestore: FirebaseFirestore? = null

    //filter
    private var maxAge: Int = 100
    private var minAge: Int = 16
    private var distancePreference: Int = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_utility_top_picks_kol)
        createNotificationChannel()
        usersDb = FirebaseDatabase.getInstance().reference
        mFirebaseFirestore = FirebaseFirestore.getInstance()
        mNotificationHelper = NotificationHelper(this)
        setupFirebaseAuth()
        checkUserSex()
        rowItems = ArrayList()
        cardCache = ArrayList()
        checkCardCache()
        arrayAdapter = PhotoAdapter(this, R.layout.item, rowItems as ArrayList<Cards>)
        updateSwipeCard()
        initUI()
    }

    private fun findFilter(dataSnapshot: DataSnapshot) {
        distancePreference = dataSnapshot.getValue(User::class.java)!!.preferDistance!!
        minAge = dataSnapshot.getValue(User::class.java)!!.preferMinAge!!
        maxAge = dataSnapshot.getValue(User::class.java)!!.preferMaxAge!!
    }

    private fun checkCardCache() {
        val intent = intent
        val isRewindActivity = intent.getBooleanExtra("isRewindActivity", false)
        if (!isRewindActivity) {
            Log.d(TAG, "checkCardCache: False")
            return
        }

        Log.d(TAG, "checkCardCache: True")

        val listUserId = intent.getStringArrayListExtra("listUserId")
        val listName = intent.getStringArrayListExtra("listName")
        val listDoB = intent.getStringArrayListExtra("listDoB")
        val listAge = intent.getIntegerArrayListExtra("listAge")
        val listUrl = intent.getStringArrayListExtra("listUrl")
        val listBio = intent.getStringArrayListExtra("listBio")
        val listInterest = intent.getStringArrayListExtra("listInterest")
        val listDistance = intent.getIntegerArrayListExtra("listDistance")
        val listShowDoB = intent.getStringArrayListExtra("listShowDoB")
        val listShowDistance = intent.getStringArrayListExtra("listShowDistance")
        for (i in 0 until listUserId!!.size) {
            val item = Cards(listUserId[i], listName!![i], listDoB!![i],
                listAge!![i], listUrl!![i], listBio!![i],
                listInterest!![i], listDistance!![i],
                listShowDoB!![i].toBoolean(), listShowDistance!![i].toBoolean())
            cardCache!!.add(cardCache!!.size, item)
        }
    }

    private fun initUI() {
        val btnSwipeLeft = findViewById<FloatingActionButton>(R.id.dislikebtn)
        val btnSwipeRight = findViewById<FloatingActionButton>(R.id.likebtn)
        val btnRewind = findViewById<FloatingActionButton>(R.id.rewindbtn)

        btnSwipeLeft.setOnClickListener { dislikeBtn() }
        btnSwipeRight.setOnClickListener { likeBtn() }
        btnRewind.setOnClickListener { rewindBtn() }

        val btnReturn = findViewById<View>(R.id.back) as ImageButton
        btnReturn.setOnClickListener { onReturn() }
        val toolbar = findViewById<TextView>(R.id.toolbartag)
        toolbar.text = getString(R.string.kol)
    }

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                myPermissionRequestLocation
            )
        } else {
            gps = GPS(this)
            val location = gps!!.mlocation
            // made the changes to set location only if location object is not null, else default location is taken
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
            }
            val userLoc: MutableMap<Any, Any> = mutableMapOf()
            userLoc["latitude"] = latitude
            userLoc["longitude"] = longitude
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            myPermissionRequestLocation -> {
                run {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            updateLocation()
                        } else {
                            Toast.makeText(
                                mContext,
                                "Location Permission Denied. You have to give permission inorder to know the user range ",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun updateSwipeCard() {
        flingContainer = findViewById<View>(R.id.frame) as SwipeFlingAdapterView
        flingContainer!!.adapter = arrayAdapter
        flingContainer!!.setFlingListener(object : SwipeFlingAdapterView.onFlingListener {
            override fun removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                Log.d("LIST", "removed object!")

                rowItems!!.removeAt(0)

                arrayAdapter?.notifyDataSetChanged()
            }

            override fun onLeftCardExit(dataObject: Any) {
                val obj = dataObject as Cards
                val userId = obj.userId
                usersDb!!.child(lookForSex!!).child(userId).child("connections").child("dislikeme")
                    .child(
                        currentUID!!
                    ).setValue(true)
                cardCache!!.add(0, obj)
                Log.d(TAG, "Added ${obj.name} to cache")
                UtilityHistoryActivity.uploadActivity(userSex!!, thisUserId!!, "You disliked ${obj.name}")
            }

            override fun onRightCardExit(dataObject: Any) {
                val obj = dataObject as Cards
                val userId = obj.userId
                usersDb!!.child(lookForSex!!).child(userId).child("connections").child("dislikeme")
                    .child(
                        currentUID!!
                    ).setValue(null)
                usersDb!!.child(lookForSex!!).child(userId).child("connections").child("likeme")
                    .child(
                        currentUID!!
                    ).setValue(true)
                UtilityHistoryActivity.uploadActivity(userSex!!, thisUserId!!, "You liked ${obj.name}")

                //check matches
                isConnectionMatch(userId, obj.name!!)
            }

            override fun onAdapterAboutToEmpty(itemsInAdapter: Int) {
                // Ask for more data here
            }

            override fun onScroll(scrollProgressPercent: Float) {
                val view = flingContainer!!.selectedView
                view.findViewById<View>(R.id.item_swipe_right_indicator).alpha = (if (scrollProgressPercent < 0) (-scrollProgressPercent) else 0.0F)
                view.findViewById<View>(R.id.item_swipe_left_indicator).alpha = (if (scrollProgressPercent > 0) scrollProgressPercent else 0.0F)
            }
        })
    }

    private fun isConnectionMatch(userId: String, username: String) {
        val currentUserConnectionsDb = usersDb!!.child(userSex!!).child(currentUID!!).child("connections").child("likeme").child(userId)
        currentUserConnectionsDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    //prompt user that match
                    //later change to notification
                    sendNotification()
                    usersDb!!.child(lookForSex!!).child(dataSnapshot.key!!).child("connections")
                        .child("match_result").child(
                            currentUID!!
                        ).setValue(true)
                    usersDb!!.child(userSex!!).child(currentUID!!).child("connections")
                        .child("match_result").child(
                            dataSnapshot.key!!
                        ).setValue(true)
                    UtilityHistoryActivity.uploadActivity(userSex!!, thisUserId!!, "You have matched with ${username}!")

                    // Create group in Realtime DB and Cloud DB
                    val key = mFirebaseFirestore!!.collection("group").document().id
                    val groupInfoDb = mFirebaseFirestore!!.collection("group").document(key)
                    val idUserList = ArrayList<String?>()
                    idUserList.add(dataSnapshot.key)
                    idUserList.add(currentUID)
                    val newGroupMap = HashMap<String, String>()
                    newGroupMap["idGroup"] = key
                    groupInfoDb.set(newGroupMap)
                    groupInfoDb.update("members", idUserList)
                    groupInfoDb.update("createTime", FieldValue.serverTimestamp())
                    usersDb!!.child(lookForSex!!).child(dataSnapshot.key!!).child("group").child(
                        currentUID!!
                    ).setValue(key)
                    usersDb!!.child(userSex!!).child(currentUID!!).child("group")
                        .child(dataSnapshot.key!!).setValue(key)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun sendNotification() {
        /*val nb = mNotificationHelper!!.getChannel1Notification(
            mContext.getString(R.string.app_name),
            mContext.getString(R.string.match_notification)
        )
        mNotificationHelper!!.manager!!.notify(1, nb.build())*/

        createNotificationChannel()

        val builder = NotificationCompat.Builder(this, "channel_match")
            .setSmallIcon(R.drawable.ic_baseline_notifications_24)
            .setContentTitle("PineApple")
            .setContentText("You've got a new match!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(0, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Matching Channel"
            val descriptionContent = "A channel for matching"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_match", name, importance).apply {
                description = descriptionContent
            }
            val notificationManager : NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun onReturn() {
        startActivity(Intent(mContext, UtilityTopPicksActivity::class.java))
        finish()
    }

    /**
     * check the user sex
     */
    fun checkUserSex() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            currentUID = user.uid
            val maleDb = FirebaseDatabase.getInstance().reference.child("male")
            maleDb.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    if (dataSnapshot.key == currentUID) {
                        Log.d(
                            TAG,
                            "onChildAdded: the sex is $userSex"
                        )
                        userSex = "male"
                        //updateLocation
                        updateLocation()
                        lookForSex = dataSnapshot.getValue(
                            User::class.java
                        )!!.preferSex
                        findInterest(dataSnapshot)
                        findFilter(dataSnapshot)

                        potentialMatch
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
                    if (dataSnapshot.key == currentUID) {
                        Log.d(
                            TAG,
                            "onChildAdded: the sex is $userSex"
                        )
                        userSex = "female"
                        //update location
                        updateLocation()
                        lookForSex = dataSnapshot.getValue(
                            User::class.java
                        )!!.preferSex
                        findInterest(dataSnapshot)

                        findFilter(dataSnapshot)

                        potentialMatch
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    private fun findInterest(dataSnapshot: DataSnapshot) {
        movies = dataSnapshot.getValue(User::class.java)!!.isHobby_movies
        music = dataSnapshot.getValue(User::class.java)!!.isHobby_music
        food = dataSnapshot.getValue(User::class.java)!!.isHobby_food
        art = dataSnapshot.getValue(User::class.java)!!.isHobby_art
    }

    val potentialMatch: Unit
        get() {
            val potentialMatch = FirebaseDatabase.getInstance().reference.child(
                lookForSex!!
            )
            val rankList = RankList()
            potentialMatch.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    if (dataSnapshot.exists() && !dataSnapshot.child("connections")
                            .child("dislikeme").hasChild(
                                currentUID!!
                            ) && !dataSnapshot.child("connections").child("likeme")
                            .hasChild(currentUID!!) && dataSnapshot.key != currentUID
                    ) {
                        val curUser = dataSnapshot.getValue(
                            User::class.java
                        )
                        val rankObject = RankObject(curUser!!, 0, dataSnapshot)
                        rankList.add(rankObject)
                        checkKOL(rankObject, rankList)
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }

    private fun checkKOL(rankObject: RankObject, rankList : RankList) {
        val user = rankObject.getUser()!!
        val likeMeRef = FirebaseDatabase.getInstance().reference.child(user.sex!!).child(user.user_id!!)
            .child("connections").child("likeme")
        Log.d(TAG_RANK, "checkKOL : ${user.user_id}")
        likeMeRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.exists()) {
                    Log.d(TAG_RANK, "${user.user_id}: ${snapshot.key} exists.")
                    rankObject.upLikeCount()
                    Log.d(TAG_RANK, "-----------------------------")
                    Log.d(TAG_RANK, "${rankObject.getUser()!!.user_id}")
                    rankList.sort()
                    rowItems!!.clear()
                    for (i in rankList.list) {
                        addToRowItems(i.getUser()!!, i.getSnapshot())
                    }
                    rowItems!!.reverse()
                } else {
                    Log.d(TAG_RANK, "${user.user_id}: ${snapshot.key} doesn't exist.")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG_RANK, "onCancelled")
            }
        })
    }

    private fun addToRowItems(curUser : User, dataSnapshot: DataSnapshot) {
        val tempFood = curUser.isHobby_food
        val tempMusic = curUser.isHobby_music
        val tempArt = curUser.isHobby_art
        val tempMovies = curUser.isHobby_movies
        val showDoB = curUser.isShowDoB
        val showDistance = curUser.isShowDistance
        if (tempMusic == music || tempArt == art || tempFood == food || tempMovies == movies) {
            //calculate age
            val dob = curUser.dateOfBirth
            val cal = CalculateAge(dob!!)
            val age = cal.age

            //initialize card view
            //check profile image first
            var profileImageUrl =
                if (lookForSex == "female") "defaultFemale" else "defaultMale"
            if (dataSnapshot.child("profileImageUrl").value != null) {
                profileImageUrl =
                    dataSnapshot.child("profileImageUrl").value.toString()
            }
            val username = curUser.username
            val bio = curUser.description
            val interest = StringBuilder()
            if (tempMovies) {
                interest.append("Movies   ")
            }
            if (tempMusic) {
                interest.append("Music   ")
            }
            if (tempArt) {
                interest.append("Art   ")
            }
            if (tempFood) {
                interest.append("Food   ")
            }

            //calculate distance
            // gps = GPS(mContext)
            Log.d(TAG, "onChildAdded: the x, y of user is $latitude, $longitude")
            Log.d(TAG, "onChildAdded: the other user x y is " + curUser.latitude + ", " + curUser.longtitude)
            val distance =
                if (gps != null) gps!!.calculateDistance(latitude, longitude,
                    curUser.latitude,  curUser.longtitude) else 0
            Log.d(TAG, "distance is $distance")

            if (age in minAge..maxAge && distance <= distancePreference) {
                val item = Cards(
                    dataSnapshot.key!!,
                    username,
                    dob,
                    age,
                    profileImageUrl,
                    bio,
                    interest.toString(),
                    distance,
                    showDoB,
                    showDistance
                )
                rowItems!!.add(item)
                arrayAdapter?.notifyDataSetChanged()
            }
        }
    }

    //Dislike Button = Swipe left
    private fun dislikeBtn() {
        if (rowItems!!.size != 0) {
            val cardItem = rowItems!![0]
            val userId = cardItem.userId
            usersDb!!.child(lookForSex!!).child(userId).child("connections").child("dislikeme")
                .child(
                    currentUID!!
                ).setValue(true)

            val removedCard = rowItems!!.removeAt(0)
            cardCache!!.add(0, removedCard)
            Log.d(TAG, "Added ${removedCard.name} to cache")

            UtilityHistoryActivity.uploadActivity(userSex!!, thisUserId!!, "You disliked ${cardItem.name}")
            arrayAdapter?.notifyDataSetChanged()
            val btnClick = Intent(mContext, BtnDislikeActivity::class.java)
            btnClick.putExtra("url", cardItem.profileImageUrl)
            startActivity(btnClick)
        }
    }

    //Like Button = Swipe right
    private fun likeBtn() {
        if (rowItems!!.size != 0) {
            val cardItem = rowItems!![0]
            val userId = cardItem.userId
            usersDb!!.child(lookForSex!!).child(userId).child("connections").child("likeme").child(
                currentUID!!
            ).setValue(true)
            UtilityHistoryActivity.uploadActivity(userSex!!, thisUserId!!, "You liked ${cardItem.name}")

            //check matches
            isConnectionMatch(userId, cardItem.name!!)

            rowItems!!.removeAt(0)

            arrayAdapter?.notifyDataSetChanged()
            val btnClick = Intent(mContext, BtnLikeActivity::class.java)
            btnClick.putExtra("url", cardItem.profileImageUrl)
            startActivity(btnClick)
        }
    }

    private fun rewindBtn() {
        if (cardCache!!.size == 0) {
            Log.d(TAG, "Card Cache is empty")
            return
        }
        val card = cardCache!!.removeAt(0)
        usersDb!!.child(lookForSex!!).child(card.userId).child("connections").child("dislikeme")
            .child(
                currentUID!!
            ).setValue(null)
        Log.d(TAG, "Rewind ${card.name}")
        transferCache()
    }

    private fun transferCache() {
        Log.d(TAG, "Transferring cache...")
        if (cardCache!!.size == 0) {
            val intent = Intent(this, UtilityTopPicksKOL::class.java)
            intent.putExtra("isRewindActivity", false)
            startActivity(intent)
            finish()
            return
        }
        val listUserId : MutableList<String> = ArrayList()
        val listName : MutableList<String> = ArrayList()
        val listDoB : MutableList<String> = ArrayList()
        val listAge : MutableList<Int> = ArrayList()
        val listUrl : MutableList<String> = ArrayList()
        val listBio : MutableList<String> = ArrayList()
        val listInterest : MutableList<String> = ArrayList()
        val listDistance : MutableList<Int> = ArrayList()
        val listShowDoB : MutableList<String> = ArrayList()
        val listShowDistance : MutableList<String> = ArrayList()
        for (card in cardCache!!) {
            listUserId.add(card.userId)
            listName.add(card.name!!)
            listDoB.add(card.dob!!)
            listAge.add(card.age)
            listUrl.add(card.profileImageUrl)
            listBio.add(card.bio!!)
            listInterest.add(card.interest)
            listDistance.add(card.distance)
            listShowDoB.add(card.showDoB.toString())
            listShowDistance.add(card.showDistance.toString())
        }
        val intent = Intent(this, UtilityTopPicksKOL::class.java)
        intent.putExtra("isRewindActivity", true)
        intent.putStringArrayListExtra("listUserId", ArrayList(listUserId))
        intent.putStringArrayListExtra("listName", ArrayList(listName))
        intent.putStringArrayListExtra("listDoB", ArrayList(listDoB))
        intent.putIntegerArrayListExtra("listAge", ArrayList(listAge))
        intent.putStringArrayListExtra("listUrl", ArrayList(listUrl))
        intent.putStringArrayListExtra("listBio", ArrayList(listBio))
        intent.putStringArrayListExtra("listInterest", ArrayList(listInterest))
        intent.putIntegerArrayListExtra("listDistance", ArrayList(listDistance))
        intent.putStringArrayListExtra("listShowDoB", ArrayList(listShowDoB))
        intent.putStringArrayListExtra("listShowDistance", ArrayList(listShowDistance))
        startActivity(intent)
    }

    private fun checkCurrentUser(user: FirebaseUser?) {
        Log.d(TAG, "checkCurrentUser: checking if user is logged in")
        if (user == null) {
            val intent = Intent(mContext, IntroductionMain::class.java)
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
                thisUserId = user.uid
            } else {
                //user is signed out
                Log.d(TAG, "onAuthStateChanged: signed_out")
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        mAuth!!.addAuthStateListener(mAuthListener!!)
        checkCurrentUser(mAuth!!.currentUser)
    }

    public override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    companion object {
        private const val TAG = "UtilityTopPicksKOL"
        private const val TAG_RANK = "RankList"
    }
}