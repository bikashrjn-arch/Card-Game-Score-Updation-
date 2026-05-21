package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class GameTypeConverters {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val roundListType = Types.newParameterizedType(List::class.java, GameRound::class.java)

    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    private val roundListAdapter = moshi.adapter<List<GameRound>>(roundListType)

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return stringListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return stringListAdapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun fromRoundList(value: List<GameRound>?): String {
        return roundListAdapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toRoundList(value: String?): List<GameRound> {
        if (value.isNullOrEmpty()) return emptyList()
        return roundListAdapter.fromJson(value) ?: emptyList()
    }
}
