package com.example.nephroticsyndrome.DataModel

data class User (
    val name: String,
    val age: Int,
    val sex: String,
    val uid: String = "",
    val userType: UserType = UserType.Unknown,
    val userCode: String = ""
)