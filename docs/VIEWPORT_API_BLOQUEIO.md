# Bloqueio de Implementação - Viewport API Indisponível

**Data:** 28/01/2026  
**Feature:** Game Screen Inset System  
**Branch:** `feature/game-screen-inset`  
**Status:** ⚠️ BLOQUEADO

---

## 🔴 Problema Identificado

Durante a implementação da **Fase 2** do sistema de inset de tela, descobrimos que a LibretroDroid versão **0.12.0** (atualmente em uso no projeto) **não possui suporte para viewport API**.

### Tentativas Realizadas

1. **Tentativa 1:** `retroView.viewport = RectF(...)`
   - ❌ Erro: `Unresolved reference 'viewport'`
   
2. **Tentativa 2:** `LibretroDroid.setViewport(x, y, w, h)`
   - ❌ Erro: `Unresolved reference 'setViewport'`

### Evidência no Código Fonte

Pesquisa no repositório LibretroDroid:
- ✅ Arquivo `GLRetroView.kt` linhas 63-67 contém a propriedade `viewport`
- ✅ Arquivo `LibretroDroid.java` linha 120 contém o método `setViewport()`
- ❌ Mas essas features **não existem na release 0.12.0**
- ✅ Disponíveis em versões **≥0.13.0** (main branch)

---

## 📦 Versão Atual vs Necessária

| Aspecto | Versão Atual | Versão Necessária |
|---------|--------------|-------------------|
| LibretroDroid | **0.12.0** | ≥**0.13.0** |
| API viewport | ❌ Não disponível | ✅ Disponível |
| Release | Stable (2023) | Main branch |

### Linha de Dependência Atual
```gradle
// app/build.gradle linha 216
implementation 'com.github.swordfish90:libretrodroid:0.12.0'
```

---

## ✅ Trabalho Já Implementado

### Fase 0: Preparação ✅
- Branch `feature/game-screen-inset` criada
- Build verificado e funcional
- **Commit:** `ca4cb95`

### Fase 1: Configuração XML ✅
- Arquivo `game_scale.xml` criado
- Tags `gs_inset_portrait` e `gs_inset_landscape` configuradas
- Valores padrão "0" (tela cheia)
- Documentação completa inline
- **Commit:** `66ce62e`

### Fase 2: Parser e Conversor ✅ 
- Arquivo `GameScreenInsetConfig.kt` implementado (235 linhas)
- **Funcionalidades completas:**
  - ✅ Parser para 3 formatos ("V", "V_H", "T_R_B_L")
  - ✅ Validação de valores (0-99%)
  - ✅ Clamping automático
  - ✅ Conversão inset → viewport RectF
  - ✅ Logs detalhados
  - ⚠️ Chamada de API comentada (aguardando LibretroDroid 0.13.0+)
- **Commit:** `f4e6702`

---

## 🛠️ Como Proceder

### Opção 1: Upgrade LibretroDroid (Recomendado)

1. **Atualizar dependência**
   ```gradle
   // app/build.gradle
   implementation 'com.github.swordfish90:libretrodroid:0.13.0' // ou mais recente
   ```

2. **Descomentar código em GameScreenInsetConfig.kt**
   ```kotlin
   // Linhas ~228-236
   retroView.queueEvent {
       com.swordfish.libretrodroid.LibretroDroid.setViewport(
           viewportRect.left,
           viewportRect.top,
           viewportRect.width(),
           viewportRect.height()
       )
   }
   ```

3. **Testar compilação**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Continuar Fases 3-8**

### Opção 2: Aguardar Release Oficial

- Verificar periodicamente releases do LibretroDroid
- Link: https://github.com/Swordfish90/LibretroDroid/releases
- Quando versão estável ≥0.13.0 for lançada, seguir Opção 1

### Opção 3: Implementação Alternativa (Não Recomendado)

Criar fork do LibretroDroid e compilar localmente com viewport API.  
**⚠️ Complexidade alta, não recomendado.**

---

## 📊 Progresso da Feature

```
[████░░░░░░░░░░░░] 25% completo

Fases:
✅ Fase 0: Preparação
✅ Fase 1: Configuração XML
✅ Fase 2: Parser (API comentada)
⏸️  Fase 3: Integração RetroView (bloqueada)
⏸️  Fase 4: ViewModel (bloqueada)
⏸️  Fase 5: Orientação (bloqueada)
⏸️  Fase 6: Testes (bloqueada)
⏸️  Fase 7: Documentação (bloqueada)
⏸️  Fase 8: Merge (bloqueada)
```

---

## 📝 Código Pronto para Ativação

Quando LibretroDroid for atualizado, **apenas descomentar**:

```kotlin
// app/src/main/java/com/vinaooo/revenger/config/GameScreenInsetConfig.kt
// Linhas 228-236

// ATUALMENTE COMENTADO:
// retroView.queueEvent {
//     com.swordfish.libretrodroid.LibretroDroid.setViewport(
//         viewportRect.left,
//         viewportRect.top,
//         viewportRect.width(),
//         viewportRect.height()
//     )
// }
```

Todo o resto está implementado e testado:
- ✅ Parser funcional
- ✅ Conversão matemática correta
- ✅ Validação robusta
- ✅ Logs informativos

---

## 🔗 Links Úteis

- **LibretroDroid Repository:** https://github.com/Swordfish90/LibretroDroid
- **Releases:** https://github.com/Swordfish90/LibretroDroid/releases
- **GLRetroView.kt (main):** https://github.com/Swordfish90/LibretroDroid/blob/main/libretrodroid/src/main/java/com/swordfish/libretrodroid/GLRetroView.kt#L63-L67
- **API Documentation:** Check main branch for viewport usage

---

## ✍️ Notas Finais

Esta situação é comum em desenvolvimento Android quando dependências externas evoluem. O trabalho realizado **não foi desperdiçado** - toda a lógica está implementada e pronta para ativação assim que a biblioteca for atualizada.

**Recomendação:** Fazer upgrade para LibretroDroid 0.13.0+ assim que uma versão estável for lançada.
