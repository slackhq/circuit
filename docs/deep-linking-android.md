Deep-linking using Circuit
==========================

[Deep linking][deeplinking] to app is a vast topic and the implementation detail can vary based on your application needs.
Deep linking strategy can be highly sophisticated or simple based on the use-case.

To keep things very simple and easy to understand, we will be focusing on deep linking to Android platform only. 
You can then extend the idea to other platforms as needed.

!!! info
    Pre-requisite: You should have basic understanding of deep linking and should go through the Android's official [training guide](https://developer.android.com/training/app-links/deep-linking) that contains useful information about deep linking to Android app.

### Deep linking strategy
Essentially, you need to define a strategy for you app to handle the incoming deep link. We will take a look at Circuit's [email app](https://slackhq.github.io/circuit/tutorial/) from the tutorial with following screens:

* **Inbox Screen** - List all incoming emails
* **Details Screen** - Show details of the email
* **Draft Screen** - Compose a new email _(additional screen added for this demo)_

By default, the app launches with **Inbox Screen** as the root screen. To deep link into other two screens, we will define URI path segment as:

* **`inbox`** - Inbox Screen
* **`view_email`** - Details Screen with `emailId` as query parameter
* **`new_email`** - Draft Screen

Assuming, our app's URI scheme is `circuitapp://` with host `emailonthego`, the deep links will look like:

* `circuitapp://emailonthego/inbox` - Deep link to Inbox Screen
* `circuitapp://emailonthego/inbox/view_email?emailId=2` - Deep link to Details Screen with email id 2 with Inbox Screen as parent
* `circuitapp://emailonthego/inbox/new_email` - Deep link to Draft Screen with Inbox Screen as parent
* `circuitapp://emailonthego/inbox/view_email/new_email?emailId=3` - Deep link to Draft Screen and in backstack there is Details Screen and Inbox Screen.

Once you have added [intent-filter](https://developer.android.com/training/app-links/deep-linking#adding-filters) in your `AndroidManifest.xml` file, you can handle the incoming deep link in your `Activity`.

### Handling deep link in Circuit
In our case, we will create a `parseDeepLink` function to parse the incoming deep link and return the list of screens to be displayed in the app.
```kotlin
// In your `MainActivity.kt` or activity that has the `intent-filter` for custom URI scheme
override fun onCreate(savedInstanceState: Bundle?) {
    // ...

    setContent {
        ComposeAppTheme {
            // When there is no deeplink data in the intent, default to Inbox Screen as root screen
            val screensStack: List<Screen> = parseDeepLink(intent) ?: listOf(InboxScreen)
            val backStack = rememberSaveableBackStack(screensStack)
            val navigator = rememberCircuitNavigator(backStack)

            // ...
        }
    }
}
```

And here is a simple implementation of `parseDeepLink` function that creates the list of screens based on the incoming deep link:
```kotlin
/**
 * Parses the deep link from the given [Intent.getData] and returns a list of screens to navigate to.
 */
private fun parseDeepLink(intent: Intent): List<Screen>? {
    val dataUri = intent.data ?: return null
    val screens = mutableListOf<Screen>()

    dataUri.pathSegments.filter { it.isNotBlank() }.forEach { pathSegment ->
        when (pathSegment) {
            "inbox" -> screens.add(InboxScreen)
            "view_email" ->
                dataUri.getQueryParameter("emailId")?.let {
                    screens.add(DetailScreen(it))
                }
            "new_email" -> screens.add(DraftNewEmailScreen)
            else -> Log.d("App", "Unknown path segment: $pathSegment")
        }
    }

    return screens.takeIf { it.isNotEmpty() }
}
```

!!! tip
    Ideally, you would have deep link components that does the parsing and building screens based on parameters provided in the deep link URI.


To test try the following command from the terminal:
```shell
adb shell am start -W \
  -a android.intent.action.VIEW \
  -d "circuitapp://emailonthego/inbox/view_email/new_email/?emailId=2"
```

It should launch the app and navigate to the Draft Screen with backstack containing Details Screen and Inbox Screen.


[circuit]: https://slackhq.github.io/circuit/
[deeplinking]: https://en.wikipedia.org/wiki/Mobile_deep_linking