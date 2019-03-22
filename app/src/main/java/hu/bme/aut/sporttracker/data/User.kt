package hu.bme.aut.sporttracker.data

data class User(
    var id: String,
    var notificationSwitch: Boolean?,
    var goal: Int?,
    var age: Int?,
    var gender: String?,
    var height: Int?,
    var weight: Int?
)