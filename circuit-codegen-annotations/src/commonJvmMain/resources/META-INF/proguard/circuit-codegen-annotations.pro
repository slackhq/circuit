# Prevent R8 failing on missing classes when not using Anvil for DI.
-dontwarn software.amazon.lastmile.kotlin.inject.anvil.**

# Prevent R8 failing on CircuitInject's meta-annotation when not using Hilt for DI.
-dontwarn dagger.hilt.GeneratesRootInput
