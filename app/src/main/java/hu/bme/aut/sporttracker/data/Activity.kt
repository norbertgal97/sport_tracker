package hu.bme.aut.sporttracker.data

data class Activity(
    var steps: Float?,
    var distance: Float?,
    var calories : Float?,
    var duration : Long?,
    var date: String
)