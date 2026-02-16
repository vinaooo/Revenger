package com.vinaooo.revenger.ui.retromenu3.navigation

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ExitSaveGridFragment
import com.vinaooo.revenger.ui.retromenu3.ManageSavesFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
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
        Log.d(TAG, "[SHOW] Menu type: $menuType")

        when (menuType) {
            MenuType.MAIN -> showMainMenu()
            MenuType.SETTINGS -> showSettingsMenu()
            MenuType.PROGRESS -> showProgressMenu()
            MenuType.ABOUT -> showAboutMenu()
            MenuType.EXIT -> showExitMenu()
            MenuType.CORE_VARIABLES -> {
                Log.w(TAG, "[SHOW] Core Variables menu not yet implemented")
            }
            MenuType.SAVE_SLOTS -> showSaveSlotsMenu()
            MenuType.LOAD_SLOTS -> showLoadSlotsMenu()
            MenuType.MANAGE_SAVES -> showManageSavesMenu()
            MenuType.EXIT_SAVE_SLOTS -> showExitSaveSlotsMenu()
        }
    }

    private fun showMainMenu() {
        Log.d(TAG, "[SHOW] Main menu")

        // Verificar se RetroMenu3Fragment já existe
        val existingFragment = fragmentManager.findFragmentByTag(TAG_MAIN_MENU)
        if (existingFragment != null && existingFragment.isAdded) {
            Log.d(TAG, "[SHOW] Main menu already visible")
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

        // Diagnostic: confirm fragment was added
        try {
            val found = fragmentManager.findFragmentByTag(TAG_MAIN_MENU)
            Log.d(TAG, "[SHOW] Main menu added - fragment=${found?.javaClass?.simpleName} isAdded=${found?.isAdded} backStack=${fragmentManager.backStackEntryCount}")
        } catch (t: Throwable) {
            Log.w(TAG, "[SHOW] failed to log fragment state after add", t)
        }

        Log.d(TAG, "[SHOW] Main menu added successfully (synchronous)")
    }

    private fun showSettingsMenu() {
        Log.d(TAG, "[SHOW] Settings menu")

        val settingsFragment = SettingsMenuFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, settingsFragment, TAG_SETTINGS_MENU)
                .addToBackStack(TAG_SETTINGS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Settings menu added successfully")
    }

    private fun showProgressMenu() {
        Log.d(TAG, "[SHOW] Progress menu")

        // Clear any existing grid submenus from backstack first
        while (fragmentManager.backStackEntryCount > 1) {
            fragmentManager.popBackStackImmediate()
        }

        val progressFragment = ProgressFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, progressFragment, TAG_PROGRESS_MENU)
                .addToBackStack(TAG_PROGRESS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Progress menu added successfully")
    }

    private fun showAboutMenu() {
        Log.d(TAG, "[SHOW] About menu")

        val aboutFragment = AboutFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, aboutFragment, TAG_ABOUT_MENU)
                .addToBackStack(TAG_ABOUT_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] About menu added successfully")
    }

    private fun showExitMenu() {
        Log.d(TAG, "[SHOW] Exit menu")

        val exitFragment = ExitFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, exitFragment, TAG_EXIT_MENU)
                .addToBackStack(TAG_EXIT_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Exit menu added successfully")
    }

    private fun showSaveSlotsMenu() {
        Log.d(TAG, "[SHOW] Save Slots menu")

        val saveSlotsFragment = SaveSlotsFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, saveSlotsFragment, TAG_SAVE_SLOTS_MENU)
                .addToBackStack(TAG_SAVE_SLOTS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Save Slots menu added successfully")
    }

    private fun showLoadSlotsMenu() {
        Log.d(TAG, "[SHOW] Load Slots menu")

        val loadSlotsFragment = LoadSlotsFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, loadSlotsFragment, TAG_LOAD_SLOTS_MENU)
                .addToBackStack(TAG_LOAD_SLOTS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Load Slots menu added successfully")
    }

    private fun showManageSavesMenu() {
        Log.d(TAG, "[SHOW] Manage Saves menu")

        val manageSavesFragment = ManageSavesFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, manageSavesFragment, TAG_MANAGE_SAVES_MENU)
                .addToBackStack(TAG_MANAGE_SAVES_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Manage Saves menu added successfully")
    }
    private fun showExitSaveSlotsMenu() {
        Log.d(TAG, "[SHOW] Exit Save Slots menu")

        val exitSaveGridFragment = ExitSaveGridFragment.newInstance()

        fragmentManager
                .beginTransaction()
                .replace(MENU_CONTAINER_ID, exitSaveGridFragment, TAG_EXIT_SAVE_SLOTS_MENU)
                .addToBackStack(TAG_EXIT_SAVE_SLOTS_MENU)
                .commitAllowingStateLoss()

        Log.d(TAG, "[SHOW] Exit Save Slots menu added successfully")
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
        Log.d(TAG, "[HIDE] Hiding current menu")

        // Encontrar fragment atual no container
        val currentFragment = fragmentManager.findFragmentById(MENU_CONTAINER_ID)

        if (currentFragment != null && currentFragment.isAdded) {
            try {
                fragmentManager.beginTransaction().remove(currentFragment).commitAllowingStateLoss()
                Log.d(TAG, "[HIDE] Menu remove requested for fragment=${currentFragment.javaClass.simpleName}")

                // Verify state after transaction (best-effort)
                val foundAfter = fragmentManager.findFragmentById(MENU_CONTAINER_ID)
                Log.d(TAG, "[HIDE] After remove: fragmentById=${foundAfter?.javaClass?.simpleName} backStack=${fragmentManager.backStackEntryCount}")
            } catch (t: Throwable) {
                Log.e(TAG, "[HIDE] Exception while removing fragment", t)
            }
        } else {
            Log.w(TAG, "[HIDE] No menu to hide (currentFragment=null or not added)")
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
        Log.d(TAG, "[BACK] Navigating back")

        // Verificar se há submenu ativo (back stack não vazio)
        if (fragmentManager.backStackEntryCount > 0) {
            // Fazer pop da back stack (volta ao menu anterior)
            fragmentManager.popBackStackImmediate()
            Log.d(TAG, "[BACK] Popped back stack successfully")
            return true
        } else {
            Log.d(TAG, "[BACK] Already at root menu")
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
        private const val TAG_SAVE_SLOTS_MENU = "SaveSlotsFragment"
        private const val TAG_LOAD_SLOTS_MENU = "LoadSlotsFragment"
        private const val TAG_MANAGE_SAVES_MENU = "ManageSavesFragment"
        private const val TAG_EXIT_SAVE_SLOTS_MENU = "ExitSaveGridFragment"

        /**
         * ID do container onde os fragments de menu são exibidos. Este é o FrameLayout definido em
         * activity_game.xml.
         */
        private val MENU_CONTAINER_ID = R.id.menu_container
    }
}
