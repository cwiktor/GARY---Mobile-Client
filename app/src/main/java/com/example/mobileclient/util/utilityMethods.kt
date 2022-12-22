package com.example.mobileclient.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mobileclient.model.Schedule
import java.time.LocalDate

fun checkPermission(permission: String, requestCode: Int, context: Context, activity: Activity) {
    if (ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_DENIED
    ) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    } else {
        Log.d("Permission", "$permission already granted")
    }
}

fun setAllergyTypeFromApi(allergyType: String, allergyTypesArray: Array<String>): String {
    return when (allergyType) {
        "SKIN_CONTACT" -> allergyTypesArray[0]
        "INGESTION" -> allergyTypesArray[1]
        "INJECTION" -> allergyTypesArray[2]
        "INHALATION" -> allergyTypesArray[3]
        else -> {
            ""
        }
    }
}

fun setAllergyTypeToApi(allergyType: String, allergyTypesArray: Array<String>): String {
    return when (allergyType) {
        allergyTypesArray[0] -> "SKIN_CONTACT"
        allergyTypesArray[1] -> "INGESTION"
        allergyTypesArray[2] -> "INJECTION"
        allergyTypesArray[3] -> "INHALATION"
        else -> {
            ""
        }
    }
}

fun findNextShift(schedule: Schedule): List<String> {
    val today = LocalDate.now().dayOfWeek.toString()
    val nearestShift: ArrayList<String> = ArrayList()
    Log.d("simple name", schedule.THURSDAY?.javaClass?.simpleName.toString())
    when {
        schedule.MONDAY?.javaClass?.simpleName.toString().equals(today,true) -> {
            nearestShift.add(schedule.MONDAY!!.start!!)
            nearestShift.add(schedule.MONDAY!!.end!!)
        }
        schedule.TUESDAY?.javaClass?.simpleName.toString().equals(today,true) -> {
            nearestShift.add(schedule.TUESDAY!!.start!!)
            nearestShift.add(schedule.TUESDAY!!.end!!)
        }
        schedule.WEDNESDAY?.javaClass?.simpleName.toString().equals(today,true) -> {
            nearestShift.add(schedule.WEDNESDAY!!.start!!)
            nearestShift.add(schedule.WEDNESDAY!!.end!!)
        }
        schedule.THURSDAY?.javaClass?.simpleName.toString().equals(today,true) -> {
            nearestShift.add(schedule.THURSDAY!!.start!!)
            nearestShift.add(schedule.THURSDAY!!.end!!)
        }
        schedule.FRIDAY?.javaClass?.simpleName.toString().equals(today,true) -> {
            nearestShift.add(schedule.FRIDAY!!.start!!)
            nearestShift.add(schedule.FRIDAY!!.end!!)
        }
        else -> {
            Log.d("Schedule", "No shift today or no matching day")
        }
    }
    Log.d("Schedule", nearestShift.toString())
    return nearestShift.toList()
}