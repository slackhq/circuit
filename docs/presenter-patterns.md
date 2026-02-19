Scaling Presenters
==================

## Overview

As your Circuit application grows, presenters naturally accumulate complexity. If you've worked on a Circuit app for a while, you may have noticed some presenters becoming harder to maintain. This guide provides patterns and recipes to help.

!!! info "Community Guide"
    This guide is based on experience at Slack building and scaling a large Android application with Circuit. We welcome contributions, alternative approaches, and feedback from the community to make this guide more comprehensive.

Here are some common signs of presenter complexity:

- **Event sink explosion**: Each mutable state value generally needs its own events, so the event sink grows quickly
- **Internal state sprawl**: State scoped to `present()` makes it hard to break out event handling into smaller functions
- **Boolean flag soup**: Many boolean flags (`showWarningBanner`, `showBottomSheetA`, `showDialogB`) lead to complex UIs with conditional blocks
- **Testing difficulties**: The more properties state has, the harder it becomes to test comprehensively

Don't worry - these are natural growing pains! A well-structured presenter exhibits these qualities:

| Quality | Description |
|---------|-------------|
| **Single Responsibility** | Handles only presentation logic |
| **Testable** | Can be unit tested in isolation with clear inputs and outputs |
| **Maintainable** | Easy to understand, modify, and extend over time |

---

## Composition Patterns

When a presenter grows too large, you have options. Here are three patterns for breaking it down, each suited to different scenarios.

### Pattern 1: Presenter Decomposition

**Presenter decomposition** involves breaking down a complex `present()` method into smaller `@Composable` helper functions without extracting full presenters.

**When to use**:

- Single presenter handling multiple related concerns
- Improving organization without full extraction
- Observation logic that can be extracted

**Example: Order details with extracted observation**

=== "Before"

    ```kotlin
    class OrderDetailsPresenter(
      private val screen: OrderDetailsScreen,
      private val navigator: Navigator,
      private val orderRepository: OrderRepository,
      private val paymentRepository: PaymentRepository,
    ) : Presenter<OrderDetailsState> {

      @Composable
      override fun present(): OrderDetailsState {
        // All observation logic inline in present()
        val order by produceRetainedState<Order?>(null) {
          orderRepository.observeOrder(screen.orderId).collect { value = it }
        }

        val paymentStatus by produceRetainedState(PaymentStatus.Unknown) {
          paymentRepository.observePaymentStatus(screen.orderId).collect { value = it }
        }

        val shippingInfo by produceRetainedState<ShippingInfo?>(null) {
          orderRepository.observeShipping(screen.orderId).collect { value = it }
        }

        // State construction and event handling all in one place
        return when {
          order == null -> OrderDetailsState.Loading
          else -> OrderDetailsState.Success(
            order = order,
            paymentStatus = paymentStatus,
            shippingInfo = shippingInfo,
          ) { event ->
            // Event handling inline
            when (event) {
              is OrderDetailsEvent.TrackPackage ->
                navigator.goTo(TrackingScreen(event.trackingId))
              is OrderDetailsEvent.ContactSupport ->
                navigator.goTo(SupportScreen(screen.orderId))
              is OrderDetailsEvent.RequestRefund ->
                navigator.goTo(RefundScreen(screen.orderId))
            }
          }
        }
      }
    }
    ```

=== "After"

    ```kotlin
    class OrderDetailsPresenter(
      private val screen: OrderDetailsScreen,
      private val navigator: Navigator,
      private val orderRepository: OrderRepository,
      private val paymentRepository: PaymentRepository,
    ) : Presenter<OrderDetailsState> {

      @Composable
      override fun present(): OrderDetailsState {
        // Extracted observation logic - present() is now a coordinator
        val order = observeOrder()
        val paymentStatus = observePaymentStatus()
        val shippingInfo = observeShipping()

        return when {
          order == null -> OrderDetailsState.Loading
          else -> OrderDetailsState.Success(
            order = order,
            paymentStatus = paymentStatus,
            shippingInfo = shippingInfo,
          ) { event ->
            handleEvent(event)
          }
        }
      }

      @Composable
      private fun observeOrder(): Order? {
        return produceRetainedState<Order?>(null) {
          orderRepository.observeOrder(screen.orderId).collect { value = it }
        }.value
      }

      @Composable
      private fun observePaymentStatus(): PaymentStatus {
        return produceRetainedState(PaymentStatus.Unknown) {
          paymentRepository.observePaymentStatus(screen.orderId).collect { value = it }
        }.value
      }

      @Composable
      private fun observeShipping(): ShippingInfo? {
        return produceRetainedState<ShippingInfo?>(null) {
          orderRepository.observeShipping(screen.orderId).collect { value = it }
        }.value
      }

      private fun handleEvent(event: OrderDetailsEvent) {
        when (event) {
          is OrderDetailsEvent.TrackPackage ->
            navigator.goTo(TrackingScreen(event.trackingId))
          is OrderDetailsEvent.ContactSupport ->
            navigator.goTo(SupportScreen(screen.orderId))
          is OrderDetailsEvent.RequestRefund ->
            navigator.goTo(RefundScreen(screen.orderId))
        }
      }
    }
    ```

=== "State & Events"

    ```kotlin
    // Screen with navigation parameter
    data class OrderDetailsScreen(val orderId: String) : Screen

    // State
    sealed interface OrderDetailsState : CircuitUiState {
      data object Loading : OrderDetailsState
      data class Success(
        val order: Order,
        val paymentStatus: PaymentStatus,
        val shippingInfo: ShippingInfo?,
        val eventSink: (OrderDetailsEvent) -> Unit,
      ) : OrderDetailsState
    }

    // Events
    sealed interface OrderDetailsEvent : CircuitUiEvent {
      data class TrackPackage(val trackingId: String) : OrderDetailsEvent
      data object ContactSupport : OrderDetailsEvent
      data object RequestRefund : OrderDetailsEvent
    }
    ```

**Key techniques**:

- Extract `@Composable` private functions for observation logic (e.g., `observeOrder()`)
- Extract non-composable private functions for event handling (e.g., `handleEvent()`)
- Keep `present()` focused on coordinating and combining state
- Each helper function should have a single responsibility

### Pattern 2: Composite Presenters

**Composite presenters** embed full child presenters to create dashboard-style screens. Each child manages its own state and events.

**When to use**:

- Building screens that combine multiple independent features
- Child components could be screens on their own
- Each component handles its own events independently

**Example: User dashboard with profile and settings**

=== "Dashboard (Composite)"

    ```kotlin
    // Composite presenter combining child presenters
    class DashboardPresenter(
      private val screen: DashboardScreen,
      private val profilePresenter: ProfilePresenter,
      private val settingsPresenter: SettingsPresenter,
      private val refreshUseCase: RefreshDashboardUseCase,
    ) : Presenter<DashboardState> {

      @Composable
      override fun present(): DashboardState {
        val scope = rememberCoroutineScope()
        var refreshState by rememberRetained {
          mutableStateOf<DashboardRefreshState>(DashboardRefreshState.Idle)
        }

        val profileState = profilePresenter.present()
        val settingsState = settingsPresenter.present()

        return when (profileState) {
          is ProfileState.Loading -> DashboardState.Loading
          is ProfileState.Loaded -> DashboardState.Loaded(
            profile = profileState,
            settings = settingsState,
            refreshState = refreshState,
          ) { event ->
            when (event) {
              DashboardEvent.Refresh -> {
                scope.launch {
                  refreshState = DashboardRefreshState.Refreshing
                  refreshUseCase.refresh()
                  refreshState = DashboardRefreshState.Idle
                }
              }
            }
          }
        }
      }
    }

    data object DashboardScreen : Screen

    sealed interface DashboardState : CircuitUiState {
      data object Loading : DashboardState
      data class Loaded(
        val profile: ProfileState.Loaded,
        val settings: SettingsState,
        val refreshState: DashboardRefreshState,
        val eventSink: (DashboardEvent) -> Unit,
      ) : DashboardState
    }

    sealed interface DashboardRefreshState {
      data object Idle : DashboardRefreshState
      data object Refreshing : DashboardRefreshState
    }

    sealed interface DashboardEvent : CircuitUiEvent {
      data object Refresh : DashboardEvent
    }
    ```

=== "ProfilePresenter"

    ```kotlin
    // Child presenter - can be used standalone or embedded
    class ProfilePresenter(
      private val screen: ProfileScreen,
      private val userRepository: UserRepository,
    ) : Presenter<ProfileState> {

      @Composable
      override fun present(): ProfileState {
        var bio by rememberRetained { mutableStateOf("") }
        val user by produceRetainedState<User?>(null) {
          value = userRepository.getCurrentUser()
          bio = value?.bio ?: ""
        }

        return when (user) {
          null -> ProfileState.Loading
          else -> ProfileState.Loaded(
            username = user.username,
            bio = bio,
          ) { event ->
            when (event) {
              is ProfileEvent.UpdateBio -> {
                bio = event.newBio
                userRepository.updateBio(event.newBio)
              }
            }
          }
        }
      }
    }

    data object ProfileScreen : Screen

    sealed interface ProfileState : CircuitUiState {
      data object Loading : ProfileState
      data class Loaded(
        val username: String,
        val bio: String,
        val eventSink: (ProfileEvent) -> Unit,
      ) : ProfileState
    }

    sealed interface ProfileEvent : CircuitUiEvent {
      data class UpdateBio(val newBio: String) : ProfileEvent
    }
    ```

=== "SettingsPresenter"

    ```kotlin
    // Child presenter - can be used standalone or embedded
    class SettingsPresenter(
      private val screen: SettingsScreen,
      private val settingsRepository: SettingsRepository,
    ) : Presenter<SettingsState> {

      @Composable
      override fun present(): SettingsState {
        val settings by produceRetainedState(Settings()) {
          settingsRepository.observeSettings().collect { value = it }
        }

        return SettingsState(
          isDarkMode = settings.isDarkMode,
          notificationsEnabled = settings.notificationsEnabled,
        ) { event ->
          when (event) {
            is SettingsEvent.ToggleDarkMode ->
              settingsRepository.setDarkMode(event.enabled)
            is SettingsEvent.ToggleNotifications ->
              settingsRepository.setNotifications(event.enabled)
          }
        }
      }
    }

    data object SettingsScreen : Screen

    data class SettingsState(
      val isDarkMode: Boolean,
      val notificationsEnabled: Boolean,
      val eventSink: (SettingsEvent) -> Unit,
    ) : CircuitUiState

    sealed interface SettingsEvent : CircuitUiEvent {
      data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent
      data class ToggleNotifications(val enabled: Boolean) : SettingsEvent
    }
    ```

=== "UI"

    ```kotlin
    @Composable
    fun Dashboard(state: DashboardState, modifier: Modifier = Modifier) {
      when (state) {
        is DashboardState.Loading -> LoadingIndicator()
        is DashboardState.Loaded -> {
          PullToRefreshBox(
            isRefreshing = state.refreshState is DashboardRefreshState.Refreshing,
            onRefresh = { state.eventSink(DashboardEvent.Refresh) },
            modifier = modifier,
          ) {
            Column {
              Profile(state.profile)
              Settings(state.settings)
            }
          }
        }
      }
    }

    @Composable
    fun Profile(state: ProfileState.Loaded, modifier: Modifier = Modifier) {
      Column(modifier) {
        Text("Username: ${state.username}")
        TextField(
          value = state.bio,
          onValueChange = { state.eventSink(ProfileEvent.UpdateBio(it)) },
        )
      }
    }

    @Composable
    fun Settings(state: SettingsState, modifier: Modifier = Modifier) {
      Column(modifier) {
        SwitchRow(
          label = "Dark Mode",
          checked = state.isDarkMode,
          onCheckedChange = { state.eventSink(SettingsEvent.ToggleDarkMode(it)) },
        )
        SwitchRow(
          label = "Notifications",
          checked = state.notificationsEnabled,
          onCheckedChange = { state.eventSink(SettingsEvent.ToggleNotifications(it)) },
        )
      }
    }
    ```

!!! tip "Injecting Child Presenters"
    How you get child presenters into the composite presenter is flexible: inject them directly, create them inline, or pull them from a Circuit instance. The key is that shared state should flow through the data layer when possible.

### Pattern 3: StateProducer

**StateProducers** are reusable components that produce state but aren't used on their own. Unlike full presenters, they don't implement the `Presenter` interface and are always consumed by a parent presenter that coordinates their output.

**When to use**:

- Reusable state logic shared across multiple screens
- Components that wouldn't make sense as standalone screens
- Parent presenter needs to coordinate multiple related pieces of state
- Extracting repetitive observation patterns into a single place

**Example: Product details with availability check**

=== "ProductDetailsPresenter"

    ```kotlin
    // Parent presenter coordinates the producer
    class ProductDetailsPresenter(
      private val screen: ProductDetailsScreen,
      private val navigator: Navigator,
      private val productRepository: ProductRepository,
      private val availabilityProducer: AvailabilityStateProducer,
    ) : Presenter<ProductDetailsState> {

      @Composable
      override fun present(): ProductDetailsState {
        val product by produceRetainedState<Product?>(null) {
          value = productRepository.getProduct(screen.productId)
        }

        // Producer handles availability presentation logic separately
        val availability = availabilityProducer.produce(screen.productId)

        return when (product) {
          null -> ProductDetailsState.Loading
          else -> ProductDetailsState.Loaded(
            product = product,
            availability = availability,
          ) { event ->
            when (event) {
              is ProductDetailsEvent.AddToCart -> {
                productRepository.addToCart(screen.productId)
                navigator.goTo(CartScreen)
              }
            }
          }
        }
      }
    }

    data class ProductDetailsScreen(val productId: String) : Screen

    sealed interface ProductDetailsState : CircuitUiState {
      data object Loading : ProductDetailsState
      data class Loaded(
        val product: Product,
        val availability: AvailabilityState,
        val eventSink: (ProductDetailsEvent) -> Unit,
      ) : ProductDetailsState
    }

    sealed interface ProductDetailsEvent : CircuitUiEvent {
      data object AddToCart : ProductDetailsEvent
    }

    data object CartScreen : Screen
    ```

=== "AvailabilityStateProducer"

    ```kotlin
    // Reusable producer - used by ProductDetails, Wishlist, etc.
    class AvailabilityStateProducer(
      private val inventoryRepository: InventoryRepository,
    ) {
      @Composable
      fun produce(productId: String): AvailabilityState {
        val inventory by produceRetainedState<Inventory?>(null) {
          inventoryRepository.observeInventory(productId).collect { value = it }
        }

        return when {
          inventory == null -> AvailabilityState.Checking
          inventory.quantity > 10 -> AvailabilityState.InStock
          inventory.quantity > 0 -> AvailabilityState.LowStock(inventory.quantity)
          else -> AvailabilityState.OutOfStock
        }
      }
    }

    sealed interface AvailabilityState {
      data object Checking : AvailabilityState
      data object InStock : AvailabilityState
      data class LowStock(val remaining: Int) : AvailabilityState
      data object OutOfStock : AvailabilityState
    }
    ```

**Key characteristics**:

- Producer is injected into the parent presenter
- Parent typically handles events and coordinates state
- Producers can have their own event sinks, though it's less common
- Reusable across multiple screens that need the same state logic
- Never used standalone; always consumed by a parent presenter

---

## Use Cases: Separating Business Logic

**Use cases** (also called interactors) encapsulate business logic in small, focused classes that can be injected into presenters and state producers. They separate "what the app does" from "how it's presented."

### Why Use Cases?

Presenters should focus on:

- Observing data and transforming it into UI state
- Routing events to the appropriate handlers
- Managing UI-specific concerns (loading states, error display)

Business logic should live elsewhere:

- Validation rules
- Data transformations
- Coordinating multiple repository calls
- Business rules and policies

### Using Use Cases in Presenters

This example brings together multiple patterns: decomposed observation functions (Pattern 1), a StateProducer (Pattern 3), use cases for business logic, and an internal state holder class.

=== "Presenter"

    ```kotlin
    class CheckoutPresenter(
      private val screen: CheckoutScreen,
      private val navigator: Navigator,
      private val cartTotalProducer: CartTotalStateProducer,
      private val observeOrderStatus: ObserveOrderStatusUseCase,
      private val validateEmail: ValidateEmailUseCase,
      private val placeOrder: PlaceOrderUseCase,
    ) : Presenter<CheckoutState> {

      @Composable
      override fun present(): CheckoutState {
        val cartTotalState = cartTotalProducer.produce()
        val emailField = rememberRetained { EmailFieldState(validateEmail) }
        val orderStatus = observeOrderStatus(screen.orderId)

        return when (cartTotalState) {
          is CartTotalState.Loading -> CheckoutState.Loading
          is CartTotalState.Ready -> CheckoutState.Ready(
            email = emailField.value,
            emailError = emailField.error,
            cartTotal = cartTotalState.total,
            orderStatus = orderStatus,
          ) { event ->
            when (event) {
              is CheckoutEvent.EmailChanged -> emailField.onValueChange(event.email)
              is CheckoutEvent.SubmitOrder -> {
                if (emailField.isValid) {
                  placeOrder(screen.orderId)
                }
              }
            }
          }
        }
      }

      @Composable
      private fun observeOrderStatus(orderId: String): OrderStatus {
        val status by produceRetainedState<OrderStatus>(OrderStatus.Idle) {
          observeOrderStatus(orderId).collect { status ->
            // Navigate on success
            if (status is OrderStatus.Success) {
              navigator.goTo(OrderConfirmationScreen(status.orderId))
            }
            value = status
          }
        }
        return status
      }
    }

    // Internal helper class for managing email field state
    private class EmailFieldState(
      private val validateEmail: ValidateEmailUseCase,
    ) {
      var value by mutableStateOf("")
        private set
      var error by mutableStateOf<String?>(null)
        private set

      val isValid: Boolean
        get() = error == null

      fun onValueChange(newValue: String) {
        value = newValue
        validate()
      }

      private fun validate() {
        error = when (val result = validateEmail(value)) {
          is ValidationResult.Error -> result.message
          ValidationResult.Valid -> null
        }
      }
    }
    ```

=== "Use Cases"

    ```kotlin
    // Synchronous use case for validation
    class ValidateEmailUseCase {
      operator fun invoke(email: String): ValidationResult {
        return when {
          email.isBlank() -> ValidationResult.Error("Email is required")
          !email.contains("@") -> ValidationResult.Error("Invalid email format")
          else -> ValidationResult.Valid
        }
      }
    }

    // Use case delegates to repository which handles the async operation
    class PlaceOrderUseCase(
      private val orderRepository: OrderRepository,
    ) {
      operator fun invoke(orderId: String) {
        orderRepository.placeOrder(orderId)
      }
    }

    // Flow-based use case for observing order status
    class ObserveOrderStatusUseCase(
      private val orderRepository: OrderRepository,
    ) {
      operator fun invoke(orderId: String): Flow<OrderStatus> {
        return orderRepository.observeOrderStatus(orderId)
      }
    }

    sealed interface OrderStatus {
      data object Idle : OrderStatus
      data object Submitting : OrderStatus
      data class Success(val orderId: String) : OrderStatus
      data class Error(val message: String) : OrderStatus
    }

    sealed interface ValidationResult {
      data object Valid : ValidationResult
      data class Error(val message: String) : ValidationResult
    }
    ```

=== "State & Events"

    ```kotlin
    // Screens
    data class CheckoutScreen(val orderId: String) : Screen
    data class OrderConfirmationScreen(val orderId: String) : Screen

    // State
    sealed interface CheckoutState : CircuitUiState {
      data object Loading : CheckoutState
      data class Ready(
        val email: String,
        val emailError: String?,
        val cartTotal: CartTotal,
        val orderStatus: OrderStatus,
        val eventSink: (CheckoutEvent) -> Unit,
      ) : CheckoutState
    }

    // Events
    sealed interface CheckoutEvent : CircuitUiEvent {
      data class EmailChanged(val email: String) : CheckoutEvent
      data object SubmitOrder : CheckoutEvent
    }
    ```

=== "StateProducer"

    ```kotlin
    // Reusable producer for cart total observation
    class CartTotalStateProducer(
      private val observeCartTotal: ObserveCartTotalUseCase,
    ) {
      @Composable
      fun produce(): CartTotalState {
        val cartTotal by produceRetainedState<CartTotal?>(null) {
          observeCartTotal().collect { value = it }
        }

        return when (cartTotal) {
          null -> CartTotalState.Loading
          else -> CartTotalState.Ready(cartTotal)
        }
      }
    }

    sealed interface CartTotalState {
      data object Loading : CartTotalState
      data class Ready(val total: CartTotal) : CartTotalState
    }

    // Flow-based use case for observing cart data
    class ObserveCartTotalUseCase(
      private val cartRepository: CartRepository,
      private val pricingService: PricingService,
    ) {
      operator fun invoke(): Flow<CartTotal> {
        return cartRepository.observeCart()
          .map { cart ->
            val subtotal = cart.items.sumOf { it.price * it.quantity }
            val discount = pricingService.calculateDiscount(cart)
            val tax = pricingService.calculateTax(subtotal - discount)
            CartTotal(subtotal, discount, tax)
          }
      }
    }
    ```

### When to Extract a Use Case

Extract business logic into a use case when:

- The same logic is needed in multiple presenters
- The logic involves multiple repositories or services
- The logic has complex rules that warrant dedicated tests
- You want to test business logic independently from presentation logic

!!! tip "Use Cases vs Repositories"
    Repositories handle data access (fetching, caching, persistence). Use cases handle business operations that may coordinate multiple repositories and apply business rules. A use case might call several repositories, but a repository should never call a use case.

---

## Testing Strategies

One of the benefits of these patterns is improved testability. Here's how to test each pattern effectively.

!!! note "Testing Decomposed Presenters"
    Decomposed presenters are tested the same way as any other presenter - the extracted helper functions are implementation details. Use `Presenter.test()` as usual.

### Testing Composite Presenters

Test composite presenters by injecting test implementations of child presenters:

```kotlin
@Test
fun `dashboard combines profile and settings state`() = runTest {
  val profilePresenter = FakeProfilePresenter(
    ProfileState.Loaded(username = "testuser", bio = "Hello") {}
  )
  val settingsPresenter = FakeSettingsPresenter(
    SettingsState(isDarkMode = true, notificationsEnabled = false) {}
  )

  val presenter = DashboardPresenter(
    screen = DashboardScreen,
    profilePresenter = profilePresenter,
    settingsPresenter = settingsPresenter,
    refreshUseCase = FakeRefreshUseCase(),
  )

  presenter.test {
    val state = awaitItem() as DashboardState.Loaded
    assertEquals("testuser", state.profile.username)
    assertTrue(state.settings.isDarkMode)
  }
}
```

### Testing StateProducers

Test state producers using Molecule directly:

```kotlin
@Test
fun `produces availability state from repository`() = runTest {
  val repository = FakeInventoryRepository().apply {
    setInventory("product-123", Inventory(quantity = 5))
  }
  val producer = AvailabilityStateProducer(repository)

  moleculeFlow(RecompositionMode.Immediate) {
    producer.produce("product-123")
  }.test {
    val state = awaitItem() as AvailabilityState.LowStock
    assertEquals(5, state.remaining)
  }
}
```

### Testing Event Flow

For presenters with complex event routing, emit events through the state's `eventSink` and verify the expected side effects:

```kotlin
@Test
fun `refresh event triggers refresh use case`() = runTest {
  val refreshUseCase = FakeRefreshUseCase()
  val presenter = DashboardPresenter(
    screen = DashboardScreen,
    profilePresenter = FakeProfilePresenter(),
    settingsPresenter = FakeSettingsPresenter(),
    refreshUseCase = refreshUseCase,
  )

  presenter.test {
    val state = awaitItem() as DashboardState.Loaded
    state.eventSink(DashboardEvent.Refresh)
    assertTrue(refreshUseCase.wasRefreshCalled)
  }
}
```

---

## Common Pitfalls

These are patterns we've seen cause issues in practice. If you spot them in your code, consider refactoring.

### 1. Giant Presenters

**Problem**: A presenter file grows large with many responsibilities.

**Solution**: Apply one of the composition patterns to break it down.

### 2. Boolean Flag Soup

**Problem**: State contains many boolean flags for UI visibility.

```kotlin
// Avoid
data class State(
  val showWarning: Boolean,
  val showDialog: Boolean,
  val showBottomSheet: Boolean,
  val showSnackbar: Boolean,
  // ...
)
```

**Solution**: Use sealed classes or nullable sub-states:

```kotlin
// Prefer
data class State(
  val dialog: DialogState?,      // null = not shown
  val bottomSheet: SheetState?,  // null = not shown
  val snackbar: SnackbarState?,  // null = not shown
)
```

### 3. Event Handler Spaghetti

**Problem**: Deeply nested `when` statements in event handlers.

```kotlin
// Avoid
{ event ->
  when (event) {
    is Event.DialogAction -> when (event.action) {
      is DialogAction.Confirm -> when (event.action.type) {
        // More nesting...
      }
    }
  }
}
```

**Solution**: Extract event handlers into separate functions, use sealed classes with flatter hierarchies, or encapsulate related state and handlers into helper classes (like `EmailFieldState` in the Use Cases example).

---

## Migration Recipes

Ready to refactor? These step-by-step recipes show how to apply the patterns to existing code.

### Recipe: Boolean Flags to Sealed States

**Before**:

```kotlin
data class ProductState(
  val isLoading: Boolean,
  val hasError: Boolean,
  val errorMessage: String?,
  val product: Product?,
  val eventSink: (Event) -> Unit,
) : CircuitUiState
```

**After**:

```kotlin
sealed interface ProductState : CircuitUiState {
  data object Loading : ProductState
  data class Error(
    val message: String,
    val eventSink: (ErrorEvent) -> Unit,
  ) : ProductState
  data class Success(
    val product: Product,
    val eventSink: (SuccessEvent) -> Unit,
  ) : ProductState
}

sealed interface ErrorEvent : CircuitUiEvent {
  data object Retry : ErrorEvent
}

sealed interface SuccessEvent : CircuitUiEvent {
  data object Refresh : SuccessEvent
  data class AddToCart(val quantity: Int) : SuccessEvent
}
```

### Recipe: Large Presenter to Composite

1. Identify logically independent sections of the presenter
2. Create separate `Screen` definitions for each section
3. Create child presenters that implement `Presenter` for each screen
4. Create a composite presenter that combines the child states
5. Update the UI to render each child state

**Before** (monolithic presenter):

```kotlin
@Composable
override fun present(): AccountState {
  // Profile logic
  val profile by produceRetainedState<Profile?>(null) { ... }
  // Settings logic
  val settings by produceRetainedState<Settings?>(null) { ... }
  // Billing logic
  val billing by produceRetainedState<Billing?>(null) { ... }
  // ... more logic
}
```

**After** (composite presenter):

```kotlin
class AccountPresenter(
  private val screen: AccountScreen,
  private val profilePresenter: ProfilePresenter,
  private val settingsPresenter: SettingsPresenter,
  private val billingPresenter: BillingPresenter,
) : Presenter<AccountState> {
  @Composable
  override fun present(): AccountState {
    return AccountState(
      profile = profilePresenter.present(),
      settings = settingsPresenter.present(),
      billing = billingPresenter.present(),
    )
  }
}
```

### Recipe: Extract StateProducer from Large Presenter

1. Identify a cohesive piece of state with its observation logic
2. Create a new class with a `@Composable` function that returns the state
3. Inject the producer into the original presenter
4. Call the producer's function in `present()`
5. Update event handling to route through the parent

**Before** (state logic embedded in presenter):

```kotlin
@Composable
override fun present(): OrderState {
  // This user-related logic could be reused elsewhere
  val user by produceRetainedState<User?>(null) {
    value = userRepository.getCurrentUser()
  }
  var isUserExpanded by rememberRetained { mutableStateOf(false) }
  // ... rest of presenter
}
```

**After** (extracted to producer):

```kotlin
class UserStateProducer(private val userRepository: UserRepository) {
  @Composable
  fun produce(): UserSectionState {
    val user by produceRetainedState<User?>(null) {
      value = userRepository.getCurrentUser()
    }
    var isExpanded by rememberRetained { mutableStateOf(false) }
    return UserSectionState(user, isExpanded) { isExpanded = !isExpanded }
  }
}

// In presenter
val userState = userStateProducer.produce()
```
