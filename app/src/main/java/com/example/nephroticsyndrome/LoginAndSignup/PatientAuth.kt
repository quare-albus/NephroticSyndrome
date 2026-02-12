package com.example.nephroticsyndrome.LoginAndSignup

import com.example.nephroticsyndrome.DataModel.User

data class PatientAuth(
    val email: String = "",
    val password: String = "",
    val user: User = User("",0,"","")
)
