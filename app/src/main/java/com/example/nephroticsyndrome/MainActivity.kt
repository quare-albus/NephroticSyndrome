package com.example.nephroticsyndrome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.nephroticsyndrome.DataModel.UserType
import com.example.nephroticsyndrome.LoginAndSignup.LogInActivity
import com.example.nephroticsyndrome.ui.theme.NephroticSyndromeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    val mainViewModel: MainViewModel by viewModels()

    // Firebase auth variable
    private lateinit var auth: FirebaseAuth

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            //Initializing the auth variable
            auth = Firebase.auth
            //Navigation Drawer State
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            //Checking if current user is signed in
            val currentUser = auth.currentUser
            if (currentUser == null) {
                startActivity(Intent(this, LogInActivity::class.java))
            }

            NephroticSyndromeTheme {
                //dialogbox visibility variable
                var showEntryDialog by remember { mutableStateOf(false) }
                //topAppBar scroll behaviour variable
                val scrollBehavior =
                    TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

                ModalNavigationDrawer(
                    drawerContent = {
                        NavDrawerContent(
                            signOut = {
                                mainViewModel.signOut {
                                    startActivity(Intent(this, LogInActivity::class.java))
                                }
                            },
                            deleteAccount = {
                                mainViewModel.deleteUser {
                                    startActivity(Intent(this, LogInActivity::class.java))
                                }
                            }
                        )
                    },
                    drawerState = drawerState
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { showEntryDialog = true },
                                containerColor = Color.Black
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add")
                            }
                        },
                        topBar = {
                            topBar(scrollBehavior){
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
                        if (mainViewModel.userInfo.collectAsState().value.userType == UserType.Patient) {
                            PatientRecordScreen(
                                mainViewModel.userInfo.collectAsState().value,
                                mainViewModel.patientRecords.collectAsState().value,
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            DoctorRecordScreen()
                        }
                    }

                }

                if (showEntryDialog) {
                    EntryDialog({ showEntryDialog = false }) { patientRecord ->
                        mainViewModel.AddPatientRecord(patientRecord)
                        showEntryDialog = false
                        Log.d("Debug Entry Patient Record #65Main", patientRecord.toString())
                    }
                }
            }
        }
    }
}
