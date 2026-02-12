package com.example.nephroticsyndrome.LoginAndSignup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly

class LogInActivity : ComponentActivity() {

    val loginAndSignupViewModel: LoginAndSignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val patientAuth = loginAndSignupViewModel.userAuth.collectAsState().value
            val isSignUp = loginAndSignupViewModel.isSignUp.collectAsState().value

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isSignUp) "Create User Account" else "Log In",
                    color = Color.White,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(30.dp))

                TextField(
                    value = patientAuth.email,
                    onValueChange = {
                        loginAndSignupViewModel.EmailChange(it)
                    },
                    label = { Text("Email") }
                )



                TextField(
                    value = patientAuth.password,
                    onValueChange = {
                        loginAndSignupViewModel.PasswordChange(it)
                    },
                    label = { Text("Password") }
                )

                if (isSignUp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text("Choose One")
                        Text("Doctor")
                        Checkbox(
                            checked = !loginAndSignupViewModel.isPt.collectAsState().value,
                            onCheckedChange = { loginAndSignupViewModel.onUserTypeChange() }
                        )
                        Text("Patient")
                        Checkbox(
                            checked = loginAndSignupViewModel.isPt.collectAsState().value,
                            onCheckedChange = { loginAndSignupViewModel.onUserTypeChange() }
                        )
                    }
                    //Name
                    TextField(
                        value = patientAuth.user.name,
                        onValueChange = {
                            loginAndSignupViewModel.onNameChange(it)
                        },
                        label = { Text("Name") }

                    )
                    //Age
                    TextField(
                        value = patientAuth.user.age.toString(),
                        onValueChange = {
                            if (it.isDigitsOnly()) {
                                loginAndSignupViewModel.onAgeChange(it.toInt())
                            }
                        },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(
                            // Suggests a numeric keyboard on mobile devices
                            keyboardType = KeyboardType.NumberPassword
                        )
                    )
                    //sex
                    TextField(
                        value = patientAuth.user.sex,
                        onValueChange = { loginAndSignupViewModel.onSexChange(it) },
                        label = { Text("Sex") }
                    )
                }

                Button(
                    onClick = {
                        if (isSignUp) {
                            loginAndSignupViewModel.signUpPatient()
                        } else {
                            loginAndSignupViewModel.signInPatient()
                        }
                    }
                ) {
                    Text(
                        if (isSignUp) "Sign Up" else "Sign In",
                        color = Color.White
                    )
                }

                if (loginAndSignupViewModel.isInvalid.collectAsState().value) {
                    Text("Invalid Credentials. Try Again")
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    if (isSignUp) "Already have an account?" else "Don't have an account?",
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isSignUp) {
                    Text(
                        "Click here to Log In",
                        modifier = Modifier.clickable { loginAndSignupViewModel.switchToSignIn() },
                        color = Color.White
                    )
                } else {
                    Text(
                        "Click Here to Sign up",
                        modifier = Modifier.clickable { loginAndSignupViewModel.switchToSignUp() },
                        color = Color.White
                    )
                }

                if(loginAndSignupViewModel.isProcComplete.collectAsState().value) {
                    finish()
                }
            }
        }
    }
}
