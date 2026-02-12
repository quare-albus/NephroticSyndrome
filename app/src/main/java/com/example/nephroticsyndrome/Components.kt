package com.example.nephroticsyndrome

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nephroticsyndrome.DataModel.User
import com.example.nephroticsyndrome.DataModel.PatientRecord

@Composable
fun PatientRecordScreen(
    user: User,
    records: List<PatientRecord>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PatientInfoCard(user)
        Spacer(modifier = Modifier.height(16.dp))
        PatientTable(records)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topBar(scrollBehavior: TopAppBarScrollBehavior, settingFunction: () -> Unit){
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(
                "DipDiary",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    onAddEntry: (PatientRecord) -> Unit = { }
) {
    var date by remember { mutableStateOf("02/02/2026") }
    var time by remember { mutableStateOf("05:07 PM") }
    var urinePrtn by remember { mutableStateOf("Neg") }
    var medication by remember { mutableStateOf("") }
    var otherProblems by remember { mutableStateOf("") }

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

            OutlinedTextField(
                value = medication,
                onValueChange = { medication = it },
                label = { Text("Medication") },
                placeholder = { Text("Name and dose") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
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
            onClick = { onAddEntry(PatientRecord(date,time,urinePrtn, medication,otherProblems)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add entry")
        }
    }
}

// Dialog box for the entry form
@Composable
fun EntryDialog(onDismissRequest: () -> Unit, onNewRecordEntry: (PatientRecord) -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            NewEntryForm {patientRecord ->
                onNewRecordEntry(patientRecord)
            }
        }
    }
}

//Side Navigation Bar for sign out and user account delete functionality
@Composable
fun NavDrawerContent(signOut :() -> Unit = {}, deleteAccount :() -> Unit = {}) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Settings",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge
            )

            HorizontalDivider()

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
    PtList: List<User>
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
            PtCard(Pt)
        }
    }
}

@Composable
fun PtCard(
    user: User
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
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
fun DoctorRecordScreen() {
    val ptList = listOf(
        User("Ethan Singh", 55, "Male"),
        User("Mia Garcia", 56, "Male"),
        User("Mia Garcia", 56, "Male"),
        User("Mia Garcia", 56, "Male"),
        User("Mia Garcia", 56, "Male"),
        User("Mia Garcia", 56, "Male"),
        User("Mia Garcia", 56, "Male"),
        User("Mia Garcia", 56, "Male")
    )

    PtLazyMatrix(ptList)
}