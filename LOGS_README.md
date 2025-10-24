# Scripts de Captura de Logs para Debug de Navegação de Menus

## Scripts Disponíveis

### 1. `capture_menu_logs.sh` - Captura Completa
Script completo que:
- Compila e instala o APK com configuração específica
- Limpa logs anteriores
- Inicia captura de logs
- Executa o aplicativo
- Fornece instruções para teste manual
- Salva logs em arquivo timestamped

**Uso:**
```bash
./capture_menu_logs.sh [config_file]
```

**Exemplos:**
```bash
./capture_menu_logs.sh config_sonic_ms.xml    # Padrão
./capture_menu_logs.sh config_zelda_gb.xml
```

### 2. `capture_logs_only.sh` - Captura Simples
Script simples que apenas captura logs por um período determinado.
Assume que o app já está instalado.

**Uso:**
```bash
./capture_logs_only.sh [duração_em_segundos]
```

**Exemplos:**
```bash
./capture_logs_only.sh        # 30 segundos (padrão)
./capture_logs_only.sh 60     # 60 segundos
```

## Como Usar para Debug

### Método 1: Captura Completa (Recomendado)
1. Execute: `./capture_menu_logs.sh`
2. Siga as instruções na tela
3. Teste a navegação problemática
4. Pressione ENTER quando terminar
5. Analise o arquivo de log gerado

### Método 2: Captura Simples
1. Certifique-se que o app está instalado
2. Execute: `./capture_logs_only.sh 45`
3. Execute o app manualmente
4. Teste a navegação problemática dentro dos 45 segundos
5. Analise o arquivo de log gerado

## Arquivos de Log Gerados

Os logs são salvos com nome: `menu_logs_YYYYMMDD_HHMMSS.log` ou `menu_navigation_test_YYYYMMDD_HHMMSS.log`

## Filtros de Log Importantes

Para analisar os logs, use estes comandos:

```bash
# Ver todos os logs de restauração
grep "\[RESTORE\]" menu_logs_*.log

# Ver mudanças de estado
grep "\[STATE_CHANGE\]" menu_logs_*.log

# Ver fechamento de submenus
grep "\[CLOSE_SUBMENU\]" menu_logs_*.log

# Ver interceptação de DPAD
grep "\[INTERCEPT\]" menu_logs_*.log

# Ver navegação
grep "\[NAV\]" menu_logs_*.log

# Ver re-registro de fragments
grep "\[RESUME\]" menu_logs_*.log
```

## Problema Específico a Investigar

Quando voltar do "Core Variables" para "About", verificar:

1. Se `[CLOSE_SUBMENU]` aparece
2. Se `[RESTORE]` mostra mudança de `CORE_VARIABLES_MENU` → `ABOUT_MENU`
3. Se `[STATE_CHANGE]` processa a mudança corretamente
4. Se `[RESUME]` mostra re-registro do AboutFragment
5. Se `[INTERCEPT]` ainda funciona após voltar

## Configurações Disponíveis

- `config_sonic_ms.xml` - Sonic Master System
- `config_sonic_md.xml` - Sonic Mega Drive
- `config_zelda_gb.xml` - Zelda Game Boy
- `config_rock_snes.xml` - Rockman SNES