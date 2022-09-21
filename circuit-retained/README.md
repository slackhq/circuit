circuit-retained
================

This artifact contains an implementation of `rememberRetained` and `produceRetainedState`. This is
useful for cases where you want to retain non-parcelable state across configuration changes. This
comes at the cost of not participating in the `SavedStateRegistry` and thus not being able to
persist across process death.
