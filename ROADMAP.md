# 🗺️ Roadmap & Ideias Futuras — In-RP

Este documento serve como um painel central para registrar ideias, sugestões, melhorias de qualidade de vida e funcionalidades planejadas para as próximas versões do **In-RP**.

---

## 📌 Próxima Versão (Backlog Imediato)

- [ ] **Incluir `/afk` no `/rp help`**: Adicionar a linha de exibição de `inrp.help.afk` no método `showHelp` de [RPCommand.java](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/src/main/java/com/tio/inrp/commands/RPCommand.java) (as chaves de tradução já estão prontas em `en_us.json` e `pt_br.json`).
- [ ] **Sincronização de Tab List após Reviver**: Acompanhar atualizações da API do NeoForge para contornar a retenção de cache do cliente vanilla sem exigir reconexão.

---

## 💡 Ideias de Mecânicas de Roleplay (Gameplay)

### 1. Comandos de Expressão & Chat de Proximidade
- [ ] **Chat Local por Padrão (Proximidade)**:
  - O chat padrão do jogo (apenas apertar `T` e enviar mensagem) passa a ser **local por proximidade** (com raio configurável no TOML, ex: 30 a 50 blocos).
  - Apenas jogadores dentro do raio recebem a mensagem no chat (ex: `[L] Player: mensagem`).
  - Notificação sutil caso nenhum jogador esteja por perto: `(Ninguém por perto ouviu você)` (igual à mecânica já existente no `/roll`).
  - Toggles e opções na config: `localChatEnabled`, `localChatRadius`, `opSpyLocalChat` (para admins poderem monitorar todo o chat local se quiserem).
- [ ] **`/g <mensagem>` (ou `/global`)**: Chat global do servidor para quando o chat local estiver ativo por padrão. Permite falar com todos os jogadores do servidor (com prefixo `[G]` e *cooldown* configurável para evitar *flood*).
- [ ] **`/off <mensagem>` (ou `/b`)**: Permite falar fora do personagem (Out-Of-Character / OOC) mesmo com o RP ligado. A mensagem é exibida com tag destacada e cor neutra (ex: `(( [OFF] Player: mensagem ))` em cinza), ideal para avisos rápidos entre jogadores sem precisar desativar e reativar o `/rp`.
- [ ] **`/me <ação>`**: Permite ao jogador narrar ações em terceira pessoa no chat local (ex: `* Arthur puxa sua espada com cautela.*`).
- [ ] **`/do <descrição>`**: Narração de acontecimentos do ambiente ou de cena (ex: `[CENA] O cavalo parece estar muito cansado após a longa viagem.`).
- [ ] **`/w` ou `/sussurro <mensagem>`**: Chat com raio de proximidade reduzido (ex: 3 a 5 blocos) para conversas sigilosas.

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

## ⚙️ Diretrizes para Novas Implementações

Ao escolher e implementar qualquer funcionalidade deste documento:
1. Manter a premissa de ser **100% Server-Side** (clientes vanilla conectam sem precisar do mod).
2. Respeitar as diretrizes de invariantes e testes do [ARCHITECTURE.md](file:///c:/Users/Rian/Documents/GitHub/in-rp-1.21.1/ARCHITECTURE.md).
3. Todas as mensagens devem possuir chaves correspondentes em `en_us.json` e `pt_br.json`.
