package com.example.nephroticsyndrome.LoginAndSignup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthComplete: () -> Unit
) {
    val patientAuth by authViewModel.userAuth.collectAsState()
    val isSignUp by authViewModel.isSignUp.collectAsState()
    val isInvalid by authViewModel.isInvalid.collectAsState()
    val isProcComplete by authViewModel.isProcComplete.collectAsState()
    val isPt by authViewModel.isPt.collectAsState()

    if (isProcComplete) {
        onAuthComplete()
        authViewModel.resetProcComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (isSignUp) "Create User Account" else "Log In",
            color = Color.Black,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        TextField(
            value = patientAuth.email,
            onValueChange = {
                authViewModel.EmailChange(it)
            },
            label = { Text("Email") }
        )

        TextField(
            value = patientAuth.password,
            onValueChange = {
                authViewModel.PasswordChange(it)
            },
            label = { Text("Password") }
        )

        if (isSignUp) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Choose One")
                Text("Doctor")
                Checkbox(
                    checked = !isPt,
                    onCheckedChange = { authViewModel.onUserTypeChange() }
                )
                Text("Patient")
                Checkbox(
                    checked = isPt,
                    onCheckedChange = { authViewModel.onUserTypeChange() }
                )
            }
            //Name
            TextField(
                value = patientAuth.user.name,
                onValueChange = {
                    authViewModel.onNameChange(it)
                },
                label = { Text("Name") }
            )
            //Age
            TextField(
                value = patientAuth.user.age.toString(),
                onValueChange = {
                    if (it.isDigitsOnly() && it.isNotEmpty()) {
                        authViewModel.onAgeChange(it.toInt())
                    } else if (it.isEmpty()) {
                        authViewModel.onAgeChange(0)
                    }
                },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            //sex
            TextField(
                value = patientAuth.user.sex,
                onValueChange = { authViewModel.onSexChange(it) },
                label = { Text("Sex") }
            )
        }

        Button(
            onClick = {
                if (isSignUp) {
                    authViewModel.signUpPatient()
                } else {
                    authViewModel.signInPatient()
                }
            }
        ) {
            Text(
                if (isSignUp) "Sign Up" else "Sign In",
                color = Color.White
            )
        }

        if (isInvalid) {
            Text("Invalid Credentials. Try Again", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            if (isSignUp) "Already have an account?" else "Don't have an account?",
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (isSignUp) {
            Text(
                "Click here to Log In",
                modifier = Modifier.clickable { authViewModel.switchToSignIn() },
                color = Color.Blue
            )
        } else {
            Text(
                "Click Here to Sign up",
                modifier = Modifier.clickable { authViewModel.switchToSignUp() },
                color = Color.Blue
            )
        }
    }
}