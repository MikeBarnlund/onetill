package com.onetill.shared.setup

import com.onetill.shared.data.AppResult
import com.onetill.shared.data.model.ConnectionStatus
import com.onetill.shared.data.model.StoreConfig
import com.onetill.shared.ecommerce.ECommerceBackend
import com.onetill.shared.fake.FakeECommerceBackend
import com.onetill.shared.fake.FakeLocalDataSource
import com.onetill.shared.fake.testStoreConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SetupManagerTest {

    private val fakeLocal = FakeLocalDataSource()
    private val fakeBackend = FakeECommerceBackend()
    private val testDispatcher = UnconfinedTestDispatcher()

    private var lastFactoryConfig: StoreConfig? = null

    private fun createSetupManager(): SetupManager {
        lastFactoryConfig = null
        return SetupManager(
            localDataSource = fakeLocal,
            backendFactory = { config ->
                lastFactoryConfig = config
                fakeBackend
            },
        )
    }

    // -- Initial state --

    @Test
    fun initialStateIsIdle() = runTest(testDispatcher) {
        val manager = createSetupManager()
        assertIs<SetupState.Idle>(manager.state.value)
    }

    @Test
    fun isSetupCompleteFalseWhenNoConfig() = runTest(testDispatcher) {
        val manager = createSetupManager()
        assertFalse(manager.isSetupComplete.first())
    }

    @Test
    fun isSetupCompleteTrueWhenConfigExists() = runTest(testDispatcher) {
        fakeLocal.saveStoreConfig(testStoreConfig())
        val manager = createSetupManager()
        assertTrue(manager.isSetupComplete.first())
    }

    // -- Validate success --

    @Test
    fun validateSuccessTransitionsToValidated() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.Connected("My Shop")
        fakeBackend.fetchStoreCurrencyResult = AppResult.Success("AUD")

        val manager = createSetupManager()
        manager.validateCredentials("https://myshop.com", "ck_key", "cs_secret")

        val state = manager.state.value
        assertIs<SetupState.Validated>(state)
        assertEquals("My Shop", state.storeName)
        assertEquals("AUD", state.currency)
    }

    @Test
    fun validateTrimsUrlAndKeys() = runTest(testDispatcher) {
        val manager = createSetupManager()
        manager.validateCredentials("  https://myshop.com/  ", "  ck_key  ", "  cs_secret  ")

        val config = lastFactoryConfig!!
        assertEquals("https://myshop.com", config.siteUrl)
        assertEquals("ck_key", config.consumerKey)
        assertEquals("cs_secret", config.consumerSecret)
    }

    // -- Validate errors --

    @Test
    fun validateInvalidCredentialsTransitionsToError() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.InvalidCredentials

        val manager = createSetupManager()
        manager.validateCredentials("https://myshop.com", "ck_bad", "cs_bad")

        val state = manager.state.value
        assertIs<SetupState.Error>(state)
        assertEquals("Invalid credentials", state.message)
    }

    @Test
    fun validateStoreNotFoundTransitionsToError() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.StoreNotFound

        val manager = createSetupManager()
        manager.validateCredentials("https://missing.com", "ck_key", "cs_secret")

        val state = manager.state.value
        assertIs<SetupState.Error>(state)
        assertEquals("Store not found", state.message)
    }

    @Test
    fun validateNetworkErrorTransitionsToError() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.NetworkError("Connection refused")

        val manager = createSetupManager()
        manager.validateCredentials("https://myshop.com", "ck_key", "cs_secret")

        val state = manager.state.value
        assertIs<SetupState.Error>(state)
        assertEquals("Connection refused", state.message)
    }

    @Test
    fun validateCurrencyFetchFailsTransitionsToError() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.Connected("My Shop")
        fakeBackend.fetchStoreCurrencyResult = AppResult.Error("Currency endpoint failed")

        val manager = createSetupManager()
        manager.validateCredentials("https://myshop.com", "ck_key", "cs_secret")

        val state = manager.state.value
        assertIs<SetupState.Error>(state)
        assertEquals("Currency endpoint failed", state.message)
    }

    // -- Save --

    @Test
    fun saveAfterValidationTransitionsToComplete() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.Connected("My Shop")
        fakeBackend.fetchStoreCurrencyResult = AppResult.Success("AUD")

        val manager = createSetupManager()
        manager.validateCredentials("https://myshop.com", "ck_key", "cs_secret")
        manager.saveConfiguration()

        val state = manager.state.value
        assertIs<SetupState.Complete>(state)
        assertEquals("https://myshop.com", state.config.siteUrl)
        assertEquals("AUD", state.config.currency)
    }

    @Test
    fun savePersistsConfigToDatabase() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.Connected("My Shop")
        fakeBackend.fetchStoreCurrencyResult = AppResult.Success("AUD")

        val manager = createSetupManager()
        manager.validateCredentials("https://myshop.com", "ck_key", "cs_secret")
        manager.saveConfiguration()

        assertEquals(1, fakeLocal.saveStoreConfigCalls)
        val saved = fakeLocal.getStoreConfig()!!
        assertEquals("https://myshop.com", saved.siteUrl)
        assertEquals("ck_key", saved.consumerKey)
        assertEquals("cs_secret", saved.consumerSecret)
        assertEquals("AUD", saved.currency)
    }

    @Test
    fun saveUpdatesIsSetupComplete() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.Connected("My Shop")
        fakeBackend.fetchStoreCurrencyResult = AppResult.Success("USD")

        val manager = createSetupManager()
        assertFalse(manager.isSetupComplete.first())

        manager.validateCredentials("https://myshop.com", "ck_key", "cs_secret")
        manager.saveConfiguration()

        assertTrue(manager.isSetupComplete.first())
    }

    @Test
    fun saveWithoutValidationThrows() = runTest(testDispatcher) {
        val manager = createSetupManager()
        assertFailsWith<IllegalStateException> {
            manager.saveConfiguration()
        }
    }

    // -- Clear --

    @Test
    fun clearDeletesConfigAndResetsToIdle() = runTest(testDispatcher) {
        fakeLocal.saveStoreConfig(testStoreConfig())
        val manager = createSetupManager()

        manager.clearConfiguration()

        assertIs<SetupState.Idle>(manager.state.value)
        assertEquals(1, fakeLocal.deleteStoreConfigCalls)
        assertNull(fakeLocal.getStoreConfig())
    }

    // -- Reset --

    @Test
    fun resetGoesToIdleWithoutDeletingDbConfig() = runTest(testDispatcher) {
        fakeLocal.saveStoreConfig(testStoreConfig())

        fakeBackend.validateConnectionResult = ConnectionStatus.Connected("My Shop")
        fakeBackend.fetchStoreCurrencyResult = AppResult.Success("USD")

        val manager = createSetupManager()
        manager.validateCredentials("https://myshop.com", "ck_key", "cs_secret")

        manager.reset()

        assertIs<SetupState.Idle>(manager.state.value)
        assertEquals(0, fakeLocal.deleteStoreConfigCalls)
        // DB config should still exist
        assertTrue(fakeLocal.getStoreConfig() != null)
    }

    @Test
    fun resetFromErrorGoesToIdle() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.InvalidCredentials

        val manager = createSetupManager()
        manager.validateCredentials("https://myshop.com", "ck_bad", "cs_bad")
        assertIs<SetupState.Error>(manager.state.value)

        manager.reset()
        assertIs<SetupState.Idle>(manager.state.value)
    }

    // -- Re-run setup --

    @Test
    fun reRunningSetupOverwritesPreviousConfig() = runTest(testDispatcher) {
        fakeBackend.validateConnectionResult = ConnectionStatus.Connected("Shop 1")
        fakeBackend.fetchStoreCurrencyResult = AppResult.Success("USD")

        val manager = createSetupManager()
        manager.validateCredentials("https://shop1.com", "ck_1", "cs_1")
        manager.saveConfiguration()

        // Re-run with different credentials
        fakeBackend.validateConnectionResult = ConnectionStatus.Connected("Shop 2")
        fakeBackend.fetchStoreCurrencyResult = AppResult.Success("EUR")

        manager.validateCredentials("https://shop2.com", "ck_2", "cs_2")
        manager.saveConfiguration()

        val state = manager.state.value
        assertIs<SetupState.Complete>(state)
        assertEquals("https://shop2.com", state.config.siteUrl)
        assertEquals("EUR", state.config.currency)

        val saved = fakeLocal.getStoreConfig()!!
        assertEquals("https://shop2.com", saved.siteUrl)
        assertEquals("EUR", saved.currency)
    }

    // -- Backend factory receives trimmed config --

    @Test
    fun backendFactoryReceivesTrimmedConfig() = runTest(testDispatcher) {
        val manager = createSetupManager()
        manager.validateCredentials("  https://myshop.com/  ", "  ck_key  ", "  cs_secret  ")

        val config = lastFactoryConfig!!
        assertEquals("https://myshop.com", config.siteUrl)
        assertEquals("ck_key", config.consumerKey)
        assertEquals("cs_secret", config.consumerSecret)
    }
}
