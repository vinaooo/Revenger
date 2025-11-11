package com.vinaooo.revenger.ui.retromenu3.navigation

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment

/**
 * Adapter que isola a lógica de transações de Fragments do NavigationController.
 *
 * Esta classe encapsula todas as operações de FragmentManager (show, hide, add, remove), permitindo
 * que o NavigationController trabalhe com conceitos de alto nível (MenuType) em vez de lidar
 * diretamente com Fragments e transações.
 *
 * RESPONSABILIDADES:
 * - Gerenciar transações de fragments (add/remove/show/hide)
 * - Mapear MenuType para Fragment classes
 * - Manter referências aos fragments ativos
 * - Garantir transações atômicas sem crashes
 *
 * IMPORTANTE: Esta classe NÃO contém lógica de navegação ou estado. Apenas executa as operações de
 * UI solicitadas pelo NavigationController.
 */
class FragmentNavigationAdapter(private val activity: FragmentActivity) {

    private val fragmentManager: FragmentManager = activity.supportFragmentManager

    /**
     * Exibe o menu especificado pelo MenuType.
     *
     * Esta operação:
     * 1. Cria o fragment apropriado se necessário
     * 2. Adiciona ao container (R.id.menu_container)
     * 3. Executa a transação com commitAllowingStateLoss()
     *
     * @param menuType O tipo de menu a ser exibido
     */
    fun showMenu(menuType: MenuType) {
        android.util.Log.d(TAG, "[SHOW] Menu type: $menuType")

        when (menuType) {
            MenuType.MAIN -> showMainMenu()
            MenuType.SETTINGS -> showSettingsMenu()
            MenuType.PROGRESS -> showProgressMenu()
            MenuType.ABOUT -> showAboutMenu()
            MenuType.EXIT -> showExitMenu()
            MenuType.CORE_VARIABLES -> {
                android.util.Log.w(TAG, "[SHOW] Core Variables menu not yet implemented")
            }
        }
    }

    private fun showMainMenu() {
        android.util.Log.d(TAG, "[SHOW] Main menu")

        // Verificar se RetroMenu3Fragment já existe
        val existingFragment = fragmentManager.findFragmentByTag(TAG_MAIN_MENU)
        if (existingFragment != null && existingFragment.isAdded) {
            android.util.Log.d(TAG, "[SHOW] Main menu already visible")
            return
        }

        // Criar novo RetroMenu3Fragment
        val mainFragment = RetroMenu3Fragment.newInstance()

        // BUGFIX: Usar commitNow() para garantir que fragment seja adicionado IMEDIATAMENTE
        // Isso previne multiple F12 presses antes do fragment estar ativo
        // commitAllowingStateLoss() é ASYNC - permite que isMenuActive() retorne false
        // mesmo depois de "added successfully", causando pause/resume imbalance
        fragmentManager
                .beginTransaction()
                .add(MENU_CONTAINER_ID, mainFragment, TAG_MAIN_MENU)
                .commitNow()

        android.util.Log.d(TAG, "[SHOW] Main menu added successfully (synchronous)")
    }

    private fun showSettingsMenu() {
        android.util.Log.d(TAG, "[SHOW] Settings menu")

        val settingsFragment = SettingsMenuFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, settingsFragment, TAG_SETTINGS_MENU)
                .addToBackStack(TAG_SETTINGS_MENU)
                .commitAllowingStateLoss()

        android.util.Log.d(TAG, "[SHOW] Settings menu added successfully")
    }

    private fun showProgressMenu() {
        android.util.Log.d(TAG, "[SHOW] Progress menu")

        val progressFragment = ProgressFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, progressFragment, TAG_PROGRESS_MENU)
                .addToBackStack(TAG_PROGRESS_MENU)
                .commitAllowingStateLoss()

        android.util.Log.d(TAG, "[SHOW] Progress menu added successfully")
    }

    private fun showAboutMenu() {
        android.util.Log.d(TAG, "[SHOW] About menu")

        val aboutFragment = AboutFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, aboutFragment, TAG_ABOUT_MENU)
                .addToBackStack(TAG_ABOUT_MENU)
                .commitAllowingStateLoss()

        android.util.Log.d(TAG, "[SHOW] About menu added successfully")
    }

    private fun showExitMenu() {
        android.util.Log.d(TAG, "[SHOW] Exit menu")

        val exitFragment = ExitFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, exitFragment, TAG_EXIT_MENU)
                .addToBackStack(TAG_EXIT_MENU)
                .commitAllowingStateLoss()

        android.util.Log.d(TAG, "[SHOW] Exit menu added successfully")
    }
    /**
     * Esconde o menu atual.
     *
     * Esta operação:
     * 1. Remove o fragment do container
     * 2. Executa a transação com commitAllowingStateLoss()
     *
     * IMPORTANTE: Não destroi o fragment, apenas o remove da tela. Isso preserva o estado para
     * possível restauração futura.
     */
    fun hideMenu() {
        android.util.Log.d(TAG, "[HIDE] Hiding current menu")

        // Encontrar fragment atual no container
        val currentFragment = fragmentManager.findFragmentById(MENU_CONTAINER_ID)

        if (currentFragment != null && currentFragment.isAdded) {
            fragmentManager.beginTransaction().remove(currentFragment).commitAllowingStateLoss()

            android.util.Log.d(TAG, "[HIDE] Menu removed successfully")
        } else {
            android.util.Log.w(TAG, "[HIDE] No menu to hide")
        }
    }

    /**
     * Navega para trás usando o FragmentManager back stack.
     *
     * Este método:
     * 1. Verifica se há fragmentos na back stack
     * 2. Remove o fragmento do topo (pop)
     * 2. Mostra o menu principal novamente
     * 3. Executa a transação com commitAllowingStateLoss()
     *
     * @return true se navegou para trás, false se já estava no menu raiz
     */
    fun navigateBack(): Boolean {
        android.util.Log.d(TAG, "[BACK] Navigating back")

        // Verificar se há submenu ativo (back stack não vazio)
        if (fragmentManager.backStackEntryCount > 0) {
            // Fazer pop da back stack (volta ao menu anterior)
            fragmentManager.popBackStackImmediate()
            android.util.Log.d(TAG, "[BACK] Popped back stack successfully")
            return true
        } else {
            android.util.Log.d(TAG, "[BACK] Already at root menu")
            return false
        }
    }

    /** Retorna o número de fragments na back stack. 0 = menu principal, >0 = em submenu */
    fun getBackStackCount(): Int {
        return fragmentManager.backStackEntryCount
    }

    companion object {
        private const val TAG = "FragmentNavigationAdapter"

        /** Tags para identificar fragments no FragmentManager */
        private const val TAG_MAIN_MENU = "RetroMenu3Fragment"
        private const val TAG_SETTINGS_MENU = "SettingsMenuFragment"
        private const val TAG_PROGRESS_MENU = "ProgressFragment"
        private const val TAG_ABOUT_MENU = "AboutFragment"
        private const val TAG_EXIT_MENU = "ExitFragment"

        /**
         * ID do container onde os fragments de menu são exibidos. Este é o FrameLayout definido em
         * activity_game.xml.
         */
        private val MENU_CONTAINER_ID = R.id.menu_container
    }
}
