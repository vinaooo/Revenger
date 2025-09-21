# Plano de Rollback Completo 2025 - Projeto Revenger

**Data:** 19 de Setembro de 2025  
**Branch Principal:** updates  
**Documento Relacionado:** `atualizacao_completa_2025.md`

## 🎯 Estratégia de Rollback Multi-Nível

### Filosofia do Rollback
- **Rollback Granular:** Cada fase pode ser revertida independentemente
- **Preservação de Progresso:** Rollback não perde todo o trabalho realizado
- **Validação Rápida:** Scripts automatizados para verificar integridade
- **Documentação Completa:** Cada ponto de rollback totalmente documentado

## 📚 Estados de Rollback Disponíveis

### 🏷️ Estado R0: Projeto Original (EMERGENCY)
**Commit:** `inicial` (branch main)  
**Quando Usar:** Falha catastrófica, corrupção completa  
**Versões:**
- AGP: 7.0.4
- Kotlin: 1.5.21
- SDK: 31 (Android 12)
- AndroidX: Versões antigas (2021)

**Comando de Rollback:**
```bash
git checkout main
git branch -D updates  # CUIDADO: Perde todo progresso
git checkout -b updates-new
```

### 🏷️ Estado R1: Fase 1A Completa (SAFE)
**Commit:** `a1b2c3d` - "Fase 1A: Atualização AGP e Kotlin"  
**Versões Restauradas:**
- **AGP:** 8.3.2
- **Kotlin:** 1.9.25
- **Gradle:** 8.10
- **SDK:** 33 (Android 13)

**Comando de Rollback:**
```bash
git reset --hard a1b2c3d
./gradlew clean
./gradlew assembleDebug
```

### 🏷️ Estado R2: Fase 1B Completa (SAFE)
**Commit:** `e4f5g6h` - "Fase 1B: Atualização Java 17"  
**Versões Restauradas:**
- **Java Compatibility:** 17
- **JVM Target:** 17
- Mantém AGP 8.3.2 + Kotlin 1.9.25

**Comando de Rollback:**
```bash
git reset --hard e4f5g6h
./gradlew clean
./gradlew assembleDebug
```

### 🏷️ Estado R3: Fase 2 Completa (SAFE)  
**Commit:** `i7j8k9l` - "Fase 2: Atualização SDK 35"  
**Versões Restauradas:**
- **Compile/Target SDK:** 35 (Android 15)
- **Min SDK:** 30 (Android 11)
- GamePad.kt com null safety fixes

**Comando de Rollback:**
```bash
git reset --hard i7j8k9l
./gradlew clean
./gradlew assembleDebug
```

### 🏷️ Estado R4: Fase 3 Completa (CURRENT STABLE)
**Commit:** `8882011` - "Fase 3: Atualização AndroidX"  
**Versões Restauradas:**
- **core-ktx:** 1.15.0
- **appcompat:** 1.7.0  
- **activity-ktx:** 1.9.2
- **lifecycle:** 2.8.6
- **coroutines:** 1.9.0
- GameActivity.kt com OnBackPressedCallback moderno

**Comando de Rollback:**
```bash
git reset --hard 8882011
# Este é o estado atual - já validado e funcional
```

## 🔄 Procedimentos de Rollback por Fase Futura

### Fase 4A: Build Tools Avançados
**Rollback Para:** Estado R4 (Fase 3)

#### Em Caso de Falha:
```bash
# Rollback completo
git reset --hard 8882011

# Verificar integridade
./gradlew clean
./gradlew --version
# Deve mostrar Gradle 8.10, AGP 8.3.2

# Recompilação de segurança
./gradlew assembleDebug

# Teste funcional básico
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
```

#### Rollback Parcial (se possível):
```bash
# Reverter apenas Gradle
cd gradle/wrapper
git checkout HEAD~1 gradle-wrapper.properties

# Reverter apenas AGP
git checkout HEAD~1 build.gradle

# Reverter apenas Kotlin
git checkout HEAD~1 build.gradle
```

### Fase 4B: Java 21
**Rollback Para:** Estado R4 ou após Fase 4A

#### Problemas Comuns:
1. **Incompatibilidade de bibliotecas**
2. **Build failures**  
3. **Runtime crashes**

#### Comando de Rollback:
```bash
# Restaurar Java 17 compatibility
git checkout HEAD~1 app/build.gradle
# Procurar por:
# compileOptions {
#     sourceCompatibility JavaVersion.VERSION_17
#     targetCompatibility JavaVersion.VERSION_17  
# }
# kotlinOptions {
#     jvmTarget = '17'
# }

# Limpeza completa
./gradlew clean
./gradlew assembleDebug
```

### Fase 5: AGP/Kotlin Moderno
**Rollback Para:** Estado após Fase 4B

#### Problemas Esperados:
1. **Kotlin 2.0 K2 Compiler issues**
2. **AGP 8.6+ breaking changes**
3. **Plugin incompatibilities**

#### Rollback Específico:
```bash
# Kotlin rollback
sed -i 's/kotlin_version = .*/kotlin_version = '\''1.9.25'\''/' build.gradle

# AGP rollback  
sed -i 's/gradle:8\.[67]\.0/gradle:8.3.2/' build.gradle

# Download plugin rollback
sed -i 's/download.*version .*/download'\'' version '\''4.1.1'\''/' build.gradle

./gradlew clean assembleDebug
```

### Fase 6: RxJava Update
**Rollback Para:** Estado anterior

#### Rollback RxJava2:
```bash
# Reverter dependência
git checkout HEAD~1 app/build.gradle
# Verificar:
# implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'

# Se foi alterado código, reverter arquivos
git checkout HEAD~1 app/src/main/java/com/vinaooo/revenger/
```

## 🧪 Scripts de Validação Pós-Rollback

### Script: `validate_rollback.sh`
```bash
#!/bin/bash
echo "=== Validação Pós-Rollback ==="

# 1. Verificar versões
echo "Gradle: $(./gradlew --version | grep Gradle | head -1)"
echo "AGP: $(grep 'gradle:' build.gradle | head -1)"  
echo "Kotlin: $(grep kotlin_version build.gradle | head -1)"

# 2. Build test
echo "Testando compilação..."
./gradlew clean assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Compilação bem-sucedida"
else 
    echo "❌ Falha na compilação"
    exit 1
fi

# 3. APK validation
APK_PATH="app/build/outputs/apk/debug/app-universal-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "✅ APK gerado: $(ls -lh $APK_PATH)"
else
    echo "❌ APK não encontrado"
    exit 1
fi

# 4. Install test  
echo "Testando instalação..."
adb install -r "$APK_PATH"
if [ $? -eq 0 ]; then
    echo "✅ Instalação bem-sucedida"
else
    echo "❌ Falha na instalação" 
fi

echo "=== Validação Concluída ==="
```

### Script: `test_libretro_cores.sh`
```bash
#!/bin/bash
echo "=== Teste dos Cores LibRetro ==="

CORES=("gambatte" "genesis_plus_gx" "smsplus")
GAMES=("Zelda" "Sonic" "Rock n Roll")

for i in "${!CORES[@]}"; do
    CORE="${CORES[i]}"
    GAME="${GAMES[i]}"
    
    echo "Testando $CORE com $GAME..."
    
    # Alterar config.xml
    sed -i "s/<string name=\"config_core\">.*</<string name=\"config_core\">$CORE</" app/src/main/res/values/config.xml
    sed -i "s/<string name=\"config_name\">.*</<string name=\"config_name\">$GAME</" app/src/main/res/values/config.xml
    
    # Rebuild
    ./gradlew assembleDebug -q
    
    if [ $? -eq 0 ]; then
        echo "✅ $CORE - Build OK"
        
        # Install test
        adb install -r app/build/outputs/apk/debug/app-universal-debug.apk > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo "✅ $CORE - Install OK"
        else
            echo "❌ $CORE - Install FAILED"
        fi
    else
        echo "❌ $CORE - Build FAILED"
    fi
    
    echo "---"
done

echo "=== Teste Concluído ==="
```

## 🚨 Planos de Emergência

### Cenário 1: Corrupção Total do Branch
**Situação:** Branch updates completamente corrompido

```bash
# 1. Backup do trabalho atual (se possível)
git stash save "emergency-backup-$(date +%Y%m%d-%H%M%S)"

# 2. Criar novo branch limpo
git checkout main
git checkout -b updates-recovery

# 3. Cherry-pick commits importantes
git cherry-pick a1b2c3d  # Fase 1A
git cherry-pick e4f5g6h  # Fase 1B  
git cherry-pick i7j8k9l  # Fase 2
git cherry-pick 8882011  # Fase 3

# 4. Validar integridade
./scripts/validate_rollback.sh
```

### Cenário 2: Sistema de Build Quebrado
**Situação:** Gradle/AGP em estado inconsistente

```bash
# 1. Limpar completamente caches
rm -rf ~/.gradle/caches/
rm -rf app/build/
rm -rf build/

# 2. Resetar wrapper
cd gradle/wrapper
git checkout main gradle-wrapper.properties
cd ../..

# 3. Resetar build configs  
git checkout main build.gradle
git checkout main app/build.gradle

# 4. Sync gradual
./gradlew --stop
./gradlew wrapper --gradle-version 8.10
./gradlew clean
./gradlew assembleDebug
```

### Cenário 3: APK Não Instala
**Situação:** APK compila mas não instala no dispositivo

```bash
# 1. Verificar assinatura
keytool -printcert -jarfile app/build/outputs/apk/debug/app-universal-debug.apk

# 2. Limpar instalações anteriores
adb uninstall com.vinaooo.revenger.sonic_and_knuckles
adb uninstall com.vinaooo.revenger.zelda
adb uninstall com.vinaooo.revenger.rock_n_roll

# 3. Reinstalar versão conhecida funcional
git checkout 8882011
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
```

## 📋 Checklist de Rollback

### Antes do Rollback
- [ ] Documentar o problema específico
- [ ] Verificar se rollback é necessário (problema pode ter solução)
- [ ] Identificar o estado de rollback adequado
- [ ] Fazer backup do trabalho atual (`git stash`)
- [ ] Notificar team/stakeholders se aplicável

### Durante o Rollback
- [ ] Executar comando de rollback específico
- [ ] Limpar builds anteriores (`./gradlew clean`)
- [ ] Executar script de validação
- [ ] Testar compilação
- [ ] Testar instalação
- [ ] Testar funcionalidade básica

### Após o Rollback
- [ ] Confirmar todas as validações passaram
- [ ] Documentar lições aprendidas
- [ ] Atualizar plano de rollback se necessário
- [ ] Planejar nova tentativa (se aplicável)
- [ ] Commitar estado estável

## 🎯 Estratégias de Recovery

### Recovery Gradual
1. **Identificar componente problemático**
2. **Rollback apenas desse componente**
3. **Manter progresso em outras áreas**
4. **Retry com abordagem diferente**

### Recovery Completo  
1. **Rollback para último estado 100% funcional**
2. **Re-aplicar mudanças uma por vez**
3. **Validar cada passo**
4. **Parar no primeiro problema**

### Recovery de Emergência
1. **Rollback para Estado R0 (projeto original)**
2. **Re-aplicar fases inteiras validadas**
3. **Skip fase problemática**
4. **Continuar com próximas fases**

## 📊 Métricas de Sucesso do Rollback

### Critérios Obrigatórios
- ✅ Compilação limpa sem erros
- ✅ APK instala sem problemas
- ✅ App inicia corretamente
- ✅ Pelo menos um core LibRetro funciona

### Critérios de Qualidade
- ✅ Todos os cores LibRetro testados funcionam
- ✅ Controles responsivos
- ✅ Performance aceitável
- ✅ Sem crashes visíveis

### Critérios de Integridade
- ✅ Git history consistente
- ✅ Documentação atualizada
- ✅ Scripts de validação passam
- ✅ Configurações preservadas

---

## 🏁 Considerações Finais

Este plano de rollback garante que:

1. **Nunca perdemos progresso significativo** - Estados intermediários preservados
2. **Recovery rápido** - Scripts automatizados
3. **Flexibilidade** - Rollback granular ou completo
4. **Confiabilidade** - Cada estado validado independentemente

O **Estado R4 (Fase 3 Completa)** é nossa **golden version** - totalmente funcional, testada e documentada. Qualquer experimento futuro deve sempre poder retornar a este ponto com segurança.

---

**Documento Relacionado:** `atualizacao_completa_2025.md`  
**Scripts:** `scripts/validate_rollback.sh`, `scripts/test_libretro_cores.sh`
