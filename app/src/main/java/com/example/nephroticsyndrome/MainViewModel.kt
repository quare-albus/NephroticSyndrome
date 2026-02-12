package com.example.nephroticsyndrome

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.nephroticsyndrome.DataModel.User
import com.example.nephroticsyndrome.DataModel.PatientRecord
import com.example.nephroticsyndrome.DataModel.UserType
import com.example.nephroticsyndrome.DataModel.toUserType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    private val _patientRecords = MutableStateFlow(mutableListOf<PatientRecord>())
    val patientRecords: StateFlow<List<PatientRecord>> = _patientRecords.asStateFlow()

    private val _userInfo = MutableStateFlow(User("Kiddo", 18, "Female", ""))
    val userInfo: StateFlow<User> = _userInfo.asStateFlow()


    init {
        _patientRecords.value += listOf(
            PatientRecord("2026-02-01", "15:33", "Neg", "", ""),
            PatientRecord(
                "2026-02-01",
                "23:29",
                "1+",
                "Atorvastatin 20mg",
                "Slight swelling in ankles"
            ),
            PatientRecord(
                "2026-01-30",
                "14:12",
                "Trace",
                "Lisinopril 10mg",
                "Slight swelling in ankles"
            ),
            PatientRecord(
                "2026-01-29",
                "23:38",
                "3+",
                "Lisinopril 10mg",
                "Slight swelling in ankles"
            )
        )

        //update patient Info
        getUserInfo()

    }

    private fun getUserInfo() {
        //setting up instance
        val db = Firebase.firestore
        val uid = Firebase.auth.currentUser!!.uid
        Log.d("Patient Id", uid)
        //getting patient info from database
        db.collection("UserRegister")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(
                        "Debug Get Patient Info #65Main",
                        "DocumentSnapshot data: ${document.data}"
                    )
                    if (document.data?.get("name") != null) {
                        _userInfo.update {
                            User(
                                name = document.data?.get("name").toString(),
                                age = document.data?.get("age").toString().toInt(),
                                sex = document.data?.get("sex").toString(),
                                uid = uid,
                                userType = document.data?.get("userType").toString().toUserType()
                            )
                        }
                    }

                    getPatientRecords()

                } else {
                    Log.d("Debug Get Patient Info #65Main", "No such document")
                }
            }
    }

    private fun getPatientRecords() {
        //setting up instance
        val db = Firebase.firestore
        val uid = _userInfo.value.uid

        //getting patient records from database
        db.collection("UserRegister")
            .document(uid)
            .collection("Records")
            .get()
            .addOnSuccessListener{documents ->
            try {
                _patientRecords.value = mutableListOf()
                for (document in documents) {
                    Log.d("Debug Get Patient Records #65Main", "${document.id} => ${document.data}")
                    _patientRecords.update { old ->
                        (old + PatientRecord(
                            date = document.data["date"].toString(),
                            time = document.data["time"].toString(),
                            urineProtein = document.data["urineProtein"].toString(),
                            medication = document.data["medication"].toString(),
                            symptoms = document.data["symptoms"].toString()
                        )) as MutableList<PatientRecord>
                    }
                }
            } catch (e: Exception) {
                Log.d("Debug Get Patient Records #65Main", "Error getting documents.", e)
            }
        }
            .addOnFailureListener { exception ->
                Log.w("Debug Get Patient Records #65Main", "Error getting documents.", exception)
            }
    }

    // Adds the patient Record To Db
    fun AddPatientRecord(patientRecord: PatientRecord) {
        // update local record
        _patientRecords.update { old ->
            (old + patientRecord) as MutableList<PatientRecord>
        }

        //update in firestore
        //setting up instance
        val db = Firebase.firestore

        //creating the hash map
        val patientRecordHash = hashMapOf(
            "date" to patientRecord.date,
            "time" to patientRecord.time,
            "urineProtein" to patientRecord.urineProtein,
            "medication" to patientRecord.medication,
            "symptoms" to patientRecord.symptoms
        )

        //adding the hash map to the database
        db.collection("UserRegister")
            .document(_userInfo.value.uid)
            .collection("Records")
            .add(patientRecordHash)
            .addOnSuccessListener { documentReference ->
                Log.d(
                    "Debug Add Patient Record #65Main",
                    "DocumentSnapshot added with ID: ${documentReference.id}"
                )
            }
            .addOnFailureListener { e ->
                Log.w("Debug Add Patient Record #65Main", "Error adding document", e)
            }
    }

    //signs out user and redirects to sign in page with the redirect() set to the sign in Page
    fun signOut(redirect: () -> Unit) {
        Firebase.auth.signOut()
        //redirect redirects to the sign in page after signing out
        redirect()
    }

    //deletes user and redirects to sign in page with the redirect() set to the sign in Page
    fun deleteUser(redirect: () -> Unit) {
        Firebase.auth.currentUser!!.delete()
        // redirect() redirects to the sign in page after deleting the user
        redirect()
    }
}


