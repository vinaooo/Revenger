package com.vinaooo.revenger.retroview

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.utils.ShaderType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for [RetroView]. The real [GLRetroView] constructor runs, but its setters that reach
 * the native library ([GLRetroView.frameSpeed], [GLRetroView.shader] and the lifecycle calls) are
 * stubbed through [mockkConstructor]. The ROM comes from a [ContextWrapper] whose [AssetManager]
 * is a mock, and [Storage]'s singleton is reset so each test gets this test's files directory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RetroView_test {

    private val romBytes = byteArrayOf(1, 2, 3, 4)
    private lateinit var appContext: Context
    private lateinit var assets: AssetManager
    private lateinit var context: Context
    private lateinit var appConfig: AppConfig

    @Before
    fun setUp() {
        resetStorageSingleton()
        appContext = ApplicationProvider.getApplicationContext()
        assets = mockk()
        every { assets.open("rom/test.rom") } answers { ByteArrayInputStream(romBytes) }
        context =
                object : ContextWrapper(appContext) {
                    override fun getAssets(): AssetManager = this@RetroView_test.assets
                }

        appConfig = mockk()
        every { appConfig.getRomName() } returns "test.rom"
        every { appConfig.getShader() } returns "sharp"
        every { appConfig.getVariables() } returns ""

        mockkConstructor(GLRetroView::class)
        every { anyConstructed<GLRetroView>().frameSpeed = any() } just runs
        every { anyConstructed<GLRetroView>().shader = any() } just runs
        every { anyConstructed<GLRetroView>().onResume() } just runs
        every { anyConstructed<GLRetroView>().onPause() } just runs
        every { anyConstructed<GLRetroView>().onDestroy() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        resetStorageSingleton()
    }

    private fun resetStorageSingleton() {
        Storage::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
    }

    private fun newRetroView(): RetroView = RetroView(context, CoroutineScope(Dispatchers.Unconfined), appConfig)

    private fun RetroView.data(): GLRetroViewData =
            RetroView::class.java.getDeclaredField("retroViewData").apply { isAccessible = true }.get(this)
                    as GLRetroViewData

    // --- ROM ---

    @Test
    fun `copia a rom dos assets para o arquivo de rom do Storage`() {
        val retroView = newRetroView()

        val storage = Storage.getInstance(appContext)
        assertArrayEquals(romBytes, storage.rom.readBytes())
        assertEquals(storage.rom.absolutePath, retroView.data().gameFilePath)
        assertEquals("libcore.so", retroView.data().coreFilePath)
    }

    @Test
    fun `sobrescreve uma rom antiga ja presente no Storage`() {
        Storage.getInstance(appContext).rom.writeBytes(byteArrayOf(9, 9, 9, 9, 9, 9))

        newRetroView()

        assertArrayEquals(romBytes, Storage.getInstance(appContext).rom.readBytes())
    }

    @Test
    fun `construir RetroView com rom ausente nos assets lanca IllegalArgumentException com a causa original`() {
        every { appConfig.getRomName() } returns "does_not_exist.rom"
        every { assets.open("rom/does_not_exist.rom") } throws FileNotFoundException("rom/does_not_exist.rom")

        val exception =
                try {
                    newRetroView()
                    null
                } catch (e: IllegalArgumentException) {
                    e
                }

        assertTrue(exception?.message?.contains("does_not_exist.rom") == true)
        assertTrue(exception?.cause is FileNotFoundException)
    }

    // --- Shader configurado ---

    @Test
    fun `usa o ShaderConfig de cada shader configurado`() {
        ShaderType.entries.forEach { type ->
            every { appConfig.getShader() } returns type.configName

            assertEquals(type.configName, type.toShaderConfig(), newRetroView().data().shader)
        }
    }

    @Test
    fun `o shader configurado ignora maiusculas e minusculas`() {
        every { appConfig.getShader() } returns "CRT"

        assertEquals(ShaderConfig.CRT, newRetroView().data().shader)
    }

    @Test
    fun `shader configurado desconhecido cai para Sharp`() {
        every { appConfig.getShader() } returns "shader_que_nao_existe"

        assertEquals(ShaderConfig.Sharp, newRetroView().data().shader)
    }

    // --- Variaveis do core ---

    @Test
    fun `sem variaveis configuradas o core recebe uma lista vazia`() {
        assertEquals(0, newRetroView().data().variables.size)
    }

    @Test
    fun `variaveis do core sao separadas por virgula e aparadas`() {
        every { appConfig.getVariables() } returns "core_a = one, core_b=two"

        val variables = newRetroView().data().variables

        assertEquals(listOf("core_a" to "one", "core_b" to "two"), variables.map { it.key to it.value })
    }

    @Test
    fun `entradas malformadas ou em branco sao ignoradas`() {
        every { appConfig.getVariables() } returns "sem_igual, ,core_a=one,"

        val variables = newRetroView().data().variables

        assertEquals(listOf("core_a" to "one"), variables.map { it.key to it.value })
    }

    @Test
    fun `um valor que contem igual e mantido inteiro`() {
        every { appConfig.getVariables() } returns "core_blob=YWJj=="

        val variables = newRetroView().data().variables

        assertEquals(listOf("core_blob" to "YWJj=="), variables.map { it.key to it.value })
    }

    // --- SRAM ---

    @Test
    fun `carrega a SRAM quando o arquivo existe`() {
        val sram = byteArrayOf(7, 8, 9)
        Storage.getInstance(appContext).sram.writeBytes(sram)

        assertArrayEquals(sram, newRetroView().data().saveRAMState)
    }

    @Test
    fun `sem arquivo de SRAM nao carrega nada`() {
        Storage.getInstance(appContext).sram.delete()

        assertNull(newRetroView().data().saveRAMState)
    }

    // --- View ---

    @Test
    fun `inicia a emulacao em velocidade normal e centralizada`() {
        val retroView = newRetroView()

        verify { anyConstructed<GLRetroView>().frameSpeed = 1 }
        assertTrue(retroView.view.preserveEGLContextOnPause)
        val params = retroView.view.layoutParams as android.widget.FrameLayout.LayoutParams
        assertEquals(android.view.Gravity.CENTER, params.gravity)
    }

    // --- Shader dinamico ---

    @Test
    fun `dynamicShader comeca como disabled`() {
        assertEquals("disabled", newRetroView().dynamicShader)
    }

    @Test
    fun `dynamicShader aplica o ShaderConfig do shader escolhido`() {
        val retroView = newRetroView()

        retroView.dynamicShader = "upscale3"

        assertEquals("upscale3", retroView.dynamicShader)
        verify { anyConstructed<GLRetroView>().shader = ShaderConfig.CUT3() }
    }

    @Test
    fun `dynamicShader desconhecido aplica Sharp`() {
        val retroView = newRetroView()

        retroView.dynamicShader = "shader_que_nao_existe"

        verify { anyConstructed<GLRetroView>().shader = ShaderConfig.Sharp }
    }

    // --- Listeners de frame ---

    @Test
    fun `frameRendered vira true depois do primeiro FrameRendered`() {
        val events = MutableSharedFlow<GLRetroView.GLRetroEvents>(extraBufferCapacity = 4)
        every { anyConstructed<GLRetroView>().getGLRetroEvents() } returns events
        val retroView = newRetroView()
        retroView.registerFrameRenderedListener()

        events.tryEmit(GLRetroView.GLRetroEvents.SurfaceCreated)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(retroView.frameRendered.value == true)

        events.tryEmit(GLRetroView.GLRetroEvents.FrameRendered)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(retroView.frameRendered.value == true)
    }

    @Test
    fun `frameRendered comeca false`() {
        assertEquals(false, newRetroView().frameRendered.value)
    }

    @Test
    fun `registerFrameCallback conta apenas eventos FrameRendered no profiler`() {
        mockkObject(AdvancedPerformanceProfiler)
        every { AdvancedPerformanceProfiler.onFrameRendered() } just runs
        every { anyConstructed<GLRetroView>().getGLRetroEvents() } returns
                flowOf(
                        GLRetroView.GLRetroEvents.SurfaceCreated,
                        GLRetroView.GLRetroEvents.FrameRendered,
                        GLRetroView.GLRetroEvents.FrameRendered
                )

        newRetroView().registerFrameCallback()

        verify(exactly = 2) { AdvancedPerformanceProfiler.onFrameRendered() }
    }

    // --- Ciclo de vida ---

    @Test
    fun `resume pause e destroy delegam para a GLRetroView`() {
        val retroView = newRetroView()

        retroView.resume()
        retroView.pause()
        retroView.destroy()

        verify(exactly = 1) { anyConstructed<GLRetroView>().onResume() }
        verify(exactly = 1) { anyConstructed<GLRetroView>().onPause() }
        verify(exactly = 1) { anyConstructed<GLRetroView>().onDestroy() }
    }
}
