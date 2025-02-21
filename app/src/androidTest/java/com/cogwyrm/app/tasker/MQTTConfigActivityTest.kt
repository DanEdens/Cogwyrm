package com.cogwyrm.app.tasker

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cogwyrm.app.R
import com.cogwyrm.app.test.AsyncTestRule
import com.cogwyrm.app.test.MockMQTTService
import com.cogwyrm.app.test.TestUtils.hasTextInputLayoutErrorText
import com.cogwyrm.app.test.TestUtils.setTextInTextInput
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MQTTConfigActivityTest {

    @get:Rule
    val asyncRule = AsyncTestRule()

    private lateinit var mockService: MockMQTTService
    private lateinit var scenario: ActivityScenario<MQTTConfigActivity>

    @Before
    fun setup() {
        mockService = MockMQTTService()
        IdlingRegistry.getInstance().register(asyncRule.getIdlingResource())
        scenario = ActivityScenario.launch(MQTTConfigActivity::class.java)
    }

    @After
    fun cleanup() {
        IdlingRegistry.getInstance().unregister(asyncRule.getIdlingResource())
        scenario.close()
    }

    @Test
    fun testInitialState() {
        // Check default port value
        onView(withId(R.id.portInput))
            .check(matches(withText("1883")))

        // Check QoS spinner default value
        onView(withId(R.id.qosSpinner))
            .check(matches(withSpinnerText("QoS 1 (At least once)")))

        // Check switches default state
        onView(withId(R.id.retainedSwitch))
            .check(matches(not(isChecked())))
        onView(withId(R.id.useSslSwitch))
            .check(matches(not(isChecked())))

        // Check status text is empty
        onView(withId(R.id.statusText))
            .check(matches(withText("")))
    }

    @Test
    fun testValidationErrors() {
        // Try to test connection with empty fields
        onView(withId(R.id.testButton)).perform(click())

        // Check error messages
        onView(withId(R.id.brokerUrlLayout))
            .check(matches(hasTextInputLayoutErrorText("Broker URL is required")))
        onView(withId(R.id.topicLayout))
            .check(matches(hasTextInputLayoutErrorText("Topic is required")))
        onView(withId(R.id.messageLayout))
            .check(matches(hasTextInputLayoutErrorText("Message is required")))
    }

    @Test
    fun testSuccessfulConnection() {
        // Fill in required fields
        onView(withId(R.id.brokerUrlInput))
            .perform(setTextInTextInput("test.mosquitto.org"))
        onView(withId(R.id.topicInput))
            .perform(setTextInTextInput("test/topic"))
        onView(withId(R.id.messageInput))
            .perform(setTextInTextInput("test message"))

        // Configure mock service
        mockService.setConnectionShouldFail(false)
        mockService.setPublishShouldFail(false)

        // Test connection
        asyncRule.setAsyncOperationIdle(false)
        onView(withId(R.id.testButton)).perform(click())

        // Wait for async operation
        asyncRule.setAsyncOperationIdle(true)

        // Verify success message
        onView(withId(R.id.statusText))
            .check(matches(withText("Test successful! Message published.")))

        // Verify mock service received correct values
        assert(mockService.lastTopic == "test/topic")
        assert(mockService.lastMessage == "test message")
        assert(mockService.lastQos == 1)
        assert(mockService.lastRetained == false)
    }

    @Test
    fun testConnectionFailure() {
        // Fill in required fields
        onView(withId(R.id.brokerUrlInput))
            .perform(setTextInTextInput("test.mosquitto.org"))
        onView(withId(R.id.topicInput))
            .perform(setTextInTextInput("test/topic"))
        onView(withId(R.id.messageInput))
            .perform(setTextInTextInput("test message"))

        // Configure mock service to fail
        mockService.setConnectionShouldFail(true)

        // Test connection
        asyncRule.setAsyncOperationIdle(false)
        onView(withId(R.id.testButton)).perform(click())

        // Wait for async operation
        asyncRule.setAsyncOperationIdle(true)

        // Verify failure message
        onView(withId(R.id.statusText))
            .check(matches(withText("Error: Mock connection failure")))
    }

    @Test
    fun testPublishFailure() {
        // Fill in required fields
        onView(withId(R.id.brokerUrlInput))
            .perform(setTextInTextInput("test.mosquitto.org"))
        onView(withId(R.id.topicInput))
            .perform(setTextInTextInput("test/topic"))
        onView(withId(R.id.messageInput))
            .perform(setTextInTextInput("test message"))

        // Configure mock service
        mockService.setConnectionShouldFail(false)
        mockService.setPublishShouldFail(true)

        // Test connection
        asyncRule.setAsyncOperationIdle(false)
        onView(withId(R.id.testButton)).perform(click())

        // Wait for async operation
        asyncRule.setAsyncOperationIdle(true)

        // Verify failure message
        onView(withId(R.id.statusText))
            .check(matches(withText("Error: Mock publish failure")))
    }

    @Test
    fun testAdvancedConfiguration() {
        // Fill in all fields
        onView(withId(R.id.brokerUrlInput))
            .perform(setTextInTextInput("test.mosquitto.org"))
        onView(withId(R.id.topicInput))
            .perform(setTextInTextInput("test/topic"))
        onView(withId(R.id.messageInput))
            .perform(setTextInTextInput("test message"))
        onView(withId(R.id.usernameInput))
            .perform(setTextInTextInput("testuser"))
        onView(withId(R.id.passwordInput))
            .perform(setTextInTextInput("testpass"))
        onView(withId(R.id.useSslSwitch))
            .perform(click())
        onView(withId(R.id.retainedSwitch))
            .perform(click())
        onView(withId(R.id.qosSpinner))
            .perform(click())
        onView(withText("QoS 2 (Exactly once)"))
            .perform(click())

        // Configure mock service
        mockService.setConnectionShouldFail(false)
        mockService.setPublishShouldFail(false)

        // Test connection
        asyncRule.setAsyncOperationIdle(false)
        onView(withId(R.id.testButton)).perform(click())

        // Wait for async operation
        asyncRule.setAsyncOperationIdle(true)

        // Verify mock service received correct values
        assert(mockService.lastTopic == "test/topic")
        assert(mockService.lastMessage == "test message")
        assert(mockService.lastQos == 2)
        assert(mockService.lastRetained == true)
    }
}
