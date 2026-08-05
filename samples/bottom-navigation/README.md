Navigation
==========

Android
-------

Simply build the project like a standard Android app.

Desktop
-------

Run `./gradlew :samples:bottom-navigation:run`.

_Note that you cannot run the project from the `main()` function in `Main.kt`, as this does not create a fat jar bundle with all necessary dependencies._

Persistence
-----------

This sample's `buildCircuitSaver()` creates a `SerializableCircuitSaver`. It registers every screen manually under `polymorphic(CircuitSaveable::class)` (because the sample does not use DI). Each platform passes the saver to `rememberSaveableNavStack()`.

Sample screens use both `@Serializable` and `@Parcelize`. Serialization handles their saved navigation state. Android screens must still implement `Parcelable`. A future release will remove this requirement.
