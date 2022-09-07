//package com.slack.circuit.sample.bottomNavBar
//
//import android.os.Parcelable
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.Scaffold
//import androidx.compose.material.Text
//import androidx.compose.material3.CenterAlignedTopAppBar
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.testTag
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.unit.sp
//import com.slack.circuit.Navigator
//import com.slack.circuit.Presenter
//import com.slack.circuit.PresenterFactory
//import com.slack.circuit.Screen
//import com.slack.circuit.sample.R
//import com.slack.circuit.sample.di.AppScope
//import com.slack.circuit.sample.petlist.PetListGrid
//import com.slack.circuit.sample.petlist.PetListScreen
//import com.slack.circuit.sample.petlist.PetListTestConstants
//import com.squareup.anvil.annotations.ContributesMultibinding
//import dagger.assisted.Assisted
//import dagger.assisted.AssistedFactory
//import dagger.assisted.AssistedInject
//import kotlinx.coroutines.flow.Flow
//import kotlinx.parcelize.Parcelize
//import javax.inject.Inject
//
//
//@Parcelize
//object HomeScreen : Screen {
//    sealed interface State : Parcelable {
//        @Parcelize
//        object Loading : State
//        @Parcelize
//        object NoAnimals : State
//        @Parcelize
//        data class Success(val state: String) : State
//    }
//
//    sealed interface Event {
//        data class ClickScreen(val screen: Screen) : Event
//    }
//}
//
//@ContributesMultibinding(AppScope::class)
//class HomeScreenPresenterFactory
//@Inject
//constructor(private val homePresenterFactory: HomePresenter.Factory) : PresenterFactory {
//    override fun create(screen: Screen, navigator: Navigator): Presenter<*, *>? {
//        if (screen is HomeScreen) return homePresenterFactory.create(navigator)
//        return null
//    }
//}
//
//class HomePresenter
//@AssistedInject
//constructor(@Assisted private val navigator: Navigator,) : Presenter<HomeScreen.State, HomeScreen.Event> {
//    val bottomNav = BottomNav(navigator)
//
//    @Composable
//    override fun present(events : Flow<HomeScreen.Event>): HomeScreen.State {
//
//    }
//
//    // Events are NavBar click events?
//    // and State would be THe grid view im returning
//
//    @Composable
//    internal fun Home(state: PetListScreen.State, events: (PetListScreen.Event) -> Unit) {
//        Scaffold(
//            modifier = Modifier
//                .systemBarsPadding()
//                .fillMaxWidth(),
//            topBar = {
//                CenterAlignedTopAppBar(
//                    title = {
//                        Text("Adoptables", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
//                    },
//                    colors =
//                    TopAppBarDefaults.centerAlignedTopAppBarColors(
//                        containerColor = MaterialTheme.colorScheme.primaryContainer
//                    )
//                )
//            },
//            bottomBar = { bottomNav.BottomNavigationBar() },
//            content = { paddingValues ->
//                when (state) {
//                    PetListScreen.State.Loading ->
//                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                            CircularProgressIndicator(modifier = Modifier.testTag(PetListTestConstants.PROGRESS_TAG))
//                        }
//                    PetListScreen.State.NoAnimals ->
//                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                            Text(
//                                modifier = Modifier.testTag(PetListTestConstants.NO_ANIMALS_TAG),
//                                text = stringResource(id = R.string.no_animals)
//                            )
//                        }
//                    is PetListScreen.State.Success ->
//                        PetListGrid(
//                            modifier = Modifier
//                                .padding(paddingValues)
//                                .fillMaxSize(),
//                            animals = state.animals,
//                            events = events
//                        )
//                }
//            }
//        )
//    }
//
//    @Composable
//    fun Navigation(navigator: Navigator) {
//
//    }
//
//    @AssistedFactory
//    interface Factory {
//        fun create(navigator: Navigator): HomePresenter
//    }
//}