# Projeto Ômega — implementação e materiais da disciplina

Esta versão usa os PDFs presentes na raiz como referência de tema e arquitetura. O cenário de exemplo “Estação Aurora” da apostila de agentes não foi transferido para o jogo: a história e os arquétipos permanecem os do Projeto Ômega.

| Material | Aplicação no jogo | Código / verificação |
| --- | --- | --- |
| `Projeto_Omega_Relatorio_Tematica_Historia.pdf`, pp. 1–2 | Agente enviado para investigar o desaparecimento da equipe; instalação científica parcialmente sem energia; pesquisas de IA e organismos; corredores, depósitos, pesquisa e segurança; recuperação de informações, controle central e fuga. Sentinela, drone e criatura têm percepções e comportamentos distintos. | `GameSession`, `Agent.Kind`, tiles e registros. A missão exige arquivos → reator → controle central → elevador. |
| `Agentes-Inteligentes-e-Maquinas-de-Estados.pdf` | Ciclo percepção → decisão → ação, estados com Enter/Update/Exit, prioridades, suspeita, memória após perder visão e atualização de caminhos limitada por tempo. | `Agent.Behavior`: sete implementações de estado. `DynamicNavigator` separado da FSM. Estados e rotas visíveis com G. |
| `Apostila IA para agentes em jogos digitais.pdf`, pp. 2–4 | Evidências com posição observada, ID, origem, confiança e validade; confiança decai; inspeção invalida a pista; suspeita local; busca com prazo. | `AgentMemory`, `Agent.Search`. Teste congela a posição lembrada quando o jogador desaparece atrás de uma parede. |
| Mesma apostila, pp. 8–11 | Arquétipos com funções observáveis; blackboard de equipe; rádio com alcance, atraso e interrupção; preservação do instante original; deduplicação e reserva de setores. | `RadioNetwork`: 340 px, 0,8 s de atraso, validade original de 9 s para contato visual. Criaturas não recebem rádio. EMP interrompe transmissão pendente. Reservas expiram em 4 s e são liberadas em cancelamentos ou remoção do proprietário. |
| Mesma apostila, p. 7 | Manter uma FSM quando o comportamento não justifica árvores e Utility AI. | FSM com patrulha, investigação, perseguição, ataque, busca, retorno e atordoamento. Não há dois sistemas disputando o movimento. |
| `Aprofundando-o-Algoritmo-A.pdf` | Grafo ponderado, g+h, heurística apropriada, listas aberta e fechada, pais, reconstrução, tratamento de impossibilidade e recálculo híbrido. | `AStarPathfinder`: fila de prioridade, menor g conhecido, conjunto fechado por matriz, Manhattan e quatro direções. |
| `Apostila de Revisão - Projeto de Inteligência Artificial.pdf`, seção 5 | Recalcular a partir da posição atual, destino atualizado e mapa atual; validar waypoints; evitar busca por quadro; distribuir trabalho entre agentes. | `DynamicNavigator`: mudança do tile-alvo / revisão do mapa / bloqueio da rota; intervalo mínimo de 0,55 s; reutilização da rota válida; até duas buscas de navegação por atualização. |
| `docs/documentacao_atv_03_astar.pdf` | Preservação da representação em tiles, custos de lama, colisão, vizinhança ortogonal e visualização da rota. | Mapa ampliado; lama custa 2, contaminação custa 5, piso custa 1. G exibe caminhos e estado de cada agente. |

## O que significa A* dinâmico aqui

É uma nova execução do A* quando informações relevantes mudam durante a partida, conforme os PDFs. Fechar uma porta altera a navegabilidade e incrementa a revisão do mapa; abrir uma porta permite novas rotas. A posição usada como objetivo é a última observada ou comunicada ao agente. Mudar de tile enquanto está visível atualiza o destino. Após perder visão, a IA busca a posição lembrada e setores plausíveis, sem consultar continuamente a posição oculta do jogador.

O controlador interrompe o deslocamento se o próximo waypoint estiver bloqueado, mesmo antes de poder solicitar outra busca. Não atravessa o obstáculo durante o cooldown. Objetivos inalcançáveis resultam em rota vazia; investigação e busca têm prazo, e a patrulha pode tentar outro posto. As verificações de alcançabilidade para distribuir setores de busca são pontuais na atribuição, separadas do orçamento de navegação por frame.

## Cooperação e contrajogo

- Sentinelas mantêm os acessos e comunicam contato confirmado; drones investigam pistas e abandonam perseguições sem contato mais cedo.
- O relato transmite uma posição antiga, não o objeto do jogador. Mantém ID e instante; confiança é reduzida para o receptor.
- A criatura é agressiva, mais sensível a ruídos e tem visão curta. Não integra a rede de rádio dos sistemas de segurança.
- Aliados próximos podem investigar setores diferentes, com reservas temporárias. A remoção ou o atordoamento do dono libera sua reserva.
- O jogador pode quebrar visão, andar devagar, lançar iscas, fechar portas e interromper o rádio com EMP.
- Ataques têm preparação visível (0,48 s na criatura e 0,65 s nos sistemas de segurança), alcance limitado e cooldown. Sair do alcance durante a preparação evita o dano.

## Testes reproduzíveis

`GameRulesTest` cobre regras básicas e o fluxo completo da missão. `AdvancedSystemsTest` cobre atraso/alcance/validade/interrupção do rádio, deduplicação, reservas, memória sem onisciência, A* após alteração do destino e do mapa, limite de buscas, portas, esquiva, decodificação de controles e mixagem PCM real.

Os testes gráficos opcionais (`UiSmoke`) continuam verificando teclado sem controle conectado. `AudioSmoke` verifica que o mixer escreve quadros no dispositivo de saída local; ele precisa ser solicitado explicitamente, pois reproduz áudio. O teste automatizado do controle injeta eventos do protocolo Linux; não substitui validar um modelo físico de adaptador USB.

## Ação e ritmo da missão

A revisão de jogabilidade mantém ação, investigação e stealth do PDF temático. A arma tem munição finita e gera uma evidência sonora na posição do disparo; a esquiva não atravessa obstáculos. NPCs atingidos podem ser interrompidos, inclusive durante uma transmissão pendente. Ao remover um agente, o coordenador libera suas reservas.

`FacilityEvents` controla eventos de contenção, com pontos de entrada autorados: não cria criaturas à vista nem junto do personagem. Esses eventos não atualizam diretamente a memória dos agentes com a posição real do jogador; eles precisam ouvir o incidente ou obter contato visual. O elevador é uma fonte sonora no mundo. Descargas elétricas avisam antes do dano e aumentam o custo do tile, provocando uma nova rota dentro do orçamento existente.

`ActionGameplayTest` valida munição, cadência, colisão dos projéteis, morte dos agentes, esquiva, eventos, custos dinâmicos e combinações de controle. `PlaythroughSmoke` verifica uma travessia da missão com agentes ativos usando as mesmas regras de movimento e interação; é uma simulação com mira automática, não uma avaliação subjetiva do desafio para uma pessoa.
