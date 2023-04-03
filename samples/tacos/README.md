# Tacos

Sample Android application demonstrating the use of a composite Circuit [Presenter][presenter]
to construct a multi-step taco ordering flow/wizard.

This application has a single circuit that manages several sub-screens with their own unique state and UI. Order state flows from the parent circuit into
child sub-screens; events from the child flow into the parent.


## Code Structure

There are 2 primary roles:

### Parent circuit
[OrderTacosCircuit][orderTacosCircuit] maintains order state and manages wizard flow. It:

* Determines which child [OrderStep][orderStep] should be active/visible
* Owns the forward and back navigation buttons in the app header
* Owns the order total bar/button in the app footer

### Child order step
Child screens consist of 3 sub-components:
1. [OrderStep][orderStep] - sealed interface defined by the parent and used when changing wizard steps.
2. [StateProducer][stateProducer] - similar to a Circuit presenter. Implementing class/function accepts order state and an event sink from the parent, and
   returns it's own state.
3. `@Composable` UI - similar to Circuit UI. Implementing class/function accepts state returned by related state producer.


## Wizard steps
This wizard has 4 unique steps:

1. [FillingsOrderStep][fillingsOrderStep] - select a single filling
2. [ToppingsOrderStep][toppingsOrderStep] - select at least 3 toppings
3. [ConfirmationOrderStep][confirmationOrderStep] - confirm order and get charge breakdown
4. [SummaryOrderStep][summaryOrderStep] - summary and option to restart wizard

[presenter]: /circuit-runtime-presenter/src/commonMain/kotlin/com/slack/circuit/runtime/presenter/Presenter.kt
[orderTacosCircuit]: src/main/kotlin/com/slack/circuit/tacos/OrderTacosCircuit.kt
[orderStep]: src/main/kotlin/com/slack/circuit/tacos/step/OrderStep.kt
[stateProducer]: src/main/kotlin/com/slack/circuit/tacos/step/OrderStep.kt#L49
[fillingsOrderStep]: src/main/kotlin/com/slack/circuit/tacos/step/FillingsOrderStep.kt
[toppingsOrderStep]: src/main/kotlin/com/slack/circuit/tacos/step/ToppingsOrderStep.kt
[confirmationOrderStep]: src/main/kotlin/com/slack/circuit/tacos/step/ConfirmationOrderStep.kt
[summaryOrderStep]: src/main/kotlin/com/slack/circuit/tacos/step/SummaryOrderStep.kt
