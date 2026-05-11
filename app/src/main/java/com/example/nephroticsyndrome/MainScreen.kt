package com.example.nephroticsyndrome

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.nephroticsyndrome.DataModel.User
import com.example.nephroticsyndrome.DataModel.UserType
import com.example.nephroticsyndrome.LoginAndSignup.AuthScreen
import com.example.nephroticsyndrome.LoginAndSignup.AuthViewModel
import com.example.nephroticsyndrome.ui.LoadingScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel
) {
    val auth: FirebaseAuth = Firebase.auth
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val isLoading by mainViewModel.isLoading.collectAsState()

    if (currentUser == null) {
        AuthScreen(
            authViewModel = authViewModel,
            onAuthComplete = {
                currentUser = auth.currentUser
                mainViewModel.getUserInfo()
            }
        )
        return
    }

    if (isLoading) {
        LoadingScreen()
        return
    }

    // Navigation Drawer State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Doctor screen navigation state
    var doctorScreen by remember { mutableStateOf("Home") }

    // Dialogbox visibility variables
    var showEntryDialog by remember { mutableStateOf(false) }
    var showChooseDoctorDialog by remember { mutableStateOf(false) }

    // TopAppBar scroll behaviour variable
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val userInfo by mainViewModel.userInfo.collectAsState()
    val medications by mainViewModel.medications.collectAsState()
    val patientRecords by mainViewModel.patientRecords.collectAsState()
    val selectedPatientRecords by mainViewModel.selectedPatientRecords.collectAsState()
    val pendingRequests by mainViewModel.pendingRequests.collectAsState()
    val pendingRequestsInfo by mainViewModel.pendingRequestsInfo.collectAsState()
    val approvedPatientsInfo by mainViewModel.approvedPatientsInfo.collectAsState()

    var selectedPatient by remember { mutableStateOf<User?>(null) }

    ModalNavigationDrawer(
        drawerContent = {
            NavDrawerContent(
                userType = userInfo.userType,
                signOut = {
                    mainViewModel.signOut {
                        currentUser = null
                    }
                },
                deleteAccount = {
                    mainViewModel.deleteUser {
                        currentUser = null
                    }
                },
                onChooseDoctor = {
                    scope.launch {
                        drawerState.close()
                        showChooseDoctorDialog = true
                    }
                },
                onHomeClick = {
                    scope.launch {
                        doctorScreen = "Home"
                        selectedPatient = null
                        drawerState.close()
                    }
                },
                onPendingRequestsClick = {
                    scope.launch {
                        doctorScreen = "PendingRequests"
                        selectedPatient = null
                        drawerState.close()
                    }
                }
            )
        },
        drawerState = drawerState
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                if (userInfo.userType == UserType.Patient) {
                    FloatingActionButton(
                        onClick = { showEntryDialog = true },
                        containerColor = Color.Black
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }
            },
            topBar = {
                TopBar(userInfo, scrollBehavior) {
                    scope.launch {
                        if (drawerState.isClosed) {
                            drawerState.open()
                        } else {
                            drawerState.close()
                        }
                    }
                }
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (userInfo.userType == UserType.Patient) {
                    PatientRecordScreen(
                        userInfo,
                        patientRecords
                    )
                } else {
                    if (doctorScreen == "Home") {
                        if (selectedPatient != null) {
                            PatientRecordScreen(
                                user = selectedPatient!!,
                                records = selectedPatientRecords,
                                onBack = { selectedPatient = null }
                            )
                        } else {
                            DoctorRecordScreen(
                                user = userInfo,
                                ptList = approvedPatientsInfo,
                                onPatientClick = { patient ->
                                    mainViewModel.fetchSelectedPatientRecords(patient.uid)
                                    selectedPatient = patient
                                }
                            )
                        }
                    } else {
                        PendingRequestsScreen(
                            pendingRequests = pendingRequestsInfo,
                            onApprove = { mainViewModel.approvePatient(it) }
                        )
                    }
                }
            }
        }
    }

    if (showEntryDialog) {
        EntryDialog(
            onDismissRequest = { showEntryDialog = false },
            medications = medications
        ) { patientRecord ->
            mainViewModel.AddPatientRecord(patientRecord)
            showEntryDialog = false
            Log.d("Debug Entry Patient Record", patientRecord.toString())
        }
    }

    if (showChooseDoctorDialog) {
        ChooseDoctorDialog(
            onDismiss = { showChooseDoctorDialog = false },
            onSubmit = { doctorCode ->
                mainViewModel.requestDoctor(doctorCode)
                showChooseDoctorDialog = false
            }
        )
    }
}
