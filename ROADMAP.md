# 🗺️ Roadmap & Ideias Futuras — In-RP

Este documento serve como um painel central para registrar ideias, sugestões, melhorias de qualidade de vida e funcionalidades planejadas para as próximas versões do **In-RP**.

---

## 📌 Próxima Versão (Backlog Imediato)

- [ ] **Incluir `/afk` no `/rp help`**: Adicionar a linha de exibição de `inrp.help.afk` no método `showHelp` de [RPCommand.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/commands/RPCommand.java) (as chaves de tradução já estão prontas em `en_us.json` e `pt_br.json`).
- [ ] **Sincronização de Tab List após Reviver**: Acompanhar atualizações da API do NeoForge para contornar a retenção de cache do cliente vanilla sem exigir reconexão.

---

## 💬 Chat Local & Global (Próxima Grande Funcionalidade)

- [ ] **Chat Local por Padrão (Proximidade)**:
  - O chat padrão do jogo (apenas apertar `T` e enviar mensagem) passa a ser **local por proximidade** (com raio configurável no TOML, ex: 30 a 50 blocos).
  - Apenas jogadores dentro do raio recebem a mensagem no chat (ex: `[L] Player: mensagem`).
  - Notificação sutil caso nenhum jogador esteja por perto: `(Ninguém por perto ouviu você)` (igual à mecânica já existente no `/roll`).
  - Toggles e opções na config: `localChatEnabled`, `localChatRadius`.
- [ ] **`/g <mensagem>` (ou `/global`)**: Chat global do servidor para quando o chat local estiver ativo por padrão. Permite falar com todos os jogadores do servidor (com prefixo `[G]` e *cooldown* configurável para evitar *flood*).
- [ ] **Chat Spy para Moderação (`/rpadmin spy` ou `/chatspy`)**:
  - Ferramenta dedicada para a administração e moderação monitorar canais em tempo real.
  - Comando *toggle* por admin (com permissão OP nível 2 ou nó de permissão correspondente).
  - Permite espiar:
    - Mensagens do chat local fora do alcance do administrador (com prefixo discreto, ex: `[SPY:Local]`).
    - Mensagens privadas do vanilla (`/tell`, `/msg`, `/w`) entre jogadores para coibir metagaming e abusos (ex: `[SPY:PM] JogadorA -> JogadorB: texto`).
    - Sussurros de proximidade ultra-curtos (`/sussurro`).
  - Configurações dedicadas no TOML para a staff controlar o que pode ser espiado (`spyLocalChat = true`, `spyPrivateMessages = true`).

---

## 💡 Ideias de Mecânicas de Roleplay (Gameplay)

### 1. Comandos de Expressão
- [ ] **`/off <mensagem>` (ou `/b`)**: Permite falar fora do personagem (Out-Of-Character / OOC) mesmo com o RP ligado. A mensagem é exibida com tag destacada e cor neutra (ex: `(( [OFF] Player: mensagem ))` em cinza), ideal para avisos rápidos entre jogadores sem precisar desativar e reativar o `/rp`.
- [ ] **`/do <descrição>`**: Narração de acontecimentos do ambiente ou de cena em terceira pessoa (ex: `[CENA] O cavalo parece estar muito cansado após a longa viagem.`).
- [ ] **`/sussurro <mensagem>`**: Fala em voz baixa com raio de proximidade ultra-curto (ex: 3 a 5 blocos) para conversas sigilosas entre personagens próximos (diferente do `/msg`/`/tell` vanilla que é privado e global).

### 2. Sistema de Identidade e Personagem
- [ ] **Nome de Personagem (RP Name)**: Permitir definir um nome fictício para o personagem (ex: `/rp nome <Nome Sobrenome>`), substituindo ou complementando o nick do Minecraft no chat e nametag.
- [ ] **Ficha Rápida (`/rp perfil [player]`)**: Exibição em mensagem formatada ou livro/baú de dados básicos do personagem: idade, ocupação/profissão, biografia curta e status.
- [ ] **Status de Humor/Estado**: Pequeno texto de estado do personagem (ex: `Ferido`, `Ocupado`, `Viajando`).

### 3. Vidas e Sobrevivência
- [ ] **Transferência de Vidas (`/lives transfer <player> [quantia]`)**: Permitir que jogadores doem ou compartilhem vidas entre si (ideal para mecânicas de sacrifício, cura ritualística ou clãs).
- [ ] **Efeito Sonoro/Visual ao Perder Vida**: Efeito dramático de som e partículas para alertar jogadores próximos quando alguém perde uma de suas vidas limitadas.
- [ ] **Itens de Reviver**: Suporte a um item consumível customizado (ou totem/coração) que reviva um jogador eliminado caso seja usado por outro jogador.

---

## 🛡️ Administração e Moderação

- [ ] **Auditoria de Logs (`inrp_audit.log`)**: Registro de ações administrativas críticas (revives, alterações de vidas, resets) em arquivo de log separado para fácil monitoramento de staff.
- [ ] **Integração com LuckPerms / Permissões NeoForge**: Nós de permissão detalhados (`inrp.command.roll`, `inrp.command.lives`, `inrp.admin.*`) além do padrão OP nível 2 vanilla.
- [ ] **Comando `/rpadmin inspect <player>`**: Painel completo para administradores verem tudo sobre o jogador em um único clique (status RP, AFK, vidas, mortes, coordenadas atuais).

---

## 🚀 Visão Futura: Ecossistema "In-RP Companion / Addon"

> **Fase de Planejamento:** A ser desenvolvido **apenas após** o mod principal (`In-RP Core`) estar maduro, completo e estável.

A ideia deste projeto futuro é expandir as fronteiras do RPG quando o servidor e os jogadores optarem por uma experiência modded completa:

* **Papel do `In-RP` (Core):** Continua sendo o mod base de regras, lógica, vidas, dados e comandos, mantendo a capacidade de operar 100% server-side para clientes vanilla.
* **Papel do `In-RP Companion` (Mod Client & Server):**
  - Instalado **tanto no servidor quanto nos clientes** que desejam o pacote estendido.
  - **Blocos e Itens Customizados:** Adição de blocos de ambientação/cenário para roleplay, itens consumíveis (ex: contrato de vidas, poções/totens de renascimento, moedas de RPG).
  - **Interfaces Gráficas (GUIs):** Telas nativas e modernas para criação/edição de ficha de personagem, rolagem visual de dados e painéis administrativos visuais.
  - **HUD & Imersão:** Indicadores na tela para status de RP, vidas restantes e balões de fala flutuantes (*speech bubbles*) sobre os personagens no chat local.
  - **Efeitos Audiovisuais:** Sons e animações próprias integradas aos eventos do Core.

---

## ⚙️ Diretrizes para Novas Implementações

Ao escolher e implementar qualquer funcionalidade deste documento:
1. Manter a premissa de ser **100% Server-Side** para o mod base `In-RP Core` (clientes vanilla conectam sem precisar do mod).
2. Respeitar as diretrizes de invariantes e testes do [ARCHITECTURE.md](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/ARCHITECTURE.md).
3. Todas as mensagens devem possuir chaves correspondentes em `en_us.json` e `pt_br.json`.

