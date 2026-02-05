# Fase 9: Testes do Sistema Multi-Slot Save States

## Status de Conclusão

### ✅ Testes Unitários (CONCLUÍDOS)

**Total**: 49 testes | **Status**: ✅ 100% passando

#### 1. SaveSlotDataTest.kt ✅ (9 testes)
- Validação do modelo de dados
- Formatação de timestamps
- Operações de comparação e cópia

#### 2. SaveStateManagerTest.kt ✅ (16 testes)
- CRUD completo de save states
- Operações de copy/move/rename
- Gerenciamento de screenshots
- Validação de singleton

#### 3. CallbacksTest.kt ✅ (11 testes)
- Validação de conformidade SOLID
- Interface Segregation Principle (ISP)
- Verificação de herança múltipla de interfaces

#### 4. MenuIntegrationTest.kt ✅ (13 testes)
- Integração do sistema RetroMenu3
- Validação de implementação de interfaces
- Testes de navegação básica

---

## 🎯 Próximas Etapas: Testes Manuais

### Pré-requisitos para Testes Manuais

1. ✅ APK compilado e instalado no dispositivo
2. ✅ Sistema multi-slot implementado (Fases 1-8)
3. ✅ Testes unitários passando
4. ⚠️ ROM configurada no config.xml
5. ⚠️ Dispositivo Android (físico ou emulador) disponível

---

## 📋 Roteiro de Testes Manuais

### Preparação do Ambiente de Teste

#### Passo 1: Verificar Configuração
```bash
# 1. Verificar ROM configurada
cat app/src/main/res/values/config.xml | grep config_rom

# 2. Build e instalação
./gradlew clean assembleDebug installDebug

# 3. Verificar instalação
adb shell pm list packages | grep revenger
```

#### Passo 2: Limpar Estado Anterior (Opcional)
```bash
# Para testar migração de save legado, NÃO execute este comando
# Para testar do zero, execute:
adb shell pm clear com.vinaooo.revenger.<config_id>
```

---

### Teste 1: Navegação Básica do Menu

**Objetivo**: Verificar se todos os menus de save states são acessíveis

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 1.1 | Abrir o emulador | Jogo carrega normalmente |
| 1.2 | Pressionar SELECT+START | Menu RetroMenu3 abre |
| 1.3 | Navegar até "Progress" | Item Progress destacado |
| 1.4 | Pressionar A ou ENTER | Submenu Progress abre |
| 1.5 | Verificar opções do Progress | Deve exibir: Save State, Load State, Manage Saves, Back |
| 1.6 | Navegar entre opções | Navegação suave, sem travamentos |
| 1.7 | Pressionar B ou BACKSPACE | Volta ao menu principal |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

### Teste 2: Grid de Save Slots (Visual)

**Objetivo**: Validar exibição e navegação no grid 3x3

#### Teste 2.1: Abrir Save State Grid

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 2.1 | No submenu Progress, selecionar "Save State" | Grid 3x3 de slots aparece |
| 2.2 | Verificar layout | 9 slots visíveis (3 linhas x 3 colunas) |
| 2.3 | Verificar slots vazios | Mostram "Empty" ou placeholder |
| 2.4 | Verificar indicador de seleção | Slot 1 (top-left) com borda amarela/destacada |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 2.2: Navegação no Grid com D-PAD

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 2.5 | Pressionar D-PAD RIGHT | Seleção move para Slot 2 |
| 2.6 | Pressionar D-PAD RIGHT novamente | Seleção move para Slot 3 |
| 2.7 | Pressionar D-PAD RIGHT (borda direita) | Seleção NÃO ultrapassa (bounded) - fica no Slot 3 |
| 2.8 | Pressionar D-PAD DOWN | Seleção move para Slot 6 (mesma coluna) |
| 2.9 | Pressionar D-PAD LEFT | Seleção move para Slot 5 |
| 2.10 | Navegar até Slot 1 e pressionar UP | Seleção NÃO ultrapassa (fica no Slot 1) |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 2.3: Navegação com Touch

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 2.11 | Tocar no Slot 5 (centro) | Slot 5 fica selecionado |
| 2.12 | Tocar no Slot 9 (canto inferior direito) | Slot 9 fica selecionado |
| 2.13 | Tocar fora do grid | Nada acontece OU fecha o grid (depende da implementação) |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

### Teste 3: Salvar State (Save Operation)

**Objetivo**: Validar o processo completo de salvamento

#### Teste 3.1: Salvar em Slot Vazio

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 3.1 | Jogar por alguns segundos | Jogo em execução |
| 3.2 | Abrir menu → Progress → Save State | Grid de save aparece |
| 3.3 | Selecionar Slot 1 (vazio) | Slot 1 destacado |
| 3.4 | Pressionar A ou ENTER | Diálogo "Name your save" aparece (opcional) |
| 3.5 | Inserir nome "Test Save 1" OU deixar padrão | Nome aceito |
| 3.6 | Confirmar save | Salvamento realizado |
| 3.7 | Grid atualizado | Slot 1 agora mostra screenshot + nome |
| 3.8 | Verificar screenshot | Deve mostrar imagem do jogo no momento do save |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 3.2: Sobrescrever Slot Ocupado

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 3.9 | Jogar mais um pouco (mudar estado do jogo) | Estado diferente do save anterior |
| 3.10 | Abrir Save State Grid | Slot 1 ainda mostra save anterior |
| 3.11 | Selecionar Slot 1 (ocupado) | Slot 1 destacado |
| 3.12 | Pressionar A ou ENTER | Diálogo de confirmação: "Overwrite 'Test Save 1'?" |
| 3.13 | Confirmar sobrescrita | Save substituído |
| 3.14 | Verificar screenshot atualizado | Deve mostrar nova imagem do estado atual |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 3.3: Múltiplos Saves

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 3.15 | Salvar em Slot 2, 3, 4 (diferentes estados) | Todos os saves criados |
| 3.16 | Verificar grid | 4 slots ocupados (1-4), 5 vazios (5-9) |
| 3.17 | Cada slot mostra screenshot único | Screenshots diferentes |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

### Teste 4: Carregar State (Load Operation)

**Objetivo**: Validar o carregamento de save states

#### Teste 4.1: Carregar de Slot Ocupado

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 4.1 | Jogar até chegar em um ponto diferente | Estado atual ≠ Slot 1 |
| 4.2 | Abrir Progress → Load State | Grid de load aparece |
| 4.3 | Selecionar Slot 1 (ocupado) | Slot 1 destacado |
| 4.4 | Pressionar A ou ENTER | Estado carregado instantaneamente |
| 4.5 | Menu fecha automaticamente | Jogo retoma do estado salvo |
| 4.6 | Verificar estado do jogo | Deve estar exatamente como no Slot 1 |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 4.2: Tentar Carregar de Slot Vazio

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 4.7 | Abrir Load State Grid | Grid aparece |
| 4.8 | Selecionar Slot 5 (vazio) | Slot 5 destacado |
| 4.9 | Pressionar A ou ENTER | Nada acontece OU mensagem "No save in this slot" |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

### Teste 5: Gerenciar Saves (Manage Saves)

**Objetivo**: Validar operações de Copy, Move, Delete, Rename

#### Teste 5.1: Copiar Save

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 5.1 | Abrir Progress → Manage Saves | Grid de gerenciamento aparece |
| 5.2 | Selecionar Slot 1 (ocupado) | Slot 1 destacado |
| 5.3 | Pressionar A ou ENTER | Menu de operações aparece |
| 5.4 | Verificar opções | Copy, Move, Delete, Rename, Back |
| 5.5 | Selecionar "Copy" | "Select Destination" aparece |
| 5.6 | Selecionar Slot 7 (vazio) como destino | Slot 7 destacado |
| 5.7 | Confirmar cópia | Cópia realizada |
| 5.8 | Verificar grid | Slot 1 ainda existe (original) |
| 5.9 | Verificar Slot 7 | Slot 7 agora tem cópia do Slot 1 |
| 5.10 | Screenshot e nome idênticos | Mesmo conteúdo |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 5.2: Mover Save

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 5.11 | Selecionar Slot 2 | Slot 2 destacado |
| 5.12 | Abrir menu de operações → Move | "Select Destination" aparece |
| 5.13 | Selecionar Slot 8 (vazio) | Slot 8 destacado |
| 5.14 | Confirmar movimentação | Save movido |
| 5.15 | Verificar Slot 2 | Agora está vazio |
| 5.16 | Verificar Slot 8 | Contém o save do Slot 2 |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 5.3: Deletar Save

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 5.17 | Selecionar Slot 3 (ocupado) | Slot 3 destacado |
| 5.18 | Abrir menu → Delete | Diálogo de confirmação: "Delete 'Nome do Save'?" |
| 5.19 | Confirmar deleção | Save deletado |
| 5.20 | Verificar Slot 3 | Agora mostra "Empty" |
| 5.21 | Tentar carregar Slot 3 | Não carrega (slot vazio) |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 5.4: Renomear Save

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 5.22 | Selecionar Slot 4 (ocupado) | Slot 4 destacado |
| 5.23 | Abrir menu → Rename | Diálogo "Rename Save" com nome atual |
| 5.24 | Alterar para "Boss Battle" | Nome aceito |
| 5.25 | Confirmar renomeação | Nome atualizado |
| 5.26 | Verificar grid | Slot 4 agora exibe "Boss Battle" |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

### Teste 6: Migração de Save Legado

**Objetivo**: Validar migração automática do save state antigo

**Pré-requisito**: Precisa de um save legado existente

#### Opção A: Com Save Legado Existente

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 6.1 | Verificar existência de save antigo | Arquivo `/files/state` existe |
| 6.2 | Abrir emulador pela primeira vez (após atualização) | App inicia normalmente |
| 6.3 | Abrir Load State Grid | Grid aparece |
| 6.4 | Verificar Slot 1 | Contém save migrado |
| 6.5 | Verificar nome do Slot 1 | Deve ter indicação "(Legacy)" ou similar |
| 6.6 | Verificar screenshot | Pode estar vazio (sem screenshot no formato antigo) |
| 6.7 | Carregar Slot 1 | Carrega o save legado corretamente |

**Status**: [ ] Passou [ ] Falhou [ ] N/A (sem save legado)  
**Observações**: _______________________________________________

#### Opção B: Forçar Criação de Save Legado (Para Teste)

```bash
# 1. Criar save legado manualmente
adb shell "echo 'legacy save data' > /data/data/com.vinaooo.revenger.<config_id>/files/state"

# 2. Reiniciar app
adb shell am force-stop com.vinaooo.revenger.<config_id>
adb shell am start -n com.vinaooo.revenger.<config_id>/.views.GameActivity

# 3. Seguir passos 6.3-6.7 acima
```

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

### Teste 7: Persistência e Estabilidade

**Objetivo**: Garantir que saves persistem após reinicialização

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 7.1 | Criar saves em vários slots (ex: 1, 3, 5, 9) | Saves criados |
| 7.2 | Fechar completamente o app | App fechado |
| 7.3 | Reabrir o emulador | App inicia normalmente |
| 7.4 | Abrir Load State Grid | Grid aparece |
| 7.5 | Verificar slots salvos anteriormente | Todos os saves ainda existem |
| 7.6 | Screenshots preservados | Screenshots ainda visíveis |
| 7.7 | Carregar um save | Carrega corretamente |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

### Teste 8: Limites e Edge Cases

**Objetivo**: Testar comportamento em situações extremas

#### Teste 8.1: Encher Todos os Slots

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 8.1 | Salvar em todos os 9 slots | Todos os slots ocupados |
| 8.2 | Verificar grid de save | Nenhum slot vazio visível |
| 8.3 | Tentar salvar novamente | Só permite sobrescrever |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

#### Teste 8.2: Nomes Especiais

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 8.4 | Renomear save com nome muito longo (50+ chars) | Truncado ou rejeitado graciosamente |
| 8.5 | Renomear com caracteres especiais (!@#$%) | Aceito ou sanitizado |
| 8.6 | Renomear com nome vazio | Usa nome padrão "Slot X" |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

### Teste 9: Performance e Responsividade

**Objetivo**: Validar que operações são rápidas e não travam a UI

| # | Ação | Resultado Esperado |
|---|------|-------------------|
| 9.1 | Abrir Save State Grid | Grid aparece em < 500ms |
| 9.2 | Salvar state | Operação completa em < 2s |
| 9.3 | Carregar state | Carregamento em < 1s |
| 9.4 | Copiar save | Cópia completa em < 1s |
| 9.5 | Deletar save | Deleção instantânea (< 500ms) |
| 9.6 | Navegar entre slots | Sem lag perceptível |

**Status**: [ ] Passou [ ] Falhou  
**Observações**: _______________________________________________

---

## 🐛 Registro de Bugs Encontrados

### Bug #1
**Descrição**: _______________________________________________  
**Passos para Reproduzir**: _______________________________________________  
**Severidade**: [ ] Crítica [ ] Alta [ ] Média [ ] Baixa  
**Status**: [ ] Aberto [ ] Corrigido [ ] Não vai corrigir  

### Bug #2
**Descrição**: _______________________________________________  
**Passos para Reproduzir**: _______________________________________________  
**Severidade**: [ ] Crítica [ ] Alta [ ] Média [ ] Baixa  
**Status**: [ ] Aberto [ ] Corrigido [ ] Não vai corrigir  

---

## 📊 Resumo Final dos Testes

### Testes Unitários
- **Total**: 49 testes
- **Passou**: _____ / 49
- **Falhou**: _____ / 49

### Testes Manuais
- **Total**: 9 categorias (60+ casos de teste)
- **Passou**: _____ / 60
- **Falhou**: _____ / 60

### Avaliação Geral
- [ ] ✅ Sistema aprovado para produção
- [ ] ⚠️ Aprovado com ressalvas (bugs menores)
- [ ] ❌ Reprovado (bugs críticos encontrados)

---

## 🎬 Comandos Úteis para Testes

```bash
# Ver logs do app em tempo real
adb logcat | grep Revenger

# Verificar estrutura de arquivos de save
adb shell ls -la /data/data/com.vinaooo.revenger.<config_id>/files/saves/

# Ver conteúdo de metadata.json de um slot
adb shell cat /data/data/com.vinaooo.revenger.<config_id>/files/saves/slot_1/metadata.json

# Limpar dados do app (reset total)
adb shell pm clear com.vinaooo.revenger.<config_id>

# Reinstalar app
./gradlew clean assembleDebug installDebug

# Capturar screenshot do device
adb exec-out screencap -p > test_screenshot.png
```

---

## ✅ Checklist Final de Aprovação

Antes de considerar a Fase 9 concluída:

- [ ] Todos os testes unitários passando (49/49)
- [ ] Pelo menos 90% dos testes manuais passando
- [ ] Nenhum bug crítico encontrado
- [ ] Performance aceitável (saves < 2s, loads < 1s)
- [ ] Migração de save legado testada e funcionando
- [ ] Screenshots sendo capturados corretamente
- [ ] Navegação touch/gamepad/teclado funcionando
- [ ] Documentação atualizada com resultados dos testes

---

**Data do Teste**: __________________  
**Testador**: ______________________  
**Dispositivo**: ____________________  
**ROM Testada**: ___________________  
**Build Version**: __________________
