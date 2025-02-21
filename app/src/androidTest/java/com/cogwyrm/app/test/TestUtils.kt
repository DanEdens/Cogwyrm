package com.cogwyrm.app.test

import android.view.View
import android.widget.EditText
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

/**
 * Custom matchers and actions for Espresso tests
 */
object TestUtils {
    /**
     * Custom matcher for TextInputLayout error text
     */
    fun hasTextInputLayoutErrorText(expectedErrorText: String): Matcher<View> {
        return object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with error text: $expectedErrorText")
            }

            override fun matchesSafely(item: TextInputLayout): Boolean {
                return item.error?.toString() == expectedErrorText
            }
        }
    }

    /**
     * Custom action to set text in EditText without keyboard animation
     */
    fun setTextInTextInput(text: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDisplayed(), isAssignableFrom(EditText::class.java))
            }

            override fun getDescription(): String {
                return "Set text in EditText"
            }

            override fun perform(uiController: UiController, view: View) {
                (view as EditText).setText(text)
                uiController.loopMainThreadUntilIdle()
            }
        }
    }
}

/**
 * Custom IdlingResource for async operations
 */
class AsyncOperationIdlingResource(private val name: String) : IdlingResource {
    private var callback: IdlingResource.ResourceCallback? = null
    @Volatile private var isIdle = true

    override fun getName(): String = name

    override fun isIdleNow(): Boolean = isIdle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    fun setIdle(idle: Boolean) {
        isIdle = idle
        if (idle) {
            callback?.onTransitionToIdle()
        }
    }
}

/**
 * Custom test rule for handling async operations
 */
class AsyncTestRule : androidx.test.rule.ActivityTestRule<Nothing>(Nothing::class.java) {
    private val idlingResource = AsyncOperationIdlingResource("AsyncOperation")

    fun setAsyncOperationIdle(idle: Boolean) {
        idlingResource.setIdle(idle)
    }

    fun getIdlingResource(): IdlingResource = idlingResource
}
