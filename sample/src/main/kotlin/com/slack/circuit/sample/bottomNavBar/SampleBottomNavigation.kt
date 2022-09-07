package com.slack.circuit.sample.bottomNavBar

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
//import com.slack.circuit.Navigator
import com.slack.circuit.sample.R

class SampleBottomNavigation constructor(/*private val navigator: Navigator*/) {
    @Composable
    fun BottomNavigationBar() {
        // These are the buttons on the NavBar, they dictate where we navigate too.
        val items = listOf(BottomNavItem.Dogs, BottomNavItem.Cats)
        BottomNavigation(
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            items.forEach { item ->
                BottomNavigationItem(
                    icon = { Icon(painterResource(id = R.drawable.drago_dog), contentDescription = item.title, modifier = Modifier.scale(0.5f)) },
                    label = { Text(text = item.title) },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(0.4f),
                    alwaysShowLabel = true,
                    selected = false,
                    onClick = {
                        // HomeScreen.Event.ClickScreen(item.screen)
                        // navigator.goTo(item.screen)
                    }
                )
            }
        }
    }
}