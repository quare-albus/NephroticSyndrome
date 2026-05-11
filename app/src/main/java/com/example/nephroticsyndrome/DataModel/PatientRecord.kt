package com.example.nephroticsyndrome.DataModel

data class PatientRecord (
    val date: String,
    val time: String,
    val urineProtein: String,
    val medication: String,
    val symptoms: String
)

data class Medication(
    val name: String,
    val dosage: String
) {
    val displayName: String get() = "$name ($dosage)"
}