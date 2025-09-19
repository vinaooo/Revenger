# Plano de Atualização - Revenger Project

**Data:** 19 de Setembro de 2025  
**Branch:** minSdk  
**Versão atual do projeto:** SDK 33, minSdk 30

---

## 📊 **Análise Completa de Versões Atuais**

### 🔴 **CRÍTICO - Atualizações Obrigatórias**

| Componente | Versão Atual | Versão Recomendada | Status | Prioridade |
|------------|--------------|-------------------|--------|------------|
| **AGP** | 8.0.2 | **8.7.2** | 🔴 Muito desatualizado | **ALTA** |
| **Kotlin** | 1.9.10 | **2.0.20** | 🔴 Muito desatualizado | **ALTA** |
| **compileSdk/targetSdk** | 33 | **35** (Android 15) | 🟡 Desatualizado | **MÉDIA** |
| **buildToolsVersion** | 33.0.2 | **35.0.0** | 🟡 Desatualizado | **MÉDIA** |

### 🟡 **MODERADO - AndroidX Libraries**

| Biblioteca | Versão Atual | Versão Recomendada | Diferença |
|------------|--------------|-------------------|-----------|
| **core-ktx** | 1.7.0 | **1.15.0** | 🔴 +8 versões |
| **appcompat** | 1.4.0 | **1.7.0** | 🔴 +3 versões |
| **activity-ktx** | 1.4.0 | **1.9.2** | 🔴 +5 versões |
| **lifecycle** | 2.4.0 | **2.8.6** | 🔴 +4 versões |
| **kotlinx-coroutines** | 1.5.1 | **1.9.0** | 🔴 +4 versões |

### 🟢 **ESPECÍFICAS - LibRetro Dependencies**

| Biblioteca | Versão Atual | Status | Observação |
|------------|--------------|--------|------------|
| **radialgamepad** | 0.6.0 | ✅ Estável | Verificar se há 0.7.x |
| **libretrodroid** | 0.6.2 | ✅ Estável | Verificar se há 0.7.x |
| **rxandroid** | 2.1.1 | 🟡 RxJava2 legado | Migrar para RxJava3? |

### ⚠️ **COMPATIBILIDADE - Java/Kotlin Target**

| Configuração | Atual | Recomendado | Razão |
|--------------|-------|-------------|-------|
| **sourceCompatibility** | Java 8 | **Java 17** | AGP 8.7+ requer Java 17 |
| **targetCompatibility** | Java 8 | **Java 17** | AGP 8.7+ requer Java 17 |
| **jvmTarget** | 1.8 | **17** | Kotlin 2.0 funciona melhor |

---

## 🚀 **Plano de Atualização por Fases**

### **FASE 1 - Preparação Crítica** ⚡

**Objetivo:** Atualizar AGP e Kotlin para versões compatíveis

**Arquivos a modificar:**
- `build.gradle` (root)
- `app/build.gradle`

**Mudanças em `build.gradle` (root):**
```groovy
buildscript {
    ext.kotlin_version = '2.0.20'  // ANTES: '1.9.10'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.7.2'  // ANTES: '8.0.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```

**Mudanças em `app/build.gradle`:**
```groovy
compileOptions {
    sourceCompatibility JavaVersion.VERSION_17  // ANTES: VERSION_1_8
    targetCompatibility JavaVersion.VERSION_17  // ANTES: VERSION_1_8
}
kotlinOptions {
    jvmTarget = '17'  // ANTES: '1.8'
}
```

**⚠️ ATENÇÃO:** Esta fase pode quebrar a compilação. Testar imediatamente após aplicar.

---

### **FASE 2 - SDK Update** 📱

**Objetivo:** Atualizar para Android 15 (SDK 35)

**Mudanças em `app/build.gradle`:**
```groovy
android {
    compileSdk 35            // ANTES: 33
    buildToolsVersion '35.0.0'  // ANTES: '33.0.2'
    
    defaultConfig {
        targetSdk 35         // ANTES: 33
        // outros valores permanecem iguais
    }
}
```

**Verificações necessárias:**
- Testar carregamento de ROMs
- Verificar funcionamento dos controles
- Validar cores LibRetro

---

### **FASE 3 - AndroidX Libraries** 📚

**Objetivo:** Atualizar bibliotecas AndroidX gradualmente

**Mudanças em `app/build.gradle` - dependencies:**
```groovy
dependencies {
    // Core Android
    implementation 'androidx.core:core-ktx:1.15.0'         // ANTES: 1.7.0
    implementation 'androidx.appcompat:appcompat:1.7.0'     // ANTES: 1.4.0
    
    // Activity & Lifecycle
    implementation 'androidx.activity:activity-ktx:1.9.2'   // ANTES: 1.4.0
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6'  // ANTES: 2.4.0
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.8.6'   // ANTES: 2.4.0
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.8.6'    // ANTES: 2.4.0
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0'  // ANTES: 1.5.1-native-mt
    
    // LibRetro (manter versões atuais por enquanto)
    implementation 'com.github.swordfish90:radialgamepad:0.6.0'
    implementation 'com.github.swordfish90:libretrodroid:0.6.2'
    
    // RxJava (considerar migração futura)
    implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
}
```

---

### **FASE 4 - LibRetro Check** 🎮

**Objetivo:** Verificar e atualizar bibliotecas específicas do emulador

**Ações:**
1. Verificar se `radialgamepad` tem versão 0.7.x disponível
2. Verificar se `libretrodroid` tem versão 0.7.x disponível
3. Testar compatibilidade com cores atuais:
   - bsnes
   - genesis_plus_gx
   - neocd
   - fbneo

**Se versões mais recentes estiverem disponíveis:**
```groovy
implementation 'com.github.swordfish90:radialgamepad:0.7.x'  // se disponível
implementation 'com.github.swordfish90:libretrodroid:0.7.x'  // se disponível
```

---

## ⚠️ **RISCOS E PRECAUÇÕES**

### **Riscos ALTOS:**

1. **AGP 8.0.2 → 8.7.2**
   - **Problema:** Mudanças significativas de API
   - **Mitigação:** Fazer backup do projeto, testar em branch separado
   - **Rollback:** Reverter para AGP 8.3.2 se necessário

2. **Kotlin 1.9 → 2.0**
   - **Problema:** Nova versão major com K2 compiler
   - **Mitigação:** Começar com Kotlin 1.9.25, depois 2.0
   - **Rollback:** Manter versão 1.9.x se houver problemas

3. **Java 8 → 17**
   - **Problema:** Mudança de runtime pode afetar LibRetro cores
   - **Mitigação:** Testes extensivos com todos os jogos
   - **Rollback:** Impossível - AGP 8.7+ requer Java 17

### **Riscos MÉDIOS:**

1. **AndroidX updates**
   - **Problema:** Possíveis breaking changes em APIs
   - **Mitigação:** Atualizar uma biblioteca por vez
   - **Rollback:** Versões específicas podem ser revertidas

2. **SDK 33 → 35**
   - **Problema:** Novas restrições de permissões/comportamento
   - **Mitigação:** Verificar documentação do Android 15
   - **Rollback:** Manter targetSdk 33 se necessário

### **Riscos BAIXOS:**

1. **LibRetro libraries**
   - **Problema:** Incompatibilidade com cores
   - **Mitigação:** Geralmente mantêm compatibilidade
   - **Rollback:** Fácil de reverter versões

---

## 🎯 **Estratégia de Execução Recomendada**

### **Opção 1: Conservadora (RECOMENDADA)**
```
1. Criar branch 'atualizacao-gradual'
2. Fazer apenas Fase 1 com versões intermediárias:
   - Kotlin 1.9.25 (não 2.0.20)
   - AGP 8.3.2 (não 8.7.2)
3. Testar completamente
4. Se estável, continuar com Fase 2
```

### **Opção 2: Agressiva (RISCO ALTO)**
```
1. Criar branch 'atualizacao-completa'
2. Aplicar Fases 1, 2 e 3 de uma vez
3. Resolver problemas conforme aparecem
4. Maior chance de incompatibilidades
```

### **Opção 3: Por Partes (MAIS SEGURA)**
```
1. Branch para cada fase
2. 'fase-1-agp-kotlin'
3. 'fase-2-sdk-35'
4. 'fase-3-androidx'
5. Merge individual após testes
```

---

## 📋 **Checklist de Testes**

### **Após Fase 1 (AGP/Kotlin):**
- [ ] Projeto compila sem erros
- [ ] APK gerado com sucesso
- [ ] App abre normalmente
- [ ] Controles respondem
- [ ] Um jogo carrega e funciona

### **Após Fase 2 (SDK):**
- [ ] Compilação com SDK 35 funciona
- [ ] Permissões funcionam corretamente
- [ ] Todos os cores LibRetro carregam
- [ ] Saves/states funcionam
- [ ] Performance mantida

### **Após Fase 3 (AndroidX):**
- [ ] Todas as telas carregam
- [ ] Navigation funciona
- [ ] Lifecycle events funcionam
- [ ] ViewModels funcionam
- [ ] LiveData/Coroutines funcionam

### **Após Fase 4 (LibRetro):**
- [ ] RadialGamePad funciona
- [ ] Todos os jogos testados carregam
- [ ] Controles touch responsivos
- [ ] Performance de emulação mantida

---

## 📝 **Notas Importantes**

1. **Backup:** Sempre fazer backup completo antes de iniciar
2. **Branch:** Trabalhar sempre em branch separado
3. **Testes:** Testar com jogos reais, não apenas compilação
4. **Performance:** Monitorar performance após cada fase
5. **Rollback:** Ter plano de rollback para cada fase

---

## 🔄 **Plano de Rollback**

### **Se Fase 1 falhar:**
```bash
git checkout minSdk
# Reverter para versões originais:
# AGP 8.0.2, Kotlin 1.9.10, Java 8
```

### **Se Fase 2 falhar:**
```groovy
// Manter AGP/Kotlin atualizados, reverter apenas SDK
compileSdk 33
targetSdk 33
buildToolsVersion '33.0.2'
```

### **Se Fase 3 falhar:**
```groovy
// Reverter bibliotecas AndroidX uma por uma
// Manter as que funcionam, reverter as problemáticas
```

---

## 📈 **Benefícios Esperados**

### **Performance:**
- Kotlin 2.0: Compilação mais rápida
- AndroidX atualizadas: Melhor performance
- AGP 8.7: Otimizações de build

### **Segurança:**
- SDK 35: Patches de segurança mais recentes
- Bibliotecas atualizadas: Correções de vulnerabilidades

### **Funcionalidade:**
- APIs mais recentes disponíveis
- Melhor compatibilidade com dispositivos novos
- Preparação para futuras atualizações

### **Manutenção:**
- Código mais moderno
- Menos dependências legadas
- Melhor suporte da comunidade

---

**Última atualização:** 19 de Setembro de 2025  
**Próxima revisão:** Após execução da Fase 1  
**Responsável:** Desenvolvedor principal  
**Status:** Planejamento concluído, aguardando execução
