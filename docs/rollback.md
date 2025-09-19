# Plano de Rollback Detalhado - Revenger Project

**Data:** 19 de Setembro de 2025  
**Branch:** minSdk  
**Documento:** Procedimentos de recuperação para falhas durante atualizações

---

## 🚨 **PROCEDIMENTOS DE EMERGÊNCIA**

### **🔴 ROLLBACK CRÍTICO - Falha Total do Projeto**

**Sintomas:**
- Projeto não compila
- Erros graves de dependências
- APK não é gerado
- App crasha ao iniciar

**Ação Imediata:**
```bash
# 1. Parar todas as operações
git stash  # salvar alterações pendentes

# 2. Voltar ao último commit estável
git checkout minSdk
git reset --hard HEAD

# 3. Limpar cache e build
./gradlew clean
rm -rf ~/.gradle/caches/
rm -rf app/build/
rm -rf build/

# 4. Verificar integridade
./gradlew build
```

**⏱️ Tempo estimado:** 5-10 minutos

---

## 📝 **ROLLBACKS POR FASE**

### **FASE 1 - Rollback AGP/Kotlin/Java**

#### **Cenário 1A: AGP não compila após atualização**

**Sintomas:**
```
> Could not resolve com.android.tools.build:gradle:8.7.2
> Unsupported method: BaseConfig.getApplicationIdSuffix()
```

**Procedimento:**
```bash
# 1. Reverter build.gradle (root)
git checkout HEAD -- build.gradle

# OU editar manualmente:
```

**Arquivo: `build.gradle` (root)**
```groovy
buildscript {
    ext.kotlin_version = '1.9.10'  // REVERTER de 2.0.20
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.0.2'  // REVERTER de 8.7.2
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```

**Validação:**
```bash
./gradlew clean
./gradlew build
```

---

#### **Cenário 1B: Java 17 causa problemas com LibRetro**

**Sintomas:**
```
> Unsupported class file major version 61
> Native library loading fails
```

**Procedimento:**

**Arquivo: `app/build.gradle`**
```groovy
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8  // REVERTER de VERSION_17
    targetCompatibility JavaVersion.VERSION_1_8  // REVERTER de VERSION_17
}
kotlinOptions {
    jvmTarget = '1.8'  // REVERTER de '17'
}
```

**Validação:**
```bash
./gradlew clean
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-universal-debug.apk
```

---

#### **Cenário 1C: Kotlin 2.0 quebra compilação**

**Sintomas:**
```
> Could not determine the dependencies of task ':app:compileDebugKotlin'
> K2 compiler errors
```

**Procedimento:**

**Opção 1 - Versão Intermediária:**
```groovy
ext.kotlin_version = '1.9.25'  // Intermediária entre 1.9.10 e 2.0.20
```

**Opção 2 - Rollback Completo:**
```groovy
ext.kotlin_version = '1.9.10'  // Versão original
```

**Validação:**
```bash
./gradlew clean
./gradlew compileDebugKotlin
```

---

### **FASE 2 - Rollback SDK Updates**

#### **Cenário 2A: SDK 35 causa problemas de permissões**

**Sintomas:**
```
> Permission denied errors
> Runtime crashes related to storage access
> LibRetro cores fail to load
```

**Procedimento:**

**Arquivo: `app/build.gradle`**
```groovy
android {
    compileSdk 33            // REVERTER de 35
    buildToolsVersion '33.0.2'  // REVERTER de '35.0.0'
    
    defaultConfig {
        targetSdk 33         // REVERTER de 35
        minSdk 30           // MANTER
        // outros valores inalterados
    }
}
```

**AndroidManifest.xml - Verificar se não há referências SDK 35:**
```xml
<!-- Remover se adicionado -->
<!-- <uses-sdk android:targetSdkVersion="35" /> -->
```

**Validação:**
```bash
./gradlew clean
./gradlew assembleDebug
# Testar carregamento de ROMs
```

---

#### **Cenário 2B: Build Tools 35 incompatíveis**

**Sintomas:**
```
> Build Tools 35.0.0 not found
> Aapt2 compilation errors
```

**Procedimento:**
```bash
# 1. Verificar Build Tools instalados
$ANDROID_HOME/tools/bin/sdkmanager --list | grep build-tools

# 2. Instalar Build Tools 33 se necessário
$ANDROID_HOME/tools/bin/sdkmanager "build-tools;33.0.2"

# 3. Reverter no build.gradle
```

**Arquivo: `app/build.gradle`**
```groovy
buildToolsVersion '33.0.2'  // REVERTER de '35.0.0'
```

---

### **FASE 3 - Rollback AndroidX Libraries**

#### **Cenário 3A: core-ktx incompatível**

**Sintomas:**
```
> Unresolved reference errors
> NoSuchMethodError at runtime
```

**Procedimento:**

**Arquivo: `app/build.gradle` - dependencies**
```groovy
dependencies {
    implementation 'androidx.core:core-ktx:1.7.0'  // REVERTER de 1.15.0
    // Manter outras dependências atualizadas se funcionarem
}
```

**Validação:**
```bash
./gradlew clean
./gradlew assembleDebug
```

---

#### **Cenário 3B: Lifecycle components quebram ViewModels**

**Sintomas:**
```
> ViewModelProvider crash
> LiveData observers not working
```

**Procedimento:**

**Arquivo: `app/build.gradle` - dependencies**
```groovy
dependencies {
    // Reverter todas as lifecycle libraries em bloco
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0'  // REVERTER de 2.8.6
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.4.0'   // REVERTER de 2.8.6
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.4.0'    // REVERTER de 2.8.6
}
```

**Validação:**
```bash
# Testar especificamente ViewModels
./gradlew clean
./gradlew assembleDebug
# Verificar se GameActivityViewModel funciona
```

---

#### **Cenário 3C: Coroutines incompatíveis**

**Sintomas:**
```
> Coroutine scope errors
> Threading issues
> RetroView async operations fail
```

**Procedimento:**

**Arquivo: `app/build.gradle` - dependencies**
```groovy
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.1-native-mt'  // REVERTER de 1.9.0
}
```

**Arquivo: Verificar código Kotlin - `RetroView.kt`**
```kotlin
// Se necessário, reverter sintaxe de coroutines
// De: launch { } 
// Para: GlobalScope.launch { }
```

---

### **FASE 4 - Rollback LibRetro Libraries**

#### **Cenário 4A: RadialGamePad nova versão incompatível**

**Sintomas:**
```
> Touch controls não respondem
> Layout de controles quebrado
> Crashes ao tocar na tela
```

**Procedimento:**

**Arquivo: `app/build.gradle` - dependencies**
```groovy
dependencies {
    implementation 'com.github.swordfish90:radialgamepad:0.6.0'  // REVERTER para versão original
}
```

**Validação:**
```bash
./gradlew clean
./gradlew assembleDebug
# Testar controles touch em jogo
```

---

#### **Cenário 4B: LibRetroDroid nova versão quebra cores**

**Sintomas:**
```
> Cores não carregam
> "Failed to load core" errors
> ROMs não iniciam
```

**Procedimento:**

**Arquivo: `app/build.gradle` - dependencies**
```groovy
dependencies {
    implementation 'com.github.swordfish90:libretrodroid:0.6.2'  // REVERTER para versão original
}
```

**Validação:**
```bash
./gradlew clean
./gradlew assembleDebug
# Testar todos os cores: bsnes, genesis_plus_gx, neocd, fbneo
```

---

## 🔧 **PROCEDIMENTOS DE RECUPERAÇÃO ESPECÍFICOS**

### **🗂️ Recuperação de Arquivos Corrompidos**

#### **Reverter arquivo específico:**
```bash
# Reverter um arquivo específico
git checkout HEAD -- app/build.gradle
git checkout HEAD -- build.gradle
git checkout HEAD -- app/src/main/AndroidManifest.xml
```

#### **Recuperar de commit específico:**
```bash
# Ver histórico de commits
git log --oneline -10

# Reverter para commit específico
git checkout <commit-hash> -- <arquivo>

# Exemplo:
git checkout 3f2d1a8 -- app/build.gradle
```

---

### **🧹 Limpeza Completa de Cache**

**Quando usar:** Erros persistentes, dependências corrompidas

```bash
# 1. Parar Android Studio
pkill -f studio

# 2. Limpar cache Gradle
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/

# 3. Limpar cache projeto
./gradlew clean
rm -rf app/build/
rm -rf build/
rm -rf .gradle/

# 4. Limpar cache Android Studio
rm -rf ~/.android/cache/
rm -rf ~/.AndroidStudio*/system/caches/

# 5. Invalidar cache IDE
# Android Studio -> File -> Invalidate Caches and Restart
```

---

### **📦 Reinstalação de Dependências**

**Quando usar:** Dependências corrompidas, problemas de resolução

```bash
# 1. Forçar re-download
./gradlew clean --refresh-dependencies

# 2. Deletar lock files
rm -f gradle.lockfile
rm -f gradle/dependency-locks/*.lockfile

# 3. Rebuild completo
./gradlew build --refresh-dependencies
```

---

## 🎯 **PROCEDIMENTOS DE TESTE PÓS-ROLLBACK**

### **✅ Checklist Básico (5 minutos)**
```bash
# 1. Compilação
./gradlew clean build

# 2. Geração APK
./gradlew assembleDebug

# 3. Instalação
adb install app/build/outputs/apk/debug/app-universal-debug.apk

# 4. App abre
adb shell am start -n com.vinaooo.revenger/com.vinaooo.revenger.GameActivity
```

### **✅ Checklist Funcional (15 minutos)**
```bash
# 1. Carregar um jogo simples (ex: Genesis)
# 2. Testar controles touch
# 3. Testar save/load state
# 4. Verificar performance
# 5. Testar rotação de tela
```

### **✅ Checklist Completo (30 minutos)**
```bash
# 1. Testar todos os cores disponíveis
# 2. Carregar diferentes tipos de ROM
# 3. Testar todos os controles
# 4. Verificar sistema de arquivos
# 5. Testar em diferentes resoluções
```

---

## 📊 **MATRIZ DE ROLLBACK RÁPIDO**

| Problema | Arquivo | Linha/Seção | Valor Original | Tempo |
|----------|---------|-------------|----------------|-------|
| AGP falha | `build.gradle` | classpath | `8.0.2` | 2min |
| Kotlin falha | `build.gradle` | kotlin_version | `1.9.10` | 2min |
| Java falha | `app/build.gradle` | sourceCompatibility | `VERSION_1_8` | 2min |
| SDK falha | `app/build.gradle` | compileSdk | `33` | 2min |
| Core-ktx falha | `app/build.gradle` | core-ktx | `1.7.0` | 3min |
| Lifecycle falha | `app/build.gradle` | lifecycle-* | `2.4.0` | 3min |
| Coroutines falha | `app/build.gradle` | coroutines-android | `1.5.1-native-mt` | 3min |
| RadialGamePad falha | `app/build.gradle` | radialgamepad | `0.6.0` | 3min |
| LibRetroDroid falha | `app/build.gradle` | libretrodroid | `0.6.2` | 5min |

---

## 🚨 **CENÁRIOS DE EMERGÊNCIA**

### **Cenário Crítico 1: Projeto não abre no Android Studio**

**Sintomas:**
- "Failed to sync project"
- "Gradle sync failed"
- IDE trava ao carregar

**Solução:**
```bash
# 1. Fechar Android Studio
pkill -f studio

# 2. Rollback completo
git stash
git checkout minSdk
git reset --hard HEAD

# 3. Limpar completamente
rm -rf .gradle/
rm -rf app/build/
rm -rf build/
rm -rf .idea/

# 4. Reabrir projeto
# Android Studio -> Open -> Selecionar pasta do projeto
```

---

### **Cenário Crítico 2: Git em estado inconsistente**

**Sintomas:**
- Merge conflicts não resolvíveis
- HEAD detached
- Working directory corrompido

**Solução:**
```bash
# 1. Salvar trabalho se possível
cp -r . ../revenger-backup-$(date +%Y%m%d_%H%M%S)

# 2. Reset completo
git fetch origin
git checkout minSdk
git reset --hard origin/minSdk
git clean -fdx

# 3. Verificar estado
git status
git log --oneline -5
```

---

### **Cenário Crítico 3: Dispositivo não consegue instalar APK**

**Sintomas:**
```
adb: failed to install: INSTALL_FAILED_VERSION_DOWNGRADE
INSTALL_FAILED_INVALID_APK
```

**Solução:**
```bash
# 1. Desinstalar versão atual
adb uninstall com.vinaooo.revenger.debug
adb uninstall com.vinaooo.revenger

# 2. Limpar dados
adb shell pm clear com.vinaooo.revenger.debug

# 3. Reinstalar
./gradlew clean assembleDebug
adb install app/build/outputs/apk/debug/app-universal-debug.apk
```

---

## 📋 **DOCUMENTAÇÃO DE ROLLBACKS REALIZADOS**

### **Template para registrar rollbacks:**

```markdown
## Rollback [Data] - [Hora]

**Problema:**
- Descrição do que deu errado

**Ações tomadas:**
- Lista de comandos executados
- Arquivos modificados

**Resultado:**
- ✅ Sucesso / ❌ Falhou

**Lições aprendidas:**
- O que evitar no futuro
- Melhorias no processo

**Tempo total:** X minutos
```

---

## ⚡ **COMANDOS RÁPIDOS DE EMERGÊNCIA**

```bash
# RESET TOTAL (usar com cuidado)
git stash && git checkout minSdk && git reset --hard HEAD && ./gradlew clean

# LIMPEZA COMPLETA
rm -rf .gradle/ app/build/ build/ && ./gradlew clean

# ROLLBACK RÁPIDO AGP
git checkout HEAD -- build.gradle && ./gradlew clean build

# ROLLBACK RÁPIDO APP
git checkout HEAD -- app/build.gradle && ./gradlew clean assembleDebug

# TESTE RÁPIDO
./gradlew clean assembleDebug && adb install app/build/outputs/apk/debug/app-universal-debug.apk
```

---

## 📞 **SUPORTE E RECURSOS**

### **Documentação de referência:**
- Android Gradle Plugin: https://developer.android.com/studio/releases/gradle-plugin
- Kotlin releases: https://kotlinlang.org/releases.html
- AndroidX releases: https://developer.android.com/jetpack/androidx/versions

### **Logs importantes:**
```bash
# Gradle build logs
./gradlew build --info --stacktrace

# Android logcat
adb logcat -v time | grep -E "(FATAL|ERROR|AndroidRuntime)"

# Verificar versões instaladas
./gradlew --version
```

---

**⚠️ IMPORTANTE:** Este documento deve ser mantido atualizado após cada rollback realizado. Registrar sempre as lições aprendidas para melhorar os procedimentos.

---

**Última atualização:** 19 de Setembro de 2025  
**Próxima revisão:** Após primeiro rollback executado  
**Status:** Procedimentos definidos, aguardando validação prática
