Shared Elements Tutorial
========================

This tutorial will help in setting up Compose Shared Elements transitions in a Circuit app. Doing so we then be able support a shared element transitioning between Circuit `Screen`s.


!!! info
    It is recommended that you have an understanding of Compose Shared Elements. Please review the [official documentation](https://developer.android.com/develop/ui/compose/animation/shared-elements) for an in-depth look at how shared elements work.


## Setup

In this tutorial we will setup the [simple email app](https://slackhq.github.io/circuit/tutorial/) from the tutorial with following shared transitions between the Inbox and Detail screens:

- A shared element transition of the email sender image
- A shared bounds transition of the email sender name
- A shared bounds transition of the email subject
- A shared bounds transition of the email body

### 1: Wrap your content with `SharedElementTransitionLayout`

To start we need to integrate the `SharedElementTransitionLayout` into the project, as `SharedElementTransitionLayout` is the root layout required for shared element transitions. It creates and provides a `SharedElementTransitionScope` which is required to use the core shared elements APIs.

Modify the apps main entry point to wrap `NavigableCircuitContent` with a `SharedElementTransitionLayout`

```kotlin title="Add SharedElementTransitionLayout" hl_lines="4 6"
val backStack = rememberSaveableBackStack(InboxScreen)
val navigator = rememberCircuitNavigator(backStack)
CircuitCompositionLocals(circuit) { 
  SharedElementTransitionLayout {
    NavigableCircuitContent(navigator = navigator, backStack = backStack) 
  }
}
```


### 2. Accessing the `SharedTransitionScope`

In order to use the `sharedElement()` and `sharedBounds()` compose `Modifier`s we now need to access the `SharedTransitionScope` that is created by the root `SharedElementTransitionLayout`.

To do this we will wrap the specific Composable Ui where we want to use `Modifier.sharedElement()`. For our shared elements we need to access the `Modifier`s in a few places in the `EmailItem` Ui. To do so we simply wrap the whole content with a `SharedElementTransitionScope`.


```kotlin title="ui.kt" hl_lines="2 5"
/** A simple email item to show in a list. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EmailItem(email: Email, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
  SharedElementTransitionScope {
    // ..
  }
```

The same can be done with `EmailDetailContent` so the matching elements can be setup.

```kotlin title="ui.kt" hl_lines="1 3"
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EmailDetailContent(email: Email, modifier: Modifier = Modifier) = SharedElementTransitionScope {
  Column(modifier.padding(16.dp)) {
    // ..
```

The `SharedElementTransitionScope` can be placed anywhere in the Ui tree. It simply provides access to the underlying `SharedTransitionScope` so that the shared element modifiers are visible. This approach means you don't have to worry about passing the scope as a context receiver or with a `CompositionLocal`. 


### 3. (Optional) Create shared element keys

Now we can start using Compose shared elements across the Inbox and Detail screens. We're going to add shared elements to transition the email sender, email title, and email body.

It is recommended to create a unique key to safely match the shared elements against each other. 

!!! info
    Using the Circuit `SharedTransitionKey` is optional, it is simply a marker type that can be helpful when grouping shared transition keys.



```kotlin title="SharedTransitionKeys.kt"
import com.slack.circuit.sharedelements.SharedTransitionKey

data class EmailSharedTransitionKey(val id: String, val type: ElementType) : SharedTransitionKey {
  enum class ElementType {
    SenderImage,
    SenderName,
    Subject,
    Body,
  }
}
```

### 4. Adding a `Modifier.sharedElement()` 


Now we can add `Modifier.sharedElement()` to the modifier chains of two matching Composable's. We will start with the sender's image, which can be found in the `EmailItem` on the Inbox screen, and the `EmailDetailContent` in the Detail screen.


```kotlin title="EmailItem" hl_lines="10-20"
fun EmailItem(email: Email, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
  SharedElementTransitionScope {
    Row(
      modifier.clickable(onClick = onClick).padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Image(
        Icons.Default.Person,
        modifier =
          Modifier.sharedElement(
              state =
                rememberSharedContentState(
                  EmailSharedTransitionKey(
                    id = email.id,
                    type = EmailSharedTransitionKey.ElementType.SenderImage,
                  )
                ),
              animatedVisibilityScope =
                requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            )
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Magenta)
            .padding(4.dp),
        colorFilter = ColorFilter.tint(Color.White),
        contentDescription = null,
      )
    // ...
```

If you created the `EmailSharedTransitionKey` you can use it here as the key for the `rememberSharedContentState`.

```kotlin title="EmailDetailContent" hl_lines="7-17"
fun EmailDetailContent(email: Email, modifier: Modifier = Modifier) = SharedElementTransitionScope {
  Column(modifier.padding(16.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Image(
        Icons.Default.Person,
        modifier =
          Modifier.sharedElement(
              state =
                rememberSharedContentState(
                  EmailSharedTransitionKey(
                    id = email.id,
                    type = EmailSharedTransitionKey.ElementType.SenderImage,
                  )
                ),
              animatedVisibilityScope =
                requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            )
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Magenta)
            .padding(4.dp),
        colorFilter = ColorFilter.tint(Color.White),
        contentDescription = null,
      )
    // ...
```

There is one crucial part of this we haven't covered yet, and that is accessing the `AnimatedVisibilityScope`. Shared elements require and `AnimatedVisibilityScope` to work, and we are using the `requireAnimatedScope` method on the `SharedElementTransitionScope` to get one.

```kotlin title="EmailItem" hl_lines="10-11"
        modifier =
          Modifier.sharedElement(
              state =
                rememberSharedContentState(
                  EmailSharedTransitionKey(
                    id = email.id,
                    type = EmailSharedTransitionKey.ElementType.SenderImage,
                  )
                ),
              animatedVisibilityScope =
                requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            )
```

The call to `requireAnimatedScope` is accessing a `AnimatedVisibilityScope` that is used in Circuit navigation. Circuit provides a few scopes by default which are detailed in the shared elements [documentation](shared-elements.md#animatedscope).

=== "Android"
    <div markdown>
    <video style="float: left; margin-right: 0.8em;" width="400" controls="true" autoplay="true" loop="true" src="../videos/shared-elements-tutorial-step-4-android.mp4" ></video>

    With that we now have a shared element transition where the sender image transitions across the two screens!
    </div>


=== "Desktop"
    <div markdown>
    <video style="float: left; margin-right: 0.8em;" width="400" controls="true" autoplay="true" loop="true" src="../videos/shared-elements-tutorial-step-4-desktop.mp4" ></video>

    With that we now have a shared element transition where the sender image transitions across the two screens!
    </div>

### 5. Adding `Modifier.sharedBounds()` 

As the remaining shared items are all `Text` Composable we will use `Modifier.sharedBounds()`. 
Initially the `sharedBounds()` setup should be the same for each type of `Text` in `EmailItem` and in `EmailDetailContent`.

```kotlin title="Sender Text"
Text(
  text = email.sender,
  modifier =
    Modifier.sharedBounds(
        sharedContentState =
          rememberSharedContentState(
            EmailSharedTransitionKey(
              id = email.id,
              type = EmailSharedTransitionKey.ElementType.SenderName,
            )
          ),
        animatedVisibilityScope =
          requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
      )
      // ...
```

```kotlin title="Subject Text"
Text(
  text = email.subject,
  modifier =
    Modifier.sharedBounds(
      sharedContentState =
        rememberSharedContentState(
          EmailSharedTransitionKey(
            id = email.id,
            type = EmailSharedTransitionKey.ElementType.Subject,
          )
        ),
      animatedVisibilityScope =
        requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
    ),
    // ...
```


```kotlin title="Body Text"
Text(
  text = email.body,
  modifier =
    Modifier.sharedBounds(
      sharedContentState =
        rememberSharedContentState(
          EmailSharedTransitionKey(
            id = email.id,
            type = EmailSharedTransitionKey.ElementType.Body,
          )
        ),
      animatedVisibilityScope =
        requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
    ),
    // ...
```

=== "Android"
    <div markdown>

    <video style="float: left; margin-right: 0.8em;" width="400" controls="true" autoplay="true" loop="true" src="../videos/shared-elements-tutorial-step-5-android.mp4" ></video>

    After the `Modifier.sharedBounds()` is added to each of the three `Text` in the `EmailItem` composable and the `EmailDetailContent` composable you should now see the majority of the email tranistioning across the two `Screens`.

    </div>

=== "Desktop"
    <div markdown>

    <video style="float: left; margin-right: 0.8em;" width="400" controls="true" autoplay="true" loop="true" src="../videos/shared-elements-tutorial-step-5-desktop.mp4" ></video>

    After the `Modifier.sharedBounds()` is added to each of the three `Text` in the `EmailItem` composable and the `EmailDetailContent` composable you should now see the majority of the email tranistioning across the two `Screens`.

    </div>

At this point you can customize the enter and exit transitions, or any of the other parameters of `sharedBounds()`, to further improve the animation.

## Conclusion

You should now be able to integrate Circuit Shared Elements into your existing app! 
Circuit Shared Elements provides easy access to the needed `SharedTransitionScope` with `SharedElementTransitionScope` directly where it is needed. The `SharedElementTransitionScope` will then provide easy access to the `AnimatedVisibilityScope` used by Circuit for `Navigation`. Once setup this will let you use the standard Compose Shared Element transitions for any `Screen` to `Screen` transition.

