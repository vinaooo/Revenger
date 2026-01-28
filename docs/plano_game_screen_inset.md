# Plano de Implementação: Game Screen Inset System

**Data:** 28/01/2026  
**Branch:** `feature/game-screen-inset`  
**Estimativa:** ~1h30  
**Status:** **BLOQUEADO - Aguardando upgrade LibretroDroid**

---

## ⚠️ NOTA IMPORTANTE - DESCOBERTA TÉCNICA

Durante a implementação da Fase 2, descobrimos que:

**❌ LibretroDroid 0.12.0 não suporta viewport API**
- Métodos `setViewport()` e propriedade `viewport` não existem nesta versão
- Esses recursos foram adicionados em versões posteriores (≥0.13.0)

**✅ Solução Implementada**
- ✅ Fase 0: Branch criada, build testado
- ✅ Fase 1: game_scale.xml criado com configurações
- ✅ Fase 2: GameScreenInsetConfig.kt implementado com:
  - Parser funcional para 3 formatos (V, V_H, T_R_B_L)
  - Conversão inset → viewport RectF
  - Validação e clamping
  - Logs completos
  - **Chamada de API comentada** (aguardando upgrade)

**🔧 Próximos Passos**
1. Upgrade LibretroDroid de 0.12.0 para 0.13.0+ (ou versão com viewport API)
2. Descomentar código em `GameScreenInsetConfig.kt` linhas ~228-236
3. Continuar implementação nas Fases 3-8

**📊 Progresso Atual: 2/8 fases completas (25%)**

---

## 📋 Sumário Executivo

Implementar um sistema configurável via XML que permite controlar a área de exibição do jogo através de margens (insets), similar ao modelo CSS. O jogo manterá automaticamente seu aspect ratio nativo dentro da área definida.

### Objetivo
Permitir que quem compilar o projeto possa definir onde e com qual tamanho relativo o jogo será exibido, otimizando para diferentes dispositivos, resoluções e preferências visuais.

### Descoberta Técnica
A biblioteca LibretroDroid possui suporte nativo via `GLRetroView.viewport = RectF(x, y, width, height)` **desde a versão 0.13.0+**.  
**Versão atual do projeto: 0.12.0** (sem suporte)

---

## 🎯 Formato de Configuração Escolhido

**Opção C - String Compacta com Shorthand**

```xml
<!-- Formatos suportados:
     "10"           = 10% em todos os lados
     "10_20"        = 10% vertical (top/bottom), 20% horizontal (left/right)
     "10_20_30_40"  = top_right_bottom_left (estilo CSS)
-->
<string name="gs_inset_portrait">0</string>
<string name="gs_inset_landscape">0</string>
```

### Conversão para Viewport
```
inset: top=5, right=25, bottom=45, left=25
       ↓
viewport: x=0.25, y=0.05, width=0.50, height=0.50
```

---

## 📁 Arquivos Envolvidos

### Novos Arquivos
| Arquivo | Descrição |
|---------|-----------|
| `app/src/main/res/values/game_scale.xml` | Configurações de inset |
| `app/src/main/java/.../config/GameScreenInsetConfig.kt` | Parser e aplicador |

### Arquivos a Modificar
| Arquivo | Modificação |
|---------|-------------|
| `RetroView.kt` | Integrar aplicação de viewport |
| `GameActivityViewModel.kt` | Chamar configuração em setupRetroView() |
| `GameActivity.kt` | Reaplicar viewport em mudanças de orientação |

---

## 🚀 Fases de Implementação

---

### Fase 0: Preparação do Ambiente

**Duração estimada:** 5 minutos

#### Tarefas
- [ ] Criar branch `feature/game-screen-inset` a partir de `develop`
- [ ] Verificar que build atual está funcionando

#### Comandos
```bash
git checkout develop
git pull origin develop
git checkout -b feature/game-screen-inset
./gradlew assembleDebug
```

#### Critério de Sucesso
- Branch criada
- Build passa sem erros

---

### Fase 1: Criar Arquivo de Configuração XML

**Duração estimada:** 10 minutos

#### Tarefas
- [ ] Criar `app/src/main/res/values/game_scale.xml`
- [ ] Adicionar tags de configuração com valores padrão (0 = sem margens)
- [ ] Documentar formato no próprio XML

#### Conteúdo do Arquivo
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- ============================================ -->
    <!-- Game Screen Inset Configuration             -->
    <!-- ============================================ -->
    
    <!-- Define margens (insets) em % para a área de exibição do jogo.
         O jogo será renderizado e centralizado dentro da área restante,
         mantendo automaticamente seu aspect ratio nativo.
         
         Formatos suportados:
         - "V"           : V% em todos os lados
         - "V_H"         : V% vertical (top/bottom), H% horizontal (left/right)
         - "T_R_B_L"     : top_right_bottom_left (estilo CSS)
         
         Valores: 0-99 (percentual da tela)
         Regras: top + bottom < 100, left + right < 100
         
         Exemplos:
         - "0"           : Sem margens (padrão atual)
         - "10"          : 10% margem em todos os lados (80% área útil)
         - "5_25"        : 5% top/bottom, 25% left/right
         - "5_25_45_25"  : top=5%, right=25%, bottom=45%, left=25%
    -->
    
    <!-- Portrait: Configuração para orientação vertical -->
    <string name="gs_inset_portrait">0</string>
    
    <!-- Landscape: Configuração para orientação horizontal -->
    <string name="gs_inset_landscape">0</string>
</resources>
```

#### Teste
- [ ] Verificar que o arquivo é válido: `./gradlew assembleDebug`
- [ ] Verificar sintaxe XML no Android Studio

#### Critério de Sucesso
- Arquivo criado sem erros de sintaxe
- Build passa

#### Commit
```bash
git add app/src/main/res/values/game_scale.xml
git commit -m "feat(config): Adicionar game_scale.xml com configurações de inset"
```

---

### Fase 2: Implementar GameScreenInsetConfig.kt

**Duração estimada:** 30 minutos

#### Tarefas
- [ ] Criar classe `GameScreenInsetConfig` em `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/config/`
- [ ] Implementar parser para os 3 formatos de string
- [ ] Implementar conversão inset → viewport (RectF)
- [ ] Implementar validação de valores
- [ ] Adicionar logs para debug

#### Estrutura da Classe
```kotlin
object GameScreenInsetConfig {
    private const val TAG = "GameScreenInsetConfig"
    
    data class Inset(
        val top: Int,
        val right: Int,
        val bottom: Int,
        val left: Int
    )
    
    // Parser principal
    fun parseInset(insetString: String): Inset
    
    // Converter inset para viewport
    fun insetToViewport(inset: Inset): RectF
    
    // Obter configuração baseada na orientação
    fun getConfiguredInset(resources: Resources, isPortrait: Boolean): Inset
    
    // Aplicar viewport ao RetroView
    fun applyToRetroView(retroView: GLRetroView, resources: Resources, isPortrait: Boolean)
}
```

#### Lógica de Parsing
```kotlin
fun parseInset(insetString: String): Inset {
    val parts = insetString.split("_").map { it.toIntOrNull() ?: 0 }
    
    return when (parts.size) {
        1 -> Inset(parts[0], parts[0], parts[0], parts[0])           // "10"
        2 -> Inset(parts[0], parts[1], parts[0], parts[1])           // "10_20"
        4 -> Inset(parts[0], parts[1], parts[2], parts[3])           // "10_20_30_40"
        else -> Inset(0, 0, 0, 0)  // fallback
    }
}
```

#### Lógica de Conversão
```kotlin
fun insetToViewport(inset: Inset): RectF {
    val x = inset.left / 100f
    val y = inset.top / 100f
    val width = (100 - inset.left - inset.right) / 100f
    val height = (100 - inset.top - inset.bottom) / 100f
    
    return RectF(x, y, width, height)
}
```

#### Teste Unitário (Manual)
Verificar parsing com diferentes formatos:
- `"0"` → Inset(0, 0, 0, 0) → RectF(0, 0, 1, 1)
- `"10"` → Inset(10, 10, 10, 10) → RectF(0.1, 0.1, 0.8, 0.8)
- `"5_25"` → Inset(5, 25, 5, 25) → RectF(0.25, 0.05, 0.5, 0.9)
- `"5_25_45_25"` → Inset(5, 25, 45, 25) → RectF(0.25, 0.05, 0.5, 0.5)

#### Critério de Sucesso
- Classe compila sem erros
- Logs aparecem corretamente

#### Commit
```bash
git add app/src/main/java/com/vinaooo/revenger/ui/retromenu3/config/GameScreenInsetConfig.kt
git commit -m "feat(viewport): Implementar GameScreenInsetConfig com parser e conversor"
```

---

### Fase 3: Integrar com RetroView.kt

**Duração estimada:** 15 minutos

#### Tarefas
- [ ] Importar `GameScreenInsetConfig` em `RetroView.kt`
- [ ] Adicionar método para aplicar viewport
- [ ] Chamar aplicação no `init {}` ou expor para chamada externa

#### Modificação em RetroView.kt
```kotlin
// Adicionar método público
fun applyViewportFromConfig(isPortrait: Boolean) {
    GameScreenInsetConfig.applyToRetroView(view, resources, isPortrait)
}
```

#### Teste
- [ ] Build passa: `./gradlew assembleDebug`
- [ ] Método acessível do ViewModel

#### Critério de Sucesso
- Compilação sem erros
- Método disponível para integração

#### Commit
```bash
git add app/src/main/java/com/vinaooo/revenger/retroview/RetroView.kt
git commit -m "feat(retroview): Adicionar método applyViewportFromConfig"
```

---

### Fase 4: Integrar com GameActivityViewModel.kt

**Duração estimada:** 15 minutos

#### Tarefas
- [ ] Modificar `setupRetroView()` para aplicar viewport após setup
- [ ] Detectar orientação atual
- [ ] Aplicar configuração correspondente

#### Modificação em GameActivityViewModel.kt
```kotlin
fun setupRetroView(activity: ComponentActivity, container: FrameLayout) {
    // ... código existente ...
    
    retroView?.let { rv ->
        // ... código existente ...
        
        // Aplicar viewport configurado
        val isPortrait = activity.resources.configuration.orientation == 
            Configuration.ORIENTATION_PORTRAIT
        rv.applyViewportFromConfig(isPortrait)
    }
}
```

#### Teste
- [ ] Instalar APK: `./gradlew installDebug`
- [ ] Verificar que jogo abre normalmente com `gs_inset_*=0`
- [ ] Verificar logs de aplicação de viewport

#### Critério de Sucesso
- Jogo abre e funciona normalmente
- Viewport aplicado (verificar via logs)

#### Commit
```bash
git add app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt
git commit -m "feat(viewmodel): Aplicar viewport em setupRetroView"
```

---

### Fase 5: Tratar Mudanças de Orientação

**Duração estimada:** 15 minutos

#### Tarefas
- [ ] Modificar `GameActivity.kt` para reaplicar viewport em `onConfigurationChanged`
- [ ] Garantir que orientação correta é detectada
- [ ] Aplicar configuração correspondente à nova orientação

#### Modificação em GameActivity.kt
```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    
    // ... código existente ...
    
    // Reaplicar viewport para nova orientação
    val isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
    viewModel.reapplyViewport(isPortrait)
}
```

#### Adicionar em GameActivityViewModel.kt
```kotlin
fun reapplyViewport(isPortrait: Boolean) {
    retroView?.applyViewportFromConfig(isPortrait)
}
```

#### Teste
- [ ] Instalar APK
- [ ] Configurar valores diferentes para portrait e landscape
- [ ] Rotacionar dispositivo e verificar que viewport muda corretamente

#### Critério de Sucesso
- Rotação aplica configuração correta
- Sem crashes durante rotação

#### Commit
```bash
git add app/src/main/java/com/vinaooo/revenger/views/GameActivity.kt
git add app/src/main/java/com/vinaooo/revenger/viewmodels/GameActivityViewModel.kt
git commit -m "feat(rotation): Reaplicar viewport em mudanças de orientação"
```

---

### Fase 6: Testes de Integração

**Duração estimada:** 20 minutos

#### Cenários de Teste

| # | Configuração Portrait | Configuração Landscape | Resultado Esperado |
|---|----------------------|------------------------|-------------------|
| 1 | `0` | `0` | Comportamento atual (sem margens) |
| 2 | `10` | `10` | Jogo 80% em ambas orientações |
| 3 | `5_25_45_25` | `0` | Portrait: 50% área topo; Landscape: full |
| 4 | `25` | `10_30` | Portrait: centralizado 50%; Landscape: assimétrico |
| 5 | `0_0_50_0` | `0_50_0_0` | Portrait: metade superior; Landscape: metade esquerda |

#### Checklist de Testes
- [ ] Cenário 1: Valores padrão
- [ ] Cenário 2: Margem uniforme
- [ ] Cenário 3: Configuração completa portrait
- [ ] Cenário 4: Configurações diferentes por orientação
- [ ] Cenário 5: Metades da tela
- [ ] Teste de valores inválidos (graceful fallback)
- [ ] Teste de rotação durante gameplay
- [ ] Verificar que menus funcionam independentemente

#### Critério de Sucesso
- Todos os cenários funcionam conforme esperado
- Sem crashes em nenhum cenário
- Aspect ratio do jogo sempre preservado

---

### Fase 7: Documentação e Cleanup

**Duração estimada:** 10 minutos

#### Tarefas
- [ ] Remover logs de debug excessivos (manter apenas essenciais)
- [ ] Adicionar comentários KDoc nas funções públicas
- [ ] Atualizar `.github/copilot-instructions.md` se necessário
- [ ] Verificar que não há warnings de compilação novos

#### Commit Final
```bash
git add -A
git commit -m "docs: Documentação e cleanup do sistema de inset"
```

---

### Fase 8: Merge para Develop

**Duração estimada:** 5 minutos

#### Tarefas
- [ ] Verificar que todos os testes passaram
- [ ] Fazer merge para develop
- [ ] Verificar build final

#### Comandos
```bash
git checkout develop
git merge feature/game-screen-inset --no-ff -m "Merge feature/game-screen-inset: Sistema de configuração de viewport"
./gradlew clean assembleDebug installDebug
```

#### Critério de Sucesso
- Merge sem conflitos
- Build e instalação bem-sucedidos
- Jogo funciona corretamente

---

## 📊 Resumo de Commits

| Fase | Mensagem de Commit |
|------|-------------------|
| 1 | `feat(config): Adicionar game_scale.xml com configurações de inset` |
| 2 | `feat(viewport): Implementar GameScreenInsetConfig com parser e conversor` |
| 3 | `feat(retroview): Adicionar método applyViewportFromConfig` |
| 4 | `feat(viewmodel): Aplicar viewport em setupRetroView` |
| 5 | `feat(rotation): Reaplicar viewport em mudanças de orientação` |
| 7 | `docs: Documentação e cleanup do sistema de inset` |
| 8 | `Merge feature/game-screen-inset: Sistema de configuração de viewport` |

---

## ⚠️ Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Viewport não aplicado | Baixa | Alto | API LibretroDroid é estável |
| Parsing incorreto | Média | Médio | Validação robusta + fallback |
| Conflito com rotação | Média | Alto | Testar extensivamente fase 5 |
| Valores inválidos | Alta | Baixo | Clamping automático |

---

## 📝 Exemplos de Uso Documentados

### Padrão (Sem Alteração)
```xml
<string name="gs_inset_portrait">0</string>
<string name="gs_inset_landscape">0</string>
```

### Jogo 80% Centralizado
```xml
<string name="gs_inset_portrait">10</string>
<string name="gs_inset_landscape">10</string>
```

### Portrait Otimizado para Gamepad
```xml
<string name="gs_inset_portrait">5_10_35_10</string>
<string name="gs_inset_landscape">0</string>
```

### Simular CRT Pequena
```xml
<string name="gs_inset_portrait">20_25_20_25</string>
<string name="gs_inset_landscape">15_30_15_30</string>
```

---

## ✅ Checklist Final

- [ ] Branch criada e nomeada corretamente
- [ ] Todos os arquivos criados/modificados
- [ ] Todos os testes passaram
- [ ] Código documentado
- [ ] Merge para develop realizado
- [ ] Build final funciona
