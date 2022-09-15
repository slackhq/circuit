package com.slack.circuit.sample.home

import com.slack.circuit.Screen
import com.slack.circuit.sample.petlist.PetListScreen

sealed class BottomNavItem(val title: String, val screen: Screen){
    object Dogs : BottomNavItem("Dogs", PetListScreen)
    object Cats: BottomNavItem("Cats", PetListScreen)
}
