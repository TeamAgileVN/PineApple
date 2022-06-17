package com.zellycookies.pineapple.utility.adapter

import android.provider.ContactsContract
import com.google.firebase.database.DataSnapshot
import com.zellycookies.pineapple.utils.User

class RankObject(user: User, private var likeCount: Int, private var dataSnapshot: DataSnapshot) {
    private var user : User? = user

    fun getUser(): User? { return user }
    fun getLikeCount(): Int { return likeCount }
    fun getSnapshot(): DataSnapshot { return dataSnapshot}
    fun upLikeCount() { likeCount++ }

}