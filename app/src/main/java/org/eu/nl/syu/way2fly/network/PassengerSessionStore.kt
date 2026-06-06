package org.eu.nl.syu.way2fly.network

import android.content.Context

object PassengerSessionStore {
    private const val PREFS_NAME = "passenger_session"
    private const val KEY_PHONE_NUMBER = "phone_number"
    private const val KEY_PASSENGER_TOKEN = "passenger_token"

    fun save(context: Context, phoneNumber: String, passengerToken: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHONE_NUMBER, phoneNumber)
            .putString(KEY_PASSENGER_TOKEN, passengerToken)
            .apply()
    }

    fun getPhoneNumber(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PHONE_NUMBER, null)
    }

    fun getPassengerToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASSENGER_TOKEN, null)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}