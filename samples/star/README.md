STAR
====

Android
-------

Simply build the apk project like a standard Android app.

Desktop
-------

Run `./gradlew :samples:star:run -Pcircuit.buildDesktop`. This property must be set to build the desktop app due to https://youtrack.jetbrains.com/issue/KT-30878.

_Note that you cannot run the project from the `main()` function in `Main.kt`, as this does not create a fat jar bundle with all necessary dependencies._
