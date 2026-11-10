# Assets originais — Projeto Ômega

Fonte reproduzível: `tools/generate_assets.py` (Python, Pillow e numpy; seed 707). Nenhum sample ou imagem de terceiros. Os recursos estão prontos para uso e não exigem Python para jogar.

## Pixel art

- `player`, `sentinel`, `drone`, `specimen`: PNG RGBA 64×64; 16 células de **16×16**. Colunas: quatro quadros. Linhas: baixo, esquerda, direita, cima.
- `facility`: atlas 128×48, 24 tiles de **16×16**. Inclui portas abertas/fechadas, controle central, manchas, corpo, rachadura, console e placa, além dos pisos e equipamentos.
- `title`: ilustração original 192×200 da câmara rompida, com organismo esquelético.
- `catalog`: folha de referência. Não é carregada pelo jogo.

- `portrait`: identificação de campo do agente, 32×32.
- `icons`: quatro ícones de 16×16 para vida, arma, pulso e fôlego.

O mundo usa tiles lógicos de 32 px; o desenho usa escala inteira sobre as células de 16 px. Isso preserva a grade de pixels sem alterar as unidades de colisão e navegação.

## Som

WAV PCM estéreo, 16 bits, 44.100 Hz. Vinte e três arquivos:

- Camadas: `ambient` (48 s), `machinery` (16 s), `heartbeat` (2 s), `breath` (4 s).
- Interação: `terminal`, `interact`, `door`, `lure`, `pulse`.
- Movimento e dano: `step`, `step2`, `step3`, `sneak`, `hit`.
- Agentes: `radio`, `alert`, `roar`.
- Ação: `shot`, `ricochet`, `breach`.
- Ambiente e final: `drip`, `win`, `lose`.

Síntese em camadas com ruído filtrado em frequência, transientes, ressonâncias, reverberação por múltiplos taps, panorama estéreo, remoção de DC, envelopes e normalização com headroom. A trilha não usa samples externos. O mixer limita a soma suavemente e controla volumes de ambiente e efeitos separados.
