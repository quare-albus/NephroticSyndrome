package com.example.nephroticsyndrome.DataModel

sealed class UserType {
    object Patient : UserType()
    object Doctor : UserType()
    object Unknown : UserType()

    override fun toString(): String {
        when (this) {
            is Patient -> return "Patient"
            is Doctor -> return "Doctor"
            is Unknown -> return "Unknown"
        }
    }
}

fun String.toUserType(): UserType = when (this) {
    "Patient" -> UserType.Patient
    "Doctor" -> UserType.Doctor
    else -> UserType.Unknown
}

