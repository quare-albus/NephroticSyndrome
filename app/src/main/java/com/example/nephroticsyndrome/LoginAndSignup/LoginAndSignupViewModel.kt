package com.example.nephroticsyndrome.LoginAndSignup

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.nephroticsyndrome.DataModel.User
import com.example.nephroticsyndrome.DataModel.UserType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class LoginAndSignupViewModel : ViewModel() {
    private val _isPt = MutableStateFlow<Boolean>(true)
    val isPt: StateFlow<Boolean> = _isPt

    private val _userAuth = MutableStateFlow<PatientAuth>(PatientAuth("","", User("",0,"","",if (isPt.value) UserType.Patient else UserType.Doctor)))
    val userAuth: StateFlow<PatientAuth> = _userAuth

    private val _isInvalid = MutableStateFlow<Boolean>(false)
    val isInvalid: StateFlow<Boolean> = _isInvalid

    private val _isProcComplete = MutableStateFlow<Boolean>(false)
    val isProcComplete: StateFlow<Boolean> = _isProcComplete

    private val _isSignUp = MutableStateFlow<Boolean>(false)
    val isSignUp: StateFlow<Boolean> = _isSignUp

    fun EmailChange(email: String){
        _userAuth.value = _userAuth.value.copy(email = email)
    }

    fun PasswordChange(password: String){
        _userAuth.value = _userAuth.value.copy(password = password)
    }

    fun onAgeChange(age: Int){
        _userAuth.value = _userAuth.value.copy(
            user = _userAuth.value.user.copy(
                age = age
            )
        )
    }

    fun onNameChange(name : String){
        _userAuth.value = _userAuth.value.copy(
            user = _userAuth.value.user.copy(
                name = name
            )
        )
    }

    fun onSexChange(sex : String){
        _userAuth.value = _userAuth.value.copy(
            user = _userAuth.value.user.copy(
                sex = sex
            )
        )
    }

    fun onUserTypeChange(){
        _isPt.value = !_isPt.value
    }

    fun signInPatient() {

         Firebase.auth.signInWithEmailAndPassword(userAuth.value.email, userAuth.value.password)
                               .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInUserWithEmail:success")
                        _isProcComplete.update { true }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInUserWithEmail:failure", task.exception)
                        _isInvalid.update { true }
                    }
                }

    }

    fun signUpPatient() {
        Firebase.auth.createUserWithEmailAndPassword(userAuth.value.email, userAuth.value.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("SignUp", "createUserWithEmail:success")
                    addNewPatient(userAuth.value, Firebase.auth.currentUser?.uid ?: "")
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("SignUp", "createUserWithEmail:failure", task.exception)
                    _isInvalid.update { true }
                }
            }
    }

    fun addNewPatient(patientAuth: PatientAuth, uid: String){
        //creating hashmap
        val patientInfoHash = hashMapOf(
            "name" to patientAuth.user.name,
            "age" to patientAuth.user.age,
            "sex" to patientAuth.user.sex,
            "userType" to patientAuth.user.userType.toString()
        )

        //setting up instance
        val db = Firebase.firestore

        //adding hashmap to database
        db.collection("UserRegister")
            .document(uid)
            .set(patientInfoHash)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added")
                _isProcComplete.update { true }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    fun switchToSignUp() {
        _isSignUp.update { true }
    }
    fun switchToSignIn() {
        _isSignUp.update { false }
    }

}