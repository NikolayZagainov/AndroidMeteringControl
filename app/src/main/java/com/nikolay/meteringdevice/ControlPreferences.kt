package com.nikolay.meteringdevice

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.view.children
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.ArrayList

object ControlPreferences {
    var allRPMvalues: MutableList<String>? = null

    fun saveRPMsettings(activity: MainActivity)
    {
        allRPMvalues = arrayListOf()
        activity.lt_rpm_values?.children?.forEach {
            if(it is RPMEntry)
            {
                allRPMvalues?.add(it.rpm_fild.text.toString())
                // Log.i("initActions", it.rpm_fild.text.toString())
            }
        }
        saveArrayList(allRPMvalues as ArrayList<String?>, "rpms", activity)
    }

    fun getRPMsettings(activity: MainActivity)
    {
        val theVal: ArrayList<String?>? = getArrayList("rpms", activity)
        if(theVal != null)
            allRPMvalues =  theVal as MutableList<String>
    }

    fun loadRPMSettinngs(activity: MainActivity)
    {
        getRPMsettings(activity)
        allRPMvalues?.forEach {
            val rpm_entry = RPMEntry(activity)
            rpm_entry.rpm_fild.setText(it.toString())
            activity.lt_rpm_values?.addView(rpm_entry, activity.lt_rpm_values!!.childCount - 1)
            Log.i("initActions", it.toString())
        }
    }

    fun saveArrayList(list: ArrayList<String?>?, key: String?, context: Context) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = prefs.edit()
        val gson = Gson()
        val json: String = gson.toJson(list)
        editor.putString(key, json)
        editor.apply()
    }

    fun getArrayList(key: String?, context: Context): ArrayList<String?>? {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val gson = Gson()
        var json: String? = prefs.getString(key, null)
        val type: Type = object : TypeToken<ArrayList<String?>?>() {}.getType()
        return gson.fromJson(json, type)
    }
}