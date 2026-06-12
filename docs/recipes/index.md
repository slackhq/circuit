Recipes
=======

Task-focused, copy-pasteable solutions to common Circuit patterns. Start with the problem you have;
there is no required reading order.

New to Circuit? Start with the [Tutorial](../tutorial.md) and [States and Events](../states-and-events.md)
first; these recipes assume you know what a `Screen`, `Presenter`, and `Ui` are.

## Loading data

- [Show loading, loaded, and error states](loading-states.md)
- [Observe a Flow or repository without leaking it](observe-a-flow.md)
- [Retry a failed load](retry-a-failed-load.md)
- [Pull to refresh](pull-to-refresh.md)
- [Paginate a list (load more on scroll)](paginate-a-list.md)
- [Debounce a search field](debounce-search.md)

## Navigation & results

- [Return a result to the previous screen](return-a-result.md)
- [Ask for confirmation with a dialog](confirmation-dialog.md)
- [Pick a value from a bottom sheet](bottom-sheet-picker.md)
- [Tabs with independent back stacks](tabs-with-back-stacks.md)
- [Navigate to an Android Activity or URL](navigate-to-android.md)
- [Intercept, block, or rewrite navigation](intercept-navigation.md)

## Composing & reusing UI

- [Embed a reusable component that delegates navigation](reusable-component-subcircuit.md)
- [Share selection state across list items](shared-selection-state.md)
- [Keep UI state across rotation and the back stack](keep-state-across-config-change.md)

## Effects & lifecycle

- [Log an impression once when a screen opens](log-an-impression.md)
- [Run a one-shot suspend action from an event](run-suspend-from-event.md)

## Forms

- [A form with validation and submit](form-with-validation.md)

## Testing

- [Test a presenter that navigates](test-a-presenter.md)
- [Test a presenter that shows an overlay](test-an-overlay.md)
