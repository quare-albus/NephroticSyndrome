package com.example.nephroticsyndrome

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.nephroticsyndrome.DataModel.User
import com.example.nephroticsyndrome.DataModel.PatientRecord
import com.example.nephroticsyndrome.DataModel.Medication
import com.example.nephroticsyndrome.DataModel.UserType
import com.example.nephroticsyndrome.DataModel.toUserType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.BufferedReader
import java.io.InputStreamReader

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _patientRecords = MutableStateFlow<List<PatientRecord>>(emptyList())
    val patientRecords: StateFlow<List<PatientRecord>> = _patientRecords.asStateFlow()

    private val _userInfo = MutableStateFlow(User("Kiddo", 18, "Female", ""))
    val userInfo: StateFlow<User> = _userInfo.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<String>>(emptyList())
    val pendingRequests: StateFlow<List<String>> = _pendingRequests.asStateFlow()

    private val _pendingRequestsInfo = MutableStateFlow<List<User>>(emptyList())
    val pendingRequestsInfo: StateFlow<List<User>> = _pendingRequestsInfo.asStateFlow()

    private var lastPendingUids: List<String> = emptyList()

    private val _approvedPatientsInfo = MutableStateFlow<List<User>>(emptyList())
    val approvedPatientsInfo: StateFlow<List<User>> = _approvedPatientsInfo.asStateFlow()

    private val _selectedPatientRecords = MutableStateFlow<List<PatientRecord>>(emptyList())
    val selectedPatientRecords: StateFlow<List<PatientRecord>> = _selectedPatientRecords.asStateFlow()

    private var lastApprovedPatientCodes: List<String> = emptyList()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


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
        loadMedications()
    }

    private fun loadMedications() {
        val medicationsList = mutableListOf<Medication>()
        try {
            val inputStream = getApplication<Application>().resources.openRawResource(R.raw.medications)
            val reader = BufferedReader(InputStreamReader(inputStream))
            // Skip header
            reader.readLine()
            var line: String? = reader.readLine()
            while (line != null) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    medicationsList.add(Medication(parts[0], parts[1]))
                }
                line = reader.readLine()
            }
            _medications.value = medicationsList
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading medications from CSV", e)
        }
    }

    fun getUserInfo() {
        val currentUser = Firebase.auth.currentUser ?: return
        val uid = currentUser.uid
        _isLoading.update { true }
        //setting up instance
        val db = Firebase.firestore
        Log.d("Patient Id", uid)
        //getting patient info from database directly by uid
        db.collection("UserRegister")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(
                        "Debug Get Patient Info #65Main",
                        "DocumentSnapshot data: ${document.data}"
                    )
                    val userCode = document.data?.get("userCode")?.toString() ?: ""
                    if (document.data?.get("name") != null) {
                        _userInfo.update {
                            User(
                                name = document.data?.get("name").toString(),
                                age = document.data?.get("age").toString().toInt(),
                                sex = document.data?.get("sex").toString(),
                                uid = uid,
                                userType = document.data?.get("userType").toString().toUserType(),
                                userCode = userCode
                            )
                        }
                    }

                    if (userInfo.value.userType == UserType.Doctor) {
                        getPendingRequests(uid)
                    } else {
                        getPatientRecords(uid)
                    }

                } else {
                    Log.d("Debug Get Patient Info #65Main", "No such document")
                    _isLoading.update { false }
                }
            }
            .addOnFailureListener {
                _isLoading.update { false }
            }
    }

    private fun getPatientRecords(uid: String) {
        //setting up instance
        val db = Firebase.firestore

        //getting patient records from database using uid as document ID
        db.collection("UserRegister")
            .document(uid)
            .collection("Records")
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val records = mutableListOf<PatientRecord>()
                    for (document in documents) {
                        Log.d("Debug Get Patient Records #65Main", "${document.id} => ${document.data}")
                        records.add(
                            PatientRecord(
                                date = document.data["date"].toString(),
                                time = document.data["time"].toString(),
                                urineProtein = document.data["urineProtein"].toString(),
                                medication = document.data["medication"].toString(),
                                symptoms = document.data["symptoms"].toString()
                            )
                        )
                    }
                    _patientRecords.value = records
                } catch (e: Exception) {
                    Log.d("Debug Get Patient Records #65Main", "Error getting documents.", e)
                } finally {
                    _isLoading.update { false }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Debug Get Patient Records #65Main", "Error getting documents.", exception)
                _isLoading.update { false }
            }
    }

    private fun getPendingRequests(doctorUid: String) {
        val db = Firebase.firestore
        db.collection("UserRegister")
            .document(doctorUid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("PendingRequests", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val requests = snapshot.get("pendingRequests") as? List<String> ?: emptyList()
                    _pendingRequests.value = requests
                    fetchPendingRequestsInfo(requests)

                    val approved = snapshot.get("approvedPatients") as? List<String> ?: emptyList()
                    fetchApprovedPatientsInfo(approved)
                }
                _isLoading.update { false }
            }
    }

    private fun fetchPendingRequestsInfo(uids: List<String>) {
        if (uids == lastPendingUids) return
        lastPendingUids = uids

        if (uids.isEmpty()) {
            _pendingRequestsInfo.value = emptyList()
            return
        }

        val db = Firebase.firestore
        db.collection("UserRegister")
            .whereIn(FieldPath.documentId(), uids)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val users = querySnapshot.documents.map { doc ->
                    User(
                        name = doc.getString("name") ?: "",
                        age = doc.getLong("age")?.toInt() ?: 0,
                        sex = doc.getString("sex") ?: "",
                        uid = doc.id,
                        userType = doc.getString("userType").toString().toUserType(),
                        userCode = doc.getString("userCode") ?: ""
                    )
                }
                _pendingRequestsInfo.value = users
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "Error fetching pending requests info", e)
            }
    }

    private fun fetchApprovedPatientsInfo(patientUids: List<String>) {
        if (patientUids == lastApprovedPatientCodes) return
        lastApprovedPatientCodes = patientUids

        if (patientUids.isEmpty()) {
            _approvedPatientsInfo.value = emptyList()
            return
        }

        val db = Firebase.firestore
        // Note: whereIn is limited to 10 items
        db.collection("UserRegister")
            .whereIn(FieldPath.documentId(), patientUids)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val users = querySnapshot.documents.map { doc ->
                    User(
                        name = doc.getString("name") ?: "",
                        age = doc.getLong("age")?.toInt() ?: 0,
                        sex = doc.getString("sex") ?: "",
                        uid = doc.id,
                        userType = doc.getString("userType").toString().toUserType(),
                        userCode = doc.getString("userCode") ?: ""
                    )
                }
                _approvedPatientsInfo.value = users
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "Error fetching approved patients info", e)
            }
    }

    fun fetchSelectedPatientRecords(uid: String) {
        val db = Firebase.firestore
        db.collection("UserRegister")
            .document(uid)
            .collection("Records")
            .get()
            .addOnSuccessListener { documents ->
                val records = documents.map { document ->
                    PatientRecord(
                        date = document.data["date"].toString(),
                        time = document.data["time"].toString(),
                        urineProtein = document.data["urineProtein"].toString(),
                        medication = document.data["medication"].toString(),
                        symptoms = document.data["symptoms"].toString()
                    )
                }
                _selectedPatientRecords.value = records
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "Error fetching selected patient records", e)
            }
    }

    fun approvePatient(patientUid: String) {
        val db = Firebase.firestore
        val doctorUid = _userInfo.value.uid

        if (doctorUid.isEmpty()) return

        // Add to approvedPatients array and remove from pendingRequests array
        db.collection("UserRegister")
            .document(doctorUid)
            .update(
                "approvedPatients", FieldValue.arrayUnion(patientUid),
                "pendingRequests", FieldValue.arrayRemove(patientUid)
            )
            .addOnSuccessListener {
                Log.d("ApprovePatient", "Patient $patientUid approved and removed from pending")
            }
            .addOnFailureListener { e ->
                Log.e("ApprovePatient", "Error approving patient", e)
            }
    }

    // Adds the patient Record To Db
    fun AddPatientRecord(patientRecord: PatientRecord) {
        // update local record
        _patientRecords.update { old ->
            old + patientRecord
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
        val user = Firebase.auth.currentUser
        if (user != null) {
            user.delete().addOnCompleteListener {
                redirect()
            }
        } else {
            redirect()
        }
    }

    fun requestDoctor(doctorCode: String) {
        val db = Firebase.firestore
        val patientUid = _userInfo.value.uid

        if (patientUid.isEmpty()) {
            Log.e("RequestDoctor", "Patient UID is empty")
            return
        }

        // Search for doctor by userCode field
        db.collection("UserRegister")
            .whereEqualTo("userCode", doctorCode)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doctorDocument = querySnapshot.documents[0]
                    val doctorUid = doctorDocument.id

                    if (doctorDocument.getString("userType") == UserType.Doctor.toString()) {
                        // Add patient to doctor's pendingRequests array
                        db.collection("UserRegister")
                            .document(doctorUid)
                            .update("pendingRequests", FieldValue.arrayUnion(patientUid))
                            .addOnSuccessListener {
                                Log.d("RequestDoctor", "Successfully sent request to doctor $doctorCode ($doctorUid)")
                            }
                            .addOnFailureListener { e ->
                                Log.e("RequestDoctor", "Failed to send request", e)
                            }
                    } else {
                        Log.e("RequestDoctor", "User found but not a doctor")
                    }
                } else {
                    Log.e("RequestDoctor", "Doctor not found with code: $doctorCode")
                }
            }
            .addOnFailureListener { e ->
                Log.e("RequestDoctor", "Error finding doctor", e)
            }
    }
}


