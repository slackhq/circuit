Scaling Presenters
==================

As your Circuit application grows, presenters naturally accumulate complexity. This guide provides patterns and recipes to keep presenters maintainable, testable, and composable.

## The Problem

Without guidance, presenters often become monolithic:

- **Event sink explosion**: Each mutable state value generally needs its own events, so the event sink grows quickly
- **Internal state sprawl**: State scoped to `present()` makes it hard to break out event handling into smaller functions
- **Boolean flag soup**: Many boolean flags (`showWarningBanner`, `showBottomSheetA`, `showDialogB`) lead to complex UIs with conditional blocks
- **Testing difficulties**: The more properties state has, the harder it becomes to test comprehensively

## What Makes a Presenter "Scalable"

A well-structured presenter exhibits these qualities:

| Quality | Description |
|---------|-------------|
| **Single Responsibility** | Handles only presentation logic |
| **Testable** | Can be unit tested in isolation with clear inputs and outputs |
| **Composable** | Can be combined with other presenters without tight coupling |

**Target metrics**:

- 80-200 lines of code
- 2-5 state variables
- Clear, single purpose

**Warning signs** to watch for:

- 500+ lines of presenter code
- 15+ state variables
- Nested `when` statements in event handlers

---

## State Management Fundamentals

Before diving into composition patterns, it's important to understand state retention and structure.

### State Retention Strategies

Circuit offers three retention mechanisms:

| Function | Survives Recomposition | Survives Back Stack | Survives Config Changes | Survives Process Death |
|----------|----------------------|-------------------|----------------------|---------------------|
| `remember` | Yes | No | No | No |
| `rememberRetained` | Yes | Yes* | Yes | No |
| `rememberSaveable` | Yes | Yes* | Yes | Yes |

*If using `NavigableCircuitContent`'s default configuration.

**Guidelines**:

- Use `remember` for transient UI state (animations, hover states)
- Use `rememberRetained` for data that's expensive to reload
- Use `rememberSaveable` for user-entered data that should survive process death

!!! warning "Retention Pitfalls"
    Never retain `Navigator`, `Context`, or other framework objects in `rememberRetained` or `rememberSaveable`. These can cause memory leaks.

### Sealed State Classes Over Boolean Flags

Instead of multiple boolean flags:

```kotlin
// Avoid: Boolean flag soup
data class OrderState(
  val isLoading: Boolean,
  val hasError: Boolean,
  val isEmpty: Boolean,
  val data: Order?,
  val eventSink: (Event) -> Unit,
) : CircuitUiState
```

Use sealed classes to model mutually exclusive states:

```kotlin
// Prefer: Sealed state hierarchy
sealed interface OrderState : CircuitUiState {
  data object Loading : OrderState
  data class Error(val message: String, val eventSink: (ErrorEvent) -> Unit) : OrderState
  data object Empty : OrderState
  data class Success(val order: Order, val eventSink: (SuccessEvent) -> Unit) : OrderState
}
```

### State-Specific Events

Different states often support different user interactions. Rather than having a single event type with events that only apply to certain states, define events specific to each state:

```kotlin
// Avoid: Monolithic event type where some events only apply to certain states
sealed interface OrderEvent : CircuitUiEvent {
  data object Retry : OrderEvent        // Only valid in Error state
  data object Refresh : OrderEvent      // Only valid in Success state
  data class UpdateQuantity(val quantity: Int) : OrderEvent  // Only valid in Success state
}
```

```kotlin
// Prefer: State-specific events
sealed interface ErrorEvent : CircuitUiEvent {
  data object Retry : ErrorEvent
}

sealed interface SuccessEvent : CircuitUiEvent {
  data object Refresh : SuccessEvent
  data class UpdateQuantity(val quantity: Int) : SuccessEvent
}
```

This ensures events are only available when they make sense:

```kotlin
@Composable
fun OrderScreen(state: OrderState, modifier: Modifier = Modifier) {
  when (state) {
    is OrderState.Loading -> LoadingIndicator()
    is OrderState.Empty -> EmptyMessage()
    is OrderState.Error -> {
      ErrorMessage(
        message = state.message,
        // Only Retry is available here
        onRetry = { state.eventSink(ErrorEvent.Retry) },
      )
    }
    is OrderState.Success -> {
      OrderDetails(
        order = state.order,
        // Refresh and UpdateQuantity are available here
        onRefresh = { state.eventSink(SuccessEvent.Refresh) },
        onQuantityChange = { state.eventSink(SuccessEvent.UpdateQuantity(it)) },
      )
    }
  }
}
```

The presenter handles each event type in the appropriate state:

```kotlin
@Composable
override fun present(): OrderState {
  var order by remember { mutableStateOf<Order?>(null) }
  var error by remember { mutableStateOf<String?>(null) }

  // ... loading logic ...

  return when {
    error != null -> OrderState.Error(error) { event ->
      when (event) {
        ErrorEvent.Retry -> {
          error = null
          // trigger reload
        }
      }
    }
    order == null -> OrderState.Loading
    order.items.isEmpty() -> OrderState.Empty
    else -> OrderState.Success(order) { event ->
      when (event) {
        SuccessEvent.Refresh -> { /* refresh order */ }
        is SuccessEvent.UpdateQuantity -> { /* update quantity */ }
      }
    }
  }
}
```

This approach:

- Makes invalid states unrepresentable
- Ensures events are only available when they're valid
- Simplifies UI rendering logic
- Provides clearer testing scenarios
- Eliminates impossible event handling (no need to handle `Retry` in `Success` state)

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

### Defining Use Cases

Use cases are typically simple classes with a single public method. They can be synchronous, suspend functions, or return Flows:

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
    // Check inventory
    val unavailable = cart.items.filter { !inventoryRepository.isAvailable(it.id) }
    if (unavailable.isNotEmpty()) {
      return OrderResult.ItemsUnavailable(unavailable)
    }

    // Place order
    val order = orderRepository.createOrder(cart)

    // Track analytics
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

### Using Use Cases in Presenters

Inject use cases into presenters to keep presentation logic clean:

```kotlin
// Screen definition with sealed state and events
data object CheckoutScreen : Screen {
  sealed interface State : CircuitUiState {
    data object Loading : State
    data class Ready(
      val email: String,
      val emailError: String?,
      val cartTotal: CartTotal,
      val orderState: OrderState,
      val eventSink: (Event) -> Unit,
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class EmailChanged(val email: String) : Event
    data object SubmitOrder : Event
  }

  // Order submission state (internal to presenter)
  sealed interface OrderState {
    data object Idle : OrderState
    data object Submitting : OrderState
    data class Error(val message: String) : OrderState
  }
}

// Encapsulates email field state and validation
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
    // Trigger validation and update error state
    error = when (val result = validateEmail(value)) {
      is ValidationResult.Error -> result.message
      ValidationResult.Valid -> null
    }
    return error == null
  }
}

class CheckoutPresenter(
  private val validateEmail: ValidateEmailUseCase,
  private val placeOrder: PlaceOrderUseCase,
  private val observeCartTotal: ObserveCartTotalUseCase,
  private val navigator: Navigator,
) : Presenter<CheckoutScreen.State> {

  @Composable
  override fun present(): CheckoutScreen.State {
    val emailField = rememberRetained { EmailFieldState(validateEmail) }
    var orderState by remember {
      mutableStateOf<CheckoutScreen.OrderState>(CheckoutScreen.OrderState.Idle)
    }

    val cartTotal by produceState<CartTotal?>(null) {
      observeCartTotal().collect { value = it }
    }

    // Handle order submission
    LaunchedEffect(orderState) {
      if (orderState is CheckoutScreen.OrderState.Submitting) {
        when (val result = placeOrder(/* cart */)) {
          is OrderResult.Success -> {
            orderState = CheckoutScreen.OrderState.Idle
            navigator.goTo(OrderConfirmationScreen(result.order.id))
          }
          is OrderResult.ItemsUnavailable -> {
            orderState = CheckoutScreen.OrderState.Error("Some items are no longer available")
          }
        }
      }
    }

    return when (cartTotal) {
      null -> CheckoutScreen.State.Loading
      else -> CheckoutScreen.State.Ready(
        email = emailField.value,
        emailError = emailField.error,
        cartTotal = cartTotal,
        orderState = orderState,
      ) { event ->
        when (event) {
          is CheckoutScreen.Event.EmailChanged -> emailField.onValueChange(event.email)
          is CheckoutScreen.Event.SubmitOrder -> {
            if (emailField.validate()) {
              orderState = CheckoutScreen.OrderState.Submitting
            }
            // If validation fails, error is already set by validate()
          }
        }
      }
    }
  }
}
```

### Using Use Cases in State Producers

State producers can also leverage use cases for reusable business logic:

```kotlin
class PriceSummaryStateProducer(
  private val observeCartTotal: ObserveCartTotalUseCase,
  private val formatCurrency: FormatCurrencyUseCase,
) {
  @Composable
  fun produce(): PriceSummaryState {
    val cartTotal by produceState<CartTotal?>(null) {
      observeCartTotal().collect { value = it }
    }

    return when (cartTotal) {
      null -> PriceSummaryState.Loading
      else -> PriceSummaryState.Loaded(
        subtotal = formatCurrency(cartTotal.subtotal),
        discount = formatCurrency(cartTotal.discount),
        tax = formatCurrency(cartTotal.tax),
        total = formatCurrency(cartTotal.total),
      )
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

Keep logic inline in the presenter when:

- It's simple state transformation (mapping, filtering)
- It's only used in one place
- Extracting it would add indirection without benefit

!!! tip "Use Cases vs Repositories"
    Repositories handle data access (fetching, caching, persistence). Use cases handle business operations that may coordinate multiple repositories and apply business rules. A use case might call several repositories, but a repository should never call a use case.

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
) : Presenter<CartScreen.State> {

  @Composable
  override fun present(): CartScreen.State {
    val cartItems by produceState(emptyList<String>()) {
      value = cartRepository.getCartItemIds()
    }

    // Produce state for each item
    val itemStates = cartItems.map { itemId ->
      cartItemProducer.produce(itemId)
    }

    return CartScreen.State(
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

```kotlin
// Child screen definitions
data object ProfileScreen : Screen {
  sealed interface State : CircuitUiState {
    data object Loading : State
    data class Loaded(
      val username: String,
      val bio: String,
      val eventSink: (Event) -> Unit,
    ) : State
  }

  sealed interface Event : CircuitUiEvent {
    data class UpdateBio(val newBio: String) : Event
  }
}

data object SettingsScreen : Screen {
  data class State(
    val isDarkMode: Boolean,
    val notificationsEnabled: Boolean,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class ToggleDarkMode(val enabled: Boolean) : Event
    data class ToggleNotifications(val enabled: Boolean) : Event
  }
}

// Composite screen combining both
data object DashboardScreen : Screen {
  sealed interface State : CircuitUiState {
    data object Loading : State
    data class Loaded(
      val profile: ProfileScreen.State.Loaded,
      val settings: SettingsScreen.State,
      val refreshState: RefreshState,
      val eventSink: (Event) -> Unit,
    ) : State
  }

  sealed interface RefreshState {
    data object Idle : RefreshState
    data object Refreshing : RefreshState
  }

  sealed interface Event : CircuitUiEvent {
    data object Refresh : Event
  }
}

// Child presenters
class ProfilePresenter(
  private val userRepository: UserRepository,
) : Presenter<ProfileScreen.State> {

  @Composable
  override fun present(): ProfileScreen.State {
    var bio by rememberRetained { mutableStateOf("") }
    val user by produceState<User?>(null) {
      value = userRepository.getCurrentUser()
      bio = value?.bio ?: ""
    }

    return when (user) {
      null -> ProfileScreen.State.Loading
      else -> ProfileScreen.State.Loaded(
        username = user.username,
        bio = bio,
      ) { event ->
        when (event) {
          is ProfileScreen.Event.UpdateBio -> {
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
) : Presenter<SettingsScreen.State> {

  @Composable
  override fun present(): SettingsScreen.State {
    val settings by produceState(Settings()) {
      settingsRepository.observeSettings().collect { value = it }
    }

    return SettingsScreen.State(
      isDarkMode = settings.isDarkMode,
      notificationsEnabled = settings.notificationsEnabled,
    ) { event ->
      when (event) {
        is SettingsScreen.Event.ToggleDarkMode ->
          settingsRepository.setDarkMode(event.enabled)
        is SettingsScreen.Event.ToggleNotifications ->
          settingsRepository.setNotifications(event.enabled)
      }
    }
  }
}

// Composite presenter
class DashboardPresenter(
  private val profilePresenter: ProfilePresenter,
  private val settingsPresenter: SettingsPresenter,
  private val refreshUseCase: RefreshDashboardUseCase,
) : Presenter<DashboardScreen.State> {

  @Composable
  override fun present(): DashboardScreen.State {
    var refreshState by remember {
      mutableStateOf<DashboardScreen.RefreshState>(DashboardScreen.RefreshState.Idle)
    }

    val profileState = profilePresenter.present()
    val settingsState = settingsPresenter.present()

    return when (profileState) {
      is ProfileScreen.State.Loading -> DashboardScreen.State.Loading
      is ProfileScreen.State.Loaded -> DashboardScreen.State.Loaded(
        profile = profileState,
        settings = settingsState,
        refreshState = refreshState,
      ) { event ->
        when (event) {
          DashboardScreen.Event.Refresh -> {
            refreshState = DashboardScreen.RefreshState.Refreshing
            refreshUseCase.refresh()
            refreshState = DashboardScreen.RefreshState.Idle
          }
        }
      }
    }
  }
}
```

**Corresponding UI**:

```kotlin
@Composable
fun Dashboard(state: DashboardScreen.State, modifier: Modifier = Modifier) {
  when (state) {
    is DashboardScreen.State.Loading -> LoadingIndicator()
    is DashboardScreen.State.Loaded -> {
      PullToRefreshBox(
        isRefreshing = state.refreshState is DashboardScreen.RefreshState.Refreshing,
        onRefresh = { state.eventSink(DashboardScreen.Event.Refresh) },
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
fun Profile(state: ProfileScreen.State.Loaded, modifier: Modifier = Modifier) {
  Column(modifier) {
    Text("Username: ${state.username}")
    TextField(
      value = state.bio,
      onValueChange = { state.eventSink(ProfileScreen.Event.UpdateBio(it)) },
    )
  }
}

@Composable
fun Settings(state: SettingsScreen.State, modifier: Modifier = Modifier) {
  Column(modifier) {
    SwitchRow(
      label = "Dark Mode",
      checked = state.isDarkMode,
      onCheckedChange = { state.eventSink(SettingsScreen.Event.ToggleDarkMode(it)) },
    )
    SwitchRow(
      label = "Notifications",
      checked = state.notificationsEnabled,
      onCheckedChange = { state.eventSink(SettingsScreen.Event.ToggleNotifications(it)) },
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
class OrderDetailsPresenter(
  private val screen: OrderDetailsScreen,
  private val orderRepository: OrderRepository,
  private val paymentRepository: PaymentRepository,
  private val navigator: Navigator,
) : Presenter<OrderDetailsScreen.State> {

  @Composable
  override fun present(): OrderDetailsScreen.State {
    // Extracted observation logic
    val order = observeOrder()
    val paymentStatus = observePaymentStatus()
    val shippingInfo = observeShipping()

    // Combine into state
    return when {
      order == null -> OrderDetailsScreen.State.Loading
      else -> OrderDetailsScreen.State.Success(
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

  private fun handleEvent(event: OrderDetailsScreen.Event) {
    when (event) {
      is OrderDetailsScreen.Event.TrackPackage ->
        navigator.goTo(TrackingScreen(event.trackingId))
      is OrderDetailsScreen.Event.ContactSupport ->
        navigator.goTo(SupportScreen(screen.orderId))
      is OrderDetailsScreen.Event.RequestRefund ->
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

## Decision Framework

Use this flowchart to choose the right pattern:

```
                    ┌─────────────────────────────┐
                    │ Is it a standalone screen   │
                    │ with its own navigation?    │
                    └─────────────┬───────────────┘
                                  │
                    ┌─────────────┴───────────────┐
                    │                             │
                   YES                           NO
                    │                             │
                    ▼                             ▼
        ┌───────────────────┐       ┌─────────────────────────┐
        │ Composite         │       │ Is it reusable across   │
        │ Presenters        │       │ multiple screens?       │
        └───────────────────┘       └───────────┬─────────────┘
                                                │
                                    ┌───────────┴───────────┐
                                    │                       │
                                   YES                     NO
                                    │                       │
                                    ▼                       ▼
                        ┌───────────────────┐   ┌───────────────────┐
                        │ StateProducer     │   │ Decomposition     │
                        └───────────────────┘   └───────────────────┘
```

### Pattern Comparison Table

| Aspect | StateProducer | Composite Presenter | Decomposition |
|--------|---------------|---------------------|---------------|
| **Reusability** | High (shared logic) | High (full screens) | Low |
| **Standalone** | No, always consumed by parent | Yes, can be used alone | No |
| **State Sharing** | Direct via parent | Through data layer | Within presenter |
| **Event Handling** | Via parent | Each handles own | Via parent |
| **Testing** | Test with Molecule | Test each presenter | Test whole presenter |
| **Use Case** | Shared state logic | Dashboard-style | Organize large presenter |

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
    ProfileScreen.State.Loaded(username = "testuser", bio = "Hello") {}
  )
  val settingsPresenter = FakeSettingsPresenter(
    SettingsScreen.State(isDarkMode = true, notificationsEnabled = false) {}
  )

  val presenter = DashboardPresenter(
    profilePresenter = profilePresenter,
    settingsPresenter = settingsPresenter,
    refreshUseCase = FakeRefreshUseCase(),
  )

  presenter.test {
    val state = awaitItem() as DashboardScreen.State.Loaded
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
    val state = awaitItem() as DashboardScreen.State.Loaded
    state.eventSink(DashboardScreen.Event.Refresh)
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

### 5. Mixed UI and Presentation Logic

**Problem**: Business logic mixed with Compose UI code.

**Solution**: Keep presenters focused on state computation. UIs should only render state and emit events.

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

### Recipe: Large Presenter to Composite

1. Identify logically independent sections of the presenter
2. Create separate `Screen` definitions for each section
3. Create child presenters that implement `Presenter` for each screen
4. Create a composite presenter that combines the child states
5. Update the UI to render each child state

---

## Summary

Building scalable presenters in Circuit requires thoughtful composition:

- **Use Cases** for extracting business logic that can be tested independently
- **StateProducers** for reusable state logic that isn't used standalone
- **Composite Presenters** for combining independent, reusable screens
- **Decomposition** for organizing complex presenters without full extraction

Start simple and extract only when complexity warrants it. The goal is maintainable, testable code that clearly expresses intent.
