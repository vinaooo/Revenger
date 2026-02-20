#!/bin/bash

# Auxiliary script for Phase 9 tests
# Multi-Slot Save States System - Revenger

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
    echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Read config_id from config.xml
get_config_id() {
    local config_id=$(grep -oP '(?<=<string name="config_id">)[^<]+' app/src/main/res/values/config.xml 2>/dev/null)
    if [ -z "$config_id" ]; then
        print_error "Could not read config_id from config.xml"
        exit 1
    fi
    echo "$config_id"
}

# Check if the app is installed
check_app_installed() {
    local config_id=$1
    local package="com.vinaooo.revenger.$config_id"
    
    if adb shell pm list packages | grep -q "$package"; then
        return 0
    else
        return 1
    fi
}

# Main menu
show_menu() {
    echo ""
    print_header "TEST MENU - PHASE 9"
    echo "1) Check test environment"
    echo "2) Build and install APK"
    echo "3) Clear app data (full reset)"
    echo "4) Create legacy save for migration test"
    echo "5) View save structure on device"
    echo "6) View metadata of a specific slot"
    echo "7) Launch app"
    echo "8) View real-time logs"
    echo "9) Capture screenshot from device"
    echo "10) Run unit tests"
    echo "0) Exit"
    echo ""
    echo -n "Choose an option: "
}

# 1. Check environment
check_environment() {
    print_header "CHECKING TEST ENVIRONMENT"
    
    # Verificar ADB
    if command -v adb &> /dev/null; then
        print_success "ADB installed"
    else
        print_error "ADB not found"
        return 1
    fi
    
    # Verificar device conectado
    if adb devices | grep -q "device$"; then
        print_success "Device connected"
    else
        print_error "No device connected"
        return 1
    fi
    
    # Ler config_id
    local config_id=$(get_config_id)
    print_success "Config ID: $config_id"
    
    # Verificar app instalado
    if check_app_installed "$config_id"; then
        print_success "App installed: com.vinaooo.revenger.$config_id"
    else
        print_warning "App not installed"
    fi
    
    # Verificar ROM configurada
    local rom=$(grep -oP '(?<=<string name="config_rom">)[^<]+' app/src/main/res/values/config.xml 2>/dev/null)
    if [ -n "$rom" ]; then
        print_success "ROM configured: $rom"
    else
        print_warning "ROM not configured"
    fi
    
    # Check unit tests
    local test_count=$(find app/src/test -name "*.kt" -type f | wc -l)
    print_success "Unit tests: $test_count files"
    
    echo ""
    print_info "Environment ready for tests!"
}

# 2. Build and install
build_and_install() {
    print_header "BUILD AND INSTALL"
    
    print_info "Building APK..."
    ./gradlew clean assembleDebug
    
    print_info "Installing on device..."
    ./gradlew installDebug
    
    print_success "APK installed successfully!"
}

# 3. Clear data
clear_app_data() {
    local config_id=$(get_config_id)
    local package="com.vinaooo.revenger.$config_id"
    
    print_header "CLEARING APP DATA"
    print_warning "This will delete ALL saves and settings!"
    echo -n "Confirm? (y/N): "
    read -r confirm
    
    if [[ "$confirm" == "s" || "$confirm" == "S" ]]; then
        adb shell pm clear "$package"
        print_success "Dados limpos!"
    else
        print_info "Operation cancelled"
    fi
}

# 4. Create legacy save
create_legacy_save() {
    local config_id=$(get_config_id)
    local package="com.vinaooo.revenger.$config_id"
    local data_dir="/data/data/$package/files"
    
    print_header "CREATING LEGACY SAVE"
    
    if ! check_app_installed "$config_id"; then
        print_error "App is not installed. Run option 2 first."
        return 1
    fi
    
    print_info "Creating legacy save file..."
    adb shell "echo 'Legacy save state data for testing migration' > $data_dir/state"
    
    if adb shell "[ -f $data_dir/state ] && echo 'exists'" | grep -q "exists"; then
        print_success "Legacy save created at: $data_dir/state"
        print_info "Restart the app to test migration"
    else
        print_error "Failed to create legacy save"
    fi
}

# 5. View save structure
show_save_structure() {
    local config_id=$(get_config_id)
    local package="com.vinaooo.revenger.$config_id"
    local saves_dir="/data/data/$package/files/saves"
    
    print_header "SAVE STRUCTURE"
    
    if ! adb shell "[ -d $saves_dir ] && echo 'exists'" | grep -q "exists"; then
        print_warning "Saves folder does not exist yet"
        print_info "Create a save first in the emulator"
        return
    fi
    
    echo ""
    adb shell "ls -laR $saves_dir"
    echo ""
    
    # Contar slots ocupados
    local occupied=$(adb shell "ls -d $saves_dir/slot_* 2>/dev/null | wc -l" | tr -d ' \r\n')
    print_info "Slots ocupados: $occupied/9"
}

# 6. View slot metadata
show_slot_metadata() {
    local config_id=$(get_config_id)
    local package="com.vinaooo.revenger.$config_id"
    
    print_header "METADATA DE SLOT"
    echo -n "Número do slot (1-9): "
    read -r slot_num
    
    if [[ ! "$slot_num" =~ ^[1-9]$ ]]; then
        print_error "Número inválido. Use 1-9."
        return 1
    fi
    
    local metadata_file="/data/data/$package/files/saves/slot_$slot_num/metadata.json"
    
    if adb shell "[ -f $metadata_file ] && echo 'exists'" | grep -q "exists"; then
        echo ""
        adb shell "cat $metadata_file" | python3 -m json.tool 2>/dev/null || adb shell "cat $metadata_file"
        echo ""
    else
        print_warning "Slot $slot_num está vazio ou não existe"
    fi
}

# 7. Launch app
start_app() {
    local config_id=$(get_config_id)
    local package="com.vinaooo.revenger.$config_id"
    
    print_header "INICIANDO APP"
    
    # Parar primeiro
    adb shell am force-stop "$package"
    
    # Iniciar activity
    adb shell am start -n "$package/.views.GameActivity"
    
    print_success "App iniciado!"
    print_info "Use SELECT+START para abrir o menu"
}

# 8. View logs
show_logs() {
    print_header "LOGS EM TEMPO REAL"
    print_info "Pressione Ctrl+C para parar"
    echo ""
    
    adb logcat | grep -E "(Revenger|SaveStateManager|SaveSlot|RetroMenu3|Screenshot)"
}

# 9. Capture screenshot
capture_screenshot() {
    print_header "CAPTURAR SCREENSHOT"
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local filename="test_screenshot_$timestamp.png"
    
    print_info "Capturando screenshot..."
    adb exec-out screencap -p > "$filename"
    
    if [ -f "$filename" ]; then
        print_success "Screenshot salvo: $filename"
    else
        print_error "Falha ao capturar screenshot"
    fi
}

# 10. Run unit tests
run_unit_tests() {
    print_header "EXECUTANDO TESTES UNITÁRIOS"
    
    ./gradlew testDebugUnitTest
    
    echo ""
    print_info "Relatório HTML disponível em:"
    print_info "app/build/reports/tests/testDebugUnitTest/index.html"
}

# Main loop
main() {
    while true; do
        show_menu
        read -r option
        
        case $option in
            1) check_environment ;;
            2) build_and_install ;;
            3) clear_app_data ;;
            4) create_legacy_save ;;
            5) show_save_structure ;;
            6) show_slot_metadata ;;
            7) start_app ;;
            8) show_logs ;;
            9) capture_screenshot ;;
            10) run_unit_tests ;;
            0) 
                print_info "Encerrando..."
                exit 0
                ;;
            *)
                print_error "Opção inválida"
                ;;
        esac
        
        echo ""
        echo -n "Pressione ENTER para continuar..."
        read -r
    done
}

# Execute
main
