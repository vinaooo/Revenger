#!/bin/bash

# Script auxiliar para testes da Fase 9
# Sistema Multi-Slot Save States - Revenger

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funções auxiliares
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

# Ler config_id do config.xml
get_config_id() {
    local config_id=$(grep -oP '(?<=<string name="config_id">)[^<]+' app/src/main/res/values/config.xml 2>/dev/null)
    if [ -z "$config_id" ]; then
        print_error "Não foi possível ler config_id do config.xml"
        exit 1
    fi
    echo "$config_id"
}

# Verificar se o app está instalado
check_app_installed() {
    local config_id=$1
    local package="com.vinaooo.revenger.$config_id"
    
    if adb shell pm list packages | grep -q "$package"; then
        return 0
    else
        return 1
    fi
}

# Menu principal
show_menu() {
    echo ""
    print_header "MENU DE TESTES - FASE 9"
    echo "1) Verificar ambiente de teste"
    echo "2) Build e instalar APK"
    echo "3) Limpar dados do app (reset completo)"
    echo "4) Criar save legado para teste de migração"
    echo "5) Ver estrutura de saves no device"
    echo "6) Ver metadata de um slot específico"
    echo "7) Iniciar app"
    echo "8) Ver logs em tempo real"
    echo "9) Capturar screenshot do device"
    echo "10) Executar testes unitários"
    echo "0) Sair"
    echo ""
    echo -n "Escolha uma opção: "
}

# 1. Verificar ambiente
check_environment() {
    print_header "VERIFICANDO AMBIENTE DE TESTE"
    
    # Verificar ADB
    if command -v adb &> /dev/null; then
        print_success "ADB instalado"
    else
        print_error "ADB não encontrado"
        return 1
    fi
    
    # Verificar device conectado
    if adb devices | grep -q "device$"; then
        print_success "Device conectado"
    else
        print_error "Nenhum device conectado"
        return 1
    fi
    
    # Ler config_id
    local config_id=$(get_config_id)
    print_success "Config ID: $config_id"
    
    # Verificar app instalado
    if check_app_installed "$config_id"; then
        print_success "App instalado: com.vinaooo.revenger.$config_id"
    else
        print_warning "App não instalado"
    fi
    
    # Verificar ROM configurada
    local rom=$(grep -oP '(?<=<string name="config_rom">)[^<]+' app/src/main/res/values/config.xml 2>/dev/null)
    if [ -n "$rom" ]; then
        print_success "ROM configurada: $rom"
    else
        print_warning "ROM não configurada"
    fi
    
    # Verificar testes unitários
    local test_count=$(find app/src/test -name "*.kt" -type f | wc -l)
    print_success "Testes unitários: $test_count arquivos"
    
    echo ""
    print_info "Ambiente pronto para testes!"
}

# 2. Build e instalar
build_and_install() {
    print_header "BUILD E INSTALAÇÃO"
    
    print_info "Compilando APK..."
    ./gradlew clean assembleDebug
    
    print_info "Instalando no device..."
    ./gradlew installDebug
    
    print_success "APK instalado com sucesso!"
}

# 3. Limpar dados
clear_app_data() {
    local config_id=$(get_config_id)
    local package="com.vinaooo.revenger.$config_id"
    
    print_header "LIMPANDO DADOS DO APP"
    print_warning "Isso irá apagar TODOS os saves e configurações!"
    echo -n "Confirma? (s/N): "
    read -r confirm
    
    if [[ "$confirm" == "s" || "$confirm" == "S" ]]; then
        adb shell pm clear "$package"
        print_success "Dados limpos!"
    else
        print_info "Operação cancelada"
    fi
}

# 4. Criar save legado
create_legacy_save() {
    local config_id=$(get_config_id)
    local package="com.vinaooo.revenger.$config_id"
    local data_dir="/data/data/$package/files"
    
    print_header "CRIANDO SAVE LEGADO"
    
    if ! check_app_installed "$config_id"; then
        print_error "App não está instalado. Execute opção 2 primeiro."
        return 1
    fi
    
    print_info "Criando arquivo de save legado..."
    adb shell "echo 'Legacy save state data for testing migration' > $data_dir/state"
    
    if adb shell "[ -f $data_dir/state ] && echo 'exists'" | grep -q "exists"; then
        print_success "Save legado criado em: $data_dir/state"
        print_info "Reinicie o app para testar a migração"
    else
        print_error "Falha ao criar save legado"
    fi
}

# 5. Ver estrutura de saves
show_save_structure() {
    local config_id=$(get_config_id)
    local package="com.vinaooo.revenger.$config_id"
    local saves_dir="/data/data/$package/files/saves"
    
    print_header "ESTRUTURA DE SAVES"
    
    if ! adb shell "[ -d $saves_dir ] && echo 'exists'" | grep -q "exists"; then
        print_warning "Pasta de saves não existe ainda"
        print_info "Crie um save primeiro no emulador"
        return
    fi
    
    echo ""
    adb shell "ls -laR $saves_dir"
    echo ""
    
    # Contar slots ocupados
    local occupied=$(adb shell "ls -d $saves_dir/slot_* 2>/dev/null | wc -l" | tr -d ' \r\n')
    print_info "Slots ocupados: $occupied/9"
}

# 6. Ver metadata de slot
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

# 7. Iniciar app
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

# 8. Ver logs
show_logs() {
    print_header "LOGS EM TEMPO REAL"
    print_info "Pressione Ctrl+C para parar"
    echo ""
    
    adb logcat | grep -E "(Revenger|SaveStateManager|SaveSlot|RetroMenu3|Screenshot)"
}

# 9. Capturar screenshot
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

# 10. Executar testes unitários
run_unit_tests() {
    print_header "EXECUTANDO TESTES UNITÁRIOS"
    
    ./gradlew testDebugUnitTest
    
    echo ""
    print_info "Relatório HTML disponível em:"
    print_info "app/build/reports/tests/testDebugUnitTest/index.html"
}

# Loop principal
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

# Executar
main
