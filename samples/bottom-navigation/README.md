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

This sample's `buildCircuitSaver()` creates a `SerializableCircuitSaver` and registers every screen
under `polymorphic(CircuitSaveable::class)`. Each platform passes that saver explicitly to
`rememberSaveableNavStack()` so its navigation history uses the same serialization setup.

For the unreleased 0.35 version, sample screens remain both `@Serializable` and `@Parcelize`.
Serialization handles their saved navigation state, while Android screens must still implement
`Parcelable`. Removing that Android requirement is planned for a future release and has not shipped.
