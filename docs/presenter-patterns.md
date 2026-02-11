Scaling Presenters
==================

## Overview

As your Circuit application grows, presenters naturally accumulate complexity. This guide provides patterns and recipes to keep presenters maintainable, testable, and composable.

Without guidance, presenters often become monolithic:

- **Event sink explosion**: Each mutable state value generally needs its own events, so the event sink grows quickly
- **Internal state sprawl**: State scoped to `present()` makes it hard to break out event handling into smaller functions
- **Boolean flag soup**: Many boolean flags (`showWarningBanner`, `showBottomSheetA`, `showDialogB`) lead to complex UIs with conditional blocks
- **Testing difficulties**: The more properties state has, the harder it becomes to test comprehensively

A well-structured presenter exhibits these qualities:

| Quality | Description |
|---------|-------------|
| **Single Responsibility** | Handles only presentation logic |
| **Testable** | Can be unit tested in isolation with clear inputs and outputs |
| **Composable** | Can be combined with other presenters without tight coupling |

---

## Composition Patterns

There are three primary patterns for breaking down complex presenters, each suited to different scenarios.

### Pattern 1: StateProducer

**StateProducers** are reusable components that produce state but aren't used on their own. Unlike full presenters, they don't implement the `Presenter` interface and are always consumed by a parent presenter that coordinates their output.

**When to use**:

- Reusable state logic shared across multiple screens
- Components that wouldn't make sense as standalone screens
- Parent presenter needs to coordinate multiple related pieces of state
- Extracting repetitive observation patterns into a single place

**Example: Shopping cart with item producer**

```kotlin
// Screen is just a navigation marker
data object CartScreen : Screen

// Cart state and events
data class CartState(
  val items: List<CartItemState>,
  val eventSink: (CartEvent) -> Unit,
) : CircuitUiState

sealed interface CartEvent : CircuitUiEvent {
  data class RemoveItem(val itemId: String) : CartEvent
  data class UpdateQuantity(val itemId: String, val quantity: Int) : CartEvent
}

// State produced by the item state producer
sealed interface CartItemState {
  data object Loading : CartItemState
  data class Loaded(val item: CartItem, val quantity: Int) : CartItemState
  data class Updating(val item: CartItem, val quantity: Int) : CartItemState
}

class CartItemStateProducer(
  private val cartRepository: CartRepository,
) {
  @Composable
  fun produce(itemId: String): CartItemState {
    var quantity by remember { mutableIntStateOf(1) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    val item by produceState<CartItem?>(null) {
      value = cartRepository.getItem(itemId)
    }

    return when {
      item == null -> CartItemState.Loading
      updateState is UpdateState.InProgress -> CartItemState.Updating(item, quantity)
      else -> CartItemState.Loaded(item, quantity)
    }
  }
}

private sealed interface UpdateState {
  data object Idle : UpdateState
  data object InProgress : UpdateState
}

// Parent presenter coordinates the producer
class CartPresenter(
  private val cartItemProducer: CartItemStateProducer,
  private val cartRepository: CartRepository,
) : Presenter<CartState> {

  @Composable
  override fun present(): CartState {
    val cartItems by produceState(emptyList<String>()) {
      value = cartRepository.getCartItemIds()
    }

    // Produce state for each item
    val itemStates = cartItems.map { itemId ->
      cartItemProducer.produce(itemId)
    }

    return CartState(
      items = itemStates,
    ) { event ->
      when (event) {
        is CartEvent.RemoveItem -> cartRepository.removeItem(event.itemId)
        is CartEvent.UpdateQuantity -> cartRepository.updateQuantity(event.itemId, event.quantity)
      }
    }
  }
}
```

**Key characteristics**:

- Producer is injected into the parent presenter
- Parent typically handles events and coordinates state
- Producers can have their own event sinks, though it's less common
- Reusable across multiple screens that need the same state logic
- Never used standalone; always consumed by a parent presenter

### Pattern 2: Composite Presenters

**Composite presenters** embed full child presenters to create dashboard-style screens. Each child manages its own state and events.

**When to use**:

- Building screens that combine multiple independent features
- Child components could be screens on their own
- Each component handles its own events independently

**Example: User dashboard with profile and settings**

=== "Presenters"

    ```kotlin
    // Child presenters
    class ProfilePresenter(
      private val userRepository: UserRepository,
    ) : Presenter<ProfileState> {

      @Composable
      override fun present(): ProfileState {
        var bio by rememberRetained { mutableStateOf("") }
        val user by produceState<User?>(null) {
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

    class SettingsPresenter(
      private val settingsRepository: SettingsRepository,
    ) : Presenter<SettingsState> {

      @Composable
      override fun present(): SettingsState {
        val settings by produceState(Settings()) {
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

    // Composite presenter combining child presenters
    class DashboardPresenter(
      private val profilePresenter: ProfilePresenter,
      private val settingsPresenter: SettingsPresenter,
      private val refreshUseCase: RefreshDashboardUseCase,
    ) : Presenter<DashboardState> {

      @Composable
      override fun present(): DashboardState {
        var refreshState by remember {
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
                refreshState = DashboardRefreshState.Refreshing
                refreshUseCase.refresh()
                refreshState = DashboardRefreshState.Idle
              }
            }
          }
        }
      }
    }
    ```

=== "State & Events"

    ```kotlin
    // Screens are just navigation markers
    data object ProfileScreen : Screen
    data object SettingsScreen : Screen
    data object DashboardScreen : Screen

    // Profile state and events
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

    // Settings state and events
    data class SettingsState(
      val isDarkMode: Boolean,
      val notificationsEnabled: Boolean,
      val eventSink: (SettingsEvent) -> Unit,
    ) : CircuitUiState

    sealed interface SettingsEvent : CircuitUiEvent {
      data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent
      data class ToggleNotifications(val enabled: Boolean) : SettingsEvent
    }

    // Dashboard state and events
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

### Pattern 3: Presenter Decomposition

**Presenter decomposition** involves breaking down a complex `present()` method into smaller `@Composable` helper functions without extracting full presenters.

**When to use**:

- Single presenter handling multiple related concerns
- Improving organization without full extraction
- Observation logic that can be extracted

**Example: Order details with extracted observation**

```kotlin
// Screen with navigation parameter
data class OrderDetailsScreen(val orderId: String) : Screen

// State and events
sealed interface OrderDetailsState : CircuitUiState {
  data object Loading : OrderDetailsState
  data class Success(
    val order: Order,
    val paymentStatus: PaymentStatus,
    val shippingInfo: ShippingInfo?,
    val eventSink: (OrderDetailsEvent) -> Unit,
  ) : OrderDetailsState
}

sealed interface OrderDetailsEvent : CircuitUiEvent {
  data class TrackPackage(val trackingId: String) : OrderDetailsEvent
  data object ContactSupport : OrderDetailsEvent
  data object RequestRefund : OrderDetailsEvent
}

class OrderDetailsPresenter(
  private val screen: OrderDetailsScreen,
  private val orderRepository: OrderRepository,
  private val paymentRepository: PaymentRepository,
  private val navigator: Navigator,
) : Presenter<OrderDetailsState> {

  @Composable
  override fun present(): OrderDetailsState {
    // Extracted observation logic
    val order = observeOrder()
    val paymentStatus = observePaymentStatus()
    val shippingInfo = observeShipping()

    // Combine into state
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
    return produceState<Order?>(null) {
      orderRepository.observeOrder(screen.orderId).collect { value = it }
    }.value
  }

  @Composable
  private fun observePaymentStatus(): PaymentStatus {
    return produceState(PaymentStatus.Unknown) {
      paymentRepository.observePaymentStatus(screen.orderId).collect { value = it }
    }.value
  }

  @Composable
  private fun observeShipping(): ShippingInfo? {
    return produceState<ShippingInfo?>(null) {
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

**Key techniques**:

- Extract `@Composable` functions for observation logic
- Extract non-composable functions for event handling
- Use multiple small `LaunchedEffect` blocks instead of one large one

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

Inject use cases into presenters to keep presentation logic clean:

=== "Presenter"

    ```kotlin
    class CheckoutPresenter(
      private val validateEmail: ValidateEmailUseCase,
      private val placeOrder: PlaceOrderUseCase,
      private val observeCartTotal: ObserveCartTotalUseCase,
      private val navigator: Navigator,
    ) : Presenter<CheckoutState> {

      @Composable
      override fun present(): CheckoutState {
        val emailField = rememberRetained { EmailFieldState(validateEmail) }
        var orderState by remember { mutableStateOf<CheckoutOrderState>(CheckoutOrderState.Idle) }

        val cartTotal by produceState<CartTotal?>(null) {
          observeCartTotal().collect { value = it }
        }

        // Handle order submission
        LaunchedEffect(orderState) {
          if (orderState is CheckoutOrderState.Submitting) {
            when (val result = placeOrder(/* cart */)) {
              is OrderResult.Success -> {
                orderState = CheckoutOrderState.Idle
                navigator.goTo(OrderConfirmationScreen(result.order.id))
              }
              is OrderResult.ItemsUnavailable -> {
                orderState = CheckoutOrderState.Error("Some items are no longer available")
              }
            }
          }
        }

        return when (cartTotal) {
          null -> CheckoutState.Loading
          else -> CheckoutState.Ready(
            email = emailField.value,
            emailError = emailField.error,
            cartTotal = cartTotal,
            orderState = orderState,
          ) { event ->
            when (event) {
              is CheckoutEvent.EmailChanged -> emailField.onValueChange(event.email)
              is CheckoutEvent.SubmitOrder -> {
                if (emailField.validate()) {
                  orderState = CheckoutOrderState.Submitting
                }
              }
            }
          }
        }
      }
    }
    ```

=== "State & Events"

    ```kotlin
    // Screen is just a navigation marker
    data object CheckoutScreen : Screen

    // State
    sealed interface CheckoutState : CircuitUiState {
      data object Loading : CheckoutState
      data class Ready(
        val email: String,
        val emailError: String?,
        val cartTotal: CartTotal,
        val orderState: CheckoutOrderState,
        val eventSink: (CheckoutEvent) -> Unit,
      ) : CheckoutState
    }

    // Events
    sealed interface CheckoutEvent : CircuitUiEvent {
      data class EmailChanged(val email: String) : CheckoutEvent
      data object SubmitOrder : CheckoutEvent
    }

    // Internal state for order submission
    sealed interface CheckoutOrderState {
      data object Idle : CheckoutOrderState
      data object Submitting : CheckoutOrderState
      data class Error(val message: String) : CheckoutOrderState
    }

    // Encapsulates email field state and validation (internal to presenter)
    private class EmailFieldState(
      private val validateEmail: ValidateEmailUseCase,
    ) {
      var value by mutableStateOf("")
        private set
      var error by mutableStateOf<String?>(null)
        private set

      fun onValueChange(newValue: String) {
        value = newValue
        error = when (val result = validateEmail(newValue)) {
          is ValidationResult.Error -> result.message
          ValidationResult.Valid -> null
        }
      }

      fun validate(): Boolean {
        error = when (val result = validateEmail(value)) {
          is ValidationResult.Error -> result.message
          ValidationResult.Valid -> null
        }
        return error == null
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

    // Suspend use case for one-shot operations
    class PlaceOrderUseCase(
      private val orderRepository: OrderRepository,
      private val inventoryRepository: InventoryRepository,
      private val analyticsTracker: AnalyticsTracker,
    ) {
      suspend operator fun invoke(cart: Cart): OrderResult {
        val unavailable = cart.items.filter { !inventoryRepository.isAvailable(it.id) }
        if (unavailable.isNotEmpty()) {
          return OrderResult.ItemsUnavailable(unavailable)
        }

        val order = orderRepository.createOrder(cart)
        analyticsTracker.trackOrderPlaced(order)
        return OrderResult.Success(order)
      }
    }

    // Flow-based use case for observing data
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
- You want to test business logic independently from UI logic

!!! tip "Use Cases vs Repositories"
    Repositories handle data access (fetching, caching, persistence). Use cases handle business operations that may coordinate multiple repositories and apply business rules. A use case might call several repositories, but a repository should never call a use case.

---

## Decision Framework

| Aspect | StateProducer | Composite Presenter | Decomposition |
|--------|---------------|---------------------|---------------|
| **Reusability** | High (shared logic) | High (full screens) | Low |
| **Standalone** | No, always consumed by parent | Yes, can be used alone | No |
| **State Sharing** | Direct via parent | Through data layer | Within presenter |
| **Event Handling** | Via parent | Each handles own | Via parent |
| **Testing** | Test with Molecule | Test each presenter | Test whole presenter |
| **Best For** | Shared state logic | Dashboard-style screens | Organize large presenter |

---

## Testing Strategies

### Testing StateProducers

Test state producers using Molecule directly:

```kotlin
@Test
fun `produces item state from repository`() = runTest {
  val repository = FakeCartRepository().apply {
    setItem("123", CartItem(id = "123", name = "Widget", price = 9.99))
  }
  val producer = CartItemStateProducer(repository)

  moleculeFlow(RecompositionMode.Immediate) {
    producer.produce("123")
  }.test {
    val state = awaitItem() as CartItemState.Loaded
    assertEquals("Widget", state.item.name)
    assertEquals(1, state.quantity)
  }
}
```

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

### Testing Event Flow

For presenters with complex event routing, use `TestEventSink`:

```kotlin
@Test
fun `refresh event triggers refresh use case`() = runTest {
  val refreshUseCase = FakeRefreshUseCase()
  val presenter = DashboardPresenter(
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

## Anti-Patterns to Avoid

### 1. Giant Presenters

**Problem**: A single presenter file exceeds 500 lines with many responsibilities.

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

### 3. Leaky State Retention

**Problem**: Retaining `Navigator`, `Context`, or other framework objects.

```kotlin
// Avoid - will cause memory leaks!
var navigator by rememberRetained { mutableStateOf(navigator) }
```

**Solution**: Only retain data, not framework objects. Access framework objects through injection.

### 4. Event Handler Spaghetti

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

**Solution**: Extract event handlers into separate functions or use sealed classes with flatter hierarchies.

---

## Migration Recipes

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
  val user by produceState<User?>(null) {
    value = userRepository.getCurrentUser()
  }
  var isUserExpanded by remember { mutableStateOf(false) }
  // ... rest of presenter
}
```

**After** (extracted to producer):

```kotlin
class UserStateProducer(private val userRepository: UserRepository) {
  @Composable
  fun produce(): UserSectionState {
    val user by produceState<User?>(null) {
      value = userRepository.getCurrentUser()
    }
    var isExpanded by remember { mutableStateOf(false) }
    return UserSectionState(user, isExpanded) { isExpanded = !isExpanded }
  }
}

// In presenter
val userState = userStateProducer.produce()
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
  val profile by produceState<Profile?>(null) { ... }
  // Settings logic
  val settings by produceState<Settings?>(null) { ... }
  // Billing logic
  val billing by produceState<Billing?>(null) { ... }
  // 500+ more lines...
}
```

**After** (composite presenter):

```kotlin
class AccountPresenter(
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

