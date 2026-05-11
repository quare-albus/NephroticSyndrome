package com.example.nephroticsyndrome

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nephroticsyndrome.DataModel.Medication
import com.example.nephroticsyndrome.DataModel.UserType
import com.example.nephroticsyndrome.DataModel.User
import com.example.nephroticsyndrome.DataModel.PatientRecord

@Composable
fun PatientRecordScreen(
    user: User,
    records: List<PatientRecord>,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Patients")
            }
        }
        PatientInfoCard(user)
        Spacer(modifier = Modifier.height(16.dp))
        PatientTable(records)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(user: User, scrollBehavior: TopAppBarScrollBehavior, settingFunction: () -> Unit) {
    val title = if (user.userType == UserType.Doctor) {
        "Dr. ${user.name} (${user.userCode})"
    } else {
        "DipDiary"
    }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = { settingFunction() }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        actions = {
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Localized description"
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun PatientInfoCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoField(
                label = "Patient name",
                value = user.name,
                modifier = Modifier.weight(1.5f)
            )
            InfoField(
                label = "Age",
                value = user.age.toString(),
                modifier = Modifier.weight(1f)
            )
            InfoField(
                label = "Sex",
                value = user.sex,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InfoField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PatientTableHeader() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp),
    ) {
        TableHeaderCell("Date", 1f)
        TableHeaderCell("Time", 1f)
        TableHeaderCell("Urine prtn", 1f)
        TableHeaderCell("Medication", 2f)
        TableHeaderCell("Other problems", 2.5f)
    }
}

@Composable
fun TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.width((70*weight).dp),
    )
}

@Composable
fun PatientTable(records: List<PatientRecord>) {
    LazyColumn(
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        item { PatientTableHeader() }

        items(records){record ->
            PatientTableRow(record)
        }
    }
}

@Composable
fun PatientTableRow(record: PatientRecord) {
    Row(
        modifier = Modifier
            .padding(vertical = 10.dp)
    ) {
        TableCell(record.date, 1f)
        TableCell(record.time, 1f)
        TableCell(record.urineProtein, 1f)
        TableCell(record.medication, 2f)
        TableCell(record.symptoms, 2.5f)
    }
    Spacer(Modifier.height(5.dp))
}

// Table cell composable
@Composable
fun TableCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.width((70*weight).dp),
        style = MaterialTheme.typography.bodySmall
    )
}


// Entry form for collecting the patients records
@Composable
fun NewEntryForm(
    modifier: Modifier = Modifier,
    medications: List<Medication> = emptyList(),
    onAddEntry: (PatientRecord) -> Unit = { }
) {
    var date by remember { mutableStateOf("02/02/2026") }
    var time by remember { mutableStateOf("05:07 PM") }
    var urinePrtn by remember { mutableStateOf("Neg") }
    var medicationSearch by remember { mutableStateOf("") }
    var selectedMedications by remember { mutableStateOf(setOf<String>()) }
    var otherProblems by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    val filteredMedications = medications.filter {
        it.displayName.contains(medicationSearch, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "New entry",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Time") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = urinePrtn,
                onValueChange = { urinePrtn = it },
                label = { Text("Urine prtn") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = medicationSearch,
                    onValueChange = {
                        medicationSearch = it
                        expanded = true
                    },
                    label = { Text("Add Medication") },
                    placeholder = { Text("Name and dose") },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filteredMedications.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion.displayName) },
                            onClick = {
                                selectedMedications = selectedMedications + suggestion.displayName
                                medicationSearch = ""
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        if (selectedMedications.isNotEmpty()) {
            Text(
                text = "Selected Medications:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                selectedMedications.forEach { med ->
                    AssistChip(
                        onClick = { },
                        label = { Text(med, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { selectedMedications = selectedMedications - med }
                            )
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = otherProblems,
            onValueChange = { otherProblems = it },
            label = { Text("Other problems") },
            placeholder = { Text("Notes, symptoms, etc.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onAddEntry(
                    PatientRecord(
                        date,
                        time,
                        urinePrtn,
                        selectedMedications.joinToString(", "),
                        otherProblems
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add entry")
        }
    }
}

// Dialog box for the entry form
@Composable
fun EntryDialog(
    onDismissRequest: () -> Unit,
    medications: List<Medication> = emptyList(),
    onNewRecordEntry: (PatientRecord) -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                NewEntryForm(medications = medications) { patientRecord ->
                    onNewRecordEntry(patientRecord)
                }
            }
        }
    }
}

//Side Navigation Bar for sign out and user account delete functionality
@Composable
fun ChooseDoctorDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var doctorCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Doctor") },
        text = {
            Column {
                Text("Enter the 10-digit alphanumeric code of your doctor:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = doctorCode,
                    onValueChange = {
                        if (it.length <= 10) doctorCode = it.uppercase()
                    },
                    label = { Text("Doctor Code") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (doctorCode.length == 10) {
                        onSubmit(doctorCode)
                    }
                },
                enabled = doctorCode.length == 10
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NavDrawerContent(
    userType: UserType = UserType.Patient,
    signOut: () -> Unit = {},
    deleteAccount: () -> Unit = {},
    onChooseDoctor: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onPendingRequestsClick: () -> Unit = {}
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Menu",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge
            )

            HorizontalDivider()

            if (userType == UserType.Doctor) {
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = false,
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = { onHomeClick() }
                )
                NavigationDrawerItem(
                    label = { Text("Pending Requests") },
                    selected = false,
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = { onPendingRequestsClick() }
                )
            }

            if (userType == UserType.Patient) {
                NavigationDrawerItem(
                    label = { Text("Choose Doctor") },
                    selected = false,
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = { onChooseDoctor() }
                )
            }

            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                "Settings",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall
            )

            NavigationDrawerItem(
                label = { Text("Sign Out", color = Color.Red) },
                selected = false,
                onClick = { signOut() }
            )
            NavigationDrawerItem(
                label = { Text("Delete Account") },
                selected = false,
                onClick = { deleteAccount() }
            )


            NavigationDrawerItem(
                label = { Text("Settings") },
                selected = false,
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                badge = { Text("20") }, // Placeholder
                onClick = { /* Handle click */ }
            )
            NavigationDrawerItem(
                label = { Text("Help and feedback") },
                selected = false,
                icon = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null) },
                onClick = { /* Handle click */ },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

//doctor screen

@Composable
fun PtLazyMatrix(
    PtList: List<User>,
    onPatientClick: (User) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(PtList) { Pt ->
            PtCard(Pt, onClick = { onPatientClick(Pt) })
        }
    }
}

@Composable
fun PtCard(
    user: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${user.sex} · Age ${user.age}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DoctorRecordScreen(
    user: User,
    ptList: List<User>,
    onPatientClick: (User) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Welcome, Dr. ${user.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your Doctor Code: ${user.userCode}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text(
            text = "Your Patients",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        PtLazyMatrix(ptList, onPatientClick)
    }
}

@Composable
fun PendingRequestsScreen(
    pendingRequests: List<User>,
    onApprove: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Pending Patient Requests",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Log.d("pendingRequests", pendingRequests.toString())

        if (pendingRequests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No pending requests",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pendingRequests) { patient ->
                    PendingRequestItem(patient, onApprove)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PendingRequestItem(user: User, onApprove: (String) -> Unit) {
    Log.d("PendingRequestItem", user.uid)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${user.sex} · Age ${user.age}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = { onApprove(user.uid) }) {
                Text("Approve")
            }
        }
    }
}
