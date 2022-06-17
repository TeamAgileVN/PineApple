package com.zellycookies.pineapple.utility.adapter

import android.util.Log

class RankList {
    var list : MutableList<RankObject> = ArrayList()

    fun add(rankObject: RankObject) {
        list.add(rankObject)
    }

    fun size(): Int {
        return list.size
    }

    fun sort() {
        list = quickSort(list.toList()).toMutableList()
        Log.d(TAG_RANK , "-----------------------------")
        for (i in 0 until list.size) {
            Log.d(TAG_RANK, "$i - ${list[i].getUser()!!.user_id} : ${list[i].getLikeCount()}")
        }
    }

    private fun quickSort(list: List<RankObject>) : List<RankObject> {
        if (list.count() < 2){
            return list
        }
        val pivot = list[list.count()/2]

        val equal = list.filter { it.getLikeCount() == pivot.getLikeCount() }

        val less = list.filter { it.getLikeCount() < pivot.getLikeCount() }

        val greater = list.filter { it.getLikeCount() > pivot.getLikeCount() }

        return quickSort(less) + equal + quickSort(greater)
    }

    companion object {
        private const val TAG_RANK = "RankList"
    }
}