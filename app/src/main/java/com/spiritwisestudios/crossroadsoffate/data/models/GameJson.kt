package com.spiritwisestudios.crossroadsoffate.data.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Single shared Gson configuration for all game JSON (asset loading and Room
 * column serialization). Both GameRepository and Converters write/read the same
 * columns, so they must serialize identically.
 */
object GameJson {
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LeadsTo::class.java, LeadsToDeserializer())
        .serializeNulls()
        .create()
}
