package hu.bme.aut.sporttracker

data class User(
    var id: String,
    var notification: Boolean?,
    var goal: Int?,
    var age: Int?,
    var gender: String?,
    var height: Int?,
    var weight: Int?
)