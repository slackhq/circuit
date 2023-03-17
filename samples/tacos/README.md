# Tacos

Sample Android application demonstrating the use of a composite Circuit [Presenter](/circuit-runtime-presenter/src/commonMain/kotlin/com/slack/circuit/runtime/presenter/Presenter.kt)
to construct a multi-step taco ordering flow/wizard.

This application has a single circuit that manages several sub-screens with their own unique state and UI. Order state flows from the parent circuit into
child sub-screens; events from the child flow into the parent.


## Code Structure

There are 2 primary roles:

### Parent circuit
[OrderTacosCircuit](src/main/kotlin/com/slack/circuit/tacos/OrderTacosCircuit.kt) maintains order state and manages wizard flow. It:

* Determines which child [OrderStep][orderStep] should be active/visible
* Owns the forward and back navigation buttons in the app header
* Owns the order total bar/button in the app footer

### Child order step
Child screens consist of 3 sub-components:
1. [OrderStep][orderStep] - sealed interface defined by the parent and used when changing wizard steps.
2. [StateProducer][orderStep] - similar to a Circuit presenter. Implementing class/function accepts order state and an event sink from the parent, and
   returns it's own state.
3. `@Composable` UI - similar to Circuit UI. Implementing class/function accepts state returned by related state producer.


## Wizard steps
This wizard has 4 unique steps:

1. [FillingsOrderStep](src/main/kotlin/com/slack/circuit/tacos/step/FillingsOrderStep.kt) - select a single filling
2. [ToppingsOrderStep](src/main/kotlin/com/slack/circuit/tacos/step/ToppingsOrderStep.kt) - select at least 3 toppings
3. [ConfirmationOrderStep](src/main/kotlin/com/slack/circuit/tacos/step/ConfirmationOrderStep.kt) - confirm order and get charge breakdown
4. [SummaryOrderStep](src/main/kotlin/com/slack/circuit/tacos/step/SummaryOrderStep.kt) - summary and option to restart wizard

[orderStep]: src/main/kotlin/com/slack/circuit/tacos/step/OrderStep.kt
