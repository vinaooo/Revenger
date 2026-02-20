package com.vinaooo.revenger.ui.retromenu3.navigation


/**
 * Interface for input adapters.
 *
 * Each input type (gamepad, touch, keyboard) implements this interface to translate its
 * specific inputs into unified NavigationEvents.
 *
 * This pattern allows:
 * - Adding new input types easily
 * - Keeping the core of the system input-agnostic
 * - Testing each adapter in isolation
 * - Reusing translation logic
 */
interface InputTranslator {
    /**
     * Translate a specific input into one or more NavigationEvents.
     *
     * @param input Input data (type depends on adapter)
     * @return List of generated NavigationEvents, or empty list if the input is not relevant
     */
    fun translate(input: Any): List<NavigationEvent>
}
