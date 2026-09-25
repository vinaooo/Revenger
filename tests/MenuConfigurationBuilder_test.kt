package com.vinaooo.revenger.ui.retromenu3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [MenuConfigurationBuilder]'s fluent API and build-time validation. */
class MenuConfigurationBuilder_test {

    private val itemA = MenuItem("a", "Item A")
    private val itemB = MenuItem("b", "Item B")
    private val itemC = MenuItem("c", "Item C")

    @Test
    fun `build monta a configuracao com todos os campos definidos`() {
        val config =
                MenuConfigurationBuilder.create()
                        .menuId("menu")
                        .title("Title")
                        .addItem(itemA)
                        .defaultSelectedIndex(0)
                        .allowNavigation(false)
                        .showBackButton(false)
                        .build()

        assertEquals("menu", config.menuId)
        assertEquals("Title", config.title)
        assertEquals(listOf(itemA), config.items)
        assertEquals(0, config.defaultSelectedIndex)
        assertFalse(config.allowNavigation)
        assertFalse(config.showBackButton)
    }

    @Test
    fun `addItem e as duas variantes de addItems acumulam itens na ordem`() {
        val config =
                MenuConfigurationBuilder.create()
                        .menuId("menu")
                        .title("Title")
                        .addItem(itemA)
                        .addItems(itemB)
                        .addItems(listOf(itemC))
                        .build()

        assertEquals(listOf(itemA, itemB, itemC), config.items)
    }

    @Test
    fun `build entrega uma copia dos itens que nao muda com adicoes posteriores`() {
        val builder = MenuConfigurationBuilder.create().menuId("menu").title("Title").addItem(itemA)

        val config = builder.build()
        builder.addItem(itemB)

        assertEquals(listOf(itemA), config.items)
    }

    @Test
    fun `build sem menuId lanca IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            MenuConfigurationBuilder.create().title("Title").addItem(itemA).build()
        }
    }

    @Test
    fun `build sem titulo lanca IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            MenuConfigurationBuilder.create().menuId("menu").addItem(itemA).build()
        }
    }

    @Test
    fun `build sem itens lanca IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            MenuConfigurationBuilder.create().menuId("menu").title("Title").build()
        }
    }

    @Test
    fun `createMainMenu gera uma configuracao valida com navegacao e botao voltar`() {
        val config = MenuConfigurationBuilder.createMainMenu().build()

        assertEquals("main_menu", config.menuId)
        assertTrue(config.items.isNotEmpty())
        assertTrue(config.allowNavigation)
        assertTrue(config.showBackButton)
    }
}
