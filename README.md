# Projeto Ômega

Jogo de ação, exploração e stealth desenvolvido para a disciplina de **Projeto de Inteligência Artificial**.

O jogo será ambientado em um **laboratório de pesquisas abandonado**, onde um experimento envolvendo inteligência artificial e organismos experimentais saiu do controle.

O jogador deverá explorar a instalação, descobrir o que aconteceu com os pesquisadores e encontrar uma forma de escapar.

## Características

* Perspectiva top-down
* Pixel art
* Cenários construídos com tiles
* Diferentes tipos de inimigos
* Inimigos com comportamentos autônomos
* Percepção por visão e som

## Tecnologias

* Java 21
* JavaFX
* Maven

## Desenvolvimento por atividades

### Atividade 1 – Game Loop e estrutura inicial

Nesta etapa foi criada a estrutura inicial do projeto e implementado o **Game Loop** utilizando `AnimationTimer` do JavaFX.

Também foram implementados:

* Estrutura inicial do jogador;
* Controle de movimentação pelo teclado;
* Atualização do jogo utilizando `deltaTime`;
* Renderização do jogador;
* Estrutura inicial do tilemap.

O Game Loop passou a controlar o ciclo de atualização e renderização do jogo.

### Atividade 2 – TileMap e representação do ambiente

Nesta etapa foi implementado o sistema de **TileMap**, utilizando uma matriz bidimensional de objetos `Tile`.

Foram definidos diferentes tipos de tiles:

* `FLOOR` – área transitável;
* `WALL` – obstáculo;
* `MUD` – terreno com custo de movimentação diferente;
* `CONTAMINATED` – área especial do ambiente.

Cada tile possui informações sobre sua possibilidade de movimentação e seu custo.

Também foram implementadas as colisões do jogador com as paredes do mapa.

### Atividade 3 – Navegação com o algoritmo A*

Nesta etapa foi implementado um agente autônomo capaz de navegar pelo tilemap utilizando o algoritmo de busca **A\***.

O agente possui uma posição inicial e um destino previamente definidos. O algoritmo analisa o tilemap, identifica os caminhos transitáveis e calcula uma rota até o destino.

Foram implementados:

* Algoritmo de busca A*;
* Função heurística baseada na distância Manhattan;
* Movimentação em quatro direções;
* Verificação de tiles transitáveis;
* Verificação dos limites do mapa;
* Reconstrução do caminho encontrado;
* Agente autônomo para execução da rota;
* Representação visual do caminho calculado;
* Destino previamente definido.

O agente percorre automaticamente o caminho encontrado pelo algoritmo e para ao alcançar o destino.

## Estrutura atual

```text
org.example
├── Main.java
├── GameLoop.java
│
├── ai
│   └── AStarPathfinder.java
│
├── entities
│   ├── Agent.java
│   └── Player.java
│
├── input
│   └── Keyboard.java
│
└── world
    ├── Tile.java
    └── TileMap.java
