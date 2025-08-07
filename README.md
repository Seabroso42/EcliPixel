# EcliPixel - Uma solução de manipulação de imagens usando programação concorrente

1. VISÃO GERAL DO PROJETO
--------------------------
O EcliPixel é um backend de processamento de imagem de alta performance, escrito em Java.
Ele utiliza a biblioteca JavaCV (um wrapper para OpenCV) para executar algoritmos de visão computacional de forma rápida e eficiente.

O sistema foi projetado com foco em performance e processamento paralelo, utilizando uma arquitetura em camadas que promove a separação de responsabilidades, facilitando a manutenção e a expansão.

O projeto pode ser utilizado através de scripts de teste para processamento em lote ou como um servidor web que expõe sua funcionalidade através de uma API HTTP.

No vídeo do insomnia, nos testamos todas as rotas. O binarizar síncrono, o async que utiliza threads para binarizar toda a pasta e devolve em outra pasta de saída e a criação do histograma. Já no vídeo da interface, o botão inarizar e ver histograma que aparecem estão pegando já dessas endpoints. Eu mostrei um No vídeo do insomnia, nos testamos todas as rotas. O binarizar síncrono, o async que utiliza threads para binarizar toda a pasta e devolve em outra pasta de saída e a criação do histograma. Já no vídeo da interface, o botão inarizar e ver histograma que aparecem estão pegando já dessas endpoints. Eu mostrei um checkbox que tem escrito binarização local (teste) que era a binarização e histograma teste de python que foi mostrado na segunda. Mas a binarização manual nos botões depois o histograma já são gerados pelo código em Java.checkbox que tem escrito binarização local (teste) que era a binarização e histograma teste de python que foi mostrado na segunda. Mas a binarização manual nos botões depois o histograma já são gerados pelo código em Java.

2. ARQUITETURA
----------------
O projeto segue uma arquitetura em camadas (3-Tier) limpa e desacoplada.

O fluxo de controle é o seguinte:

[CLIENTE (Teste ou API)] -> [CAMADA DE CONTROLE/I-O] -> [CAMADA DE ORQUESTRAÇÃO] -> [CAMADA DE SERVIÇO]

  - Camada de I/O: `PixelCorreio.java`
    - Responsável por toda a interação com o sistema de arquivos (ler, listar, salvar).

  - Camada de Serviço (Motor): `EcliPixel.java`
    - Contém a lógica pura de todos os algoritmos de processamento. É 100% stateless (não armazena estado), o que garante a segurança em ambientes com múltiplas threads.

  - Camada de Orquestração: `PixelMestre.java`
    - O "cérebro" da concorrência. Gerencia um pool de threads para executar trabalhos pesados em paralelo (processamento em lote). Contém a lógica de negócio de alto nível.

  - Camada de Controle/API: `Servidor.java` ou Classes de Teste
    - A porta de entrada do sistema. Recebe os comandos (seja via requisição HTTP ou de um script de teste) e delega o trabalho para as camadas inferiores.


3. ESTRATÉGIAS DE PARALELISMO
------------------------------
O projeto implementa duas formas complementares de paralelismo:

* 1. Paralelismo Inter-Imagem (Processamento em Lote):
  - Gerenciado pelo `PixelMestre`.
  - Processa MÚLTIPLAS imagens diferentes ao mesmo tempo, uma imagem por thread.
  - Ideal para acelerar o processamento de grandes volumes de arquivos.

* 2. Paralelismo Intra-Imagem (Otimização de Operação):
  - Encapsulado em métodos específicos do `EcliPixel` (ex: `binarizarParalelo`).
  - Processa UMA ÚNICA imagem grande de forma mais rápida, "fatiando-a" e processando cada fatia em uma thread diferente usando ROIs (Regions of Interest).
  - É uma ferramenta que o `PixelMestre` pode decidir usar com base em regras de negócio (ex: se a imagem for maior que 2000 pixels de largura).


4. GUIA DE TESTES E VALIDAÇÃO
-----------------------------
O projeto contém vários arquivos de teste na pasta `src/main/java/` para validar diferentes aspectos do sistema.

* A. `TesteSequencialDefinitivo.java`
  - PROPÓSITO: Verificar a CORREÇÃO de todos os algoritmos de forma isolada e sequencial.
  - O QUE FAZ: Itera sobre todas as imagens da pasta `resources/input` e aplica um pipeline completo de funções (Gaussian, binarizar, converter HSV, isolar canal, calcular/desenhar histograma), salvando cada etapa.
  - COMO INTERPRETAR: É o nosso teste de "controle de qualidade". Garante que a base do `EcliPixel` é sólida e que os algoritmos produzem o resultado visual esperado.

* B. `TesteIsolarCanal.java`
  - PROPÓSITO: Testar especificamente a função `isolarCanal` em todos os três canais BGR.
  - O QUE FAZ: Para cada imagem colorida, extrai e salva os canais Azul (1), Verde (2) e Vermelho (3) como imagens separadas em tons de cinza.
  - COMO INTERPRETAR: Permite a validação visual da separação de canais.

* C. `TesteSegmentacaoAvancado.java`
  - PROPÓSITO: Validar a função de segmentação por cor `segmentarHSV`, a mais complexa do `EcliPixel`.
  - O QUE FAZ: Testa vários intervalos de cor (diferentes tons de azul, verde e vermelho) em múltiplas imagens, salvando tanto a máscara binária quanto o recorte colorido.
  - COMO INTERPRETAR: Demonstra a flexibilidade e a robustez do algoritmo de segmentação em diferentes cenários.

* D. `TesteParalelo.java` e `TesteParaleloBinarizacao.java`
  - PROPÓSITO: Testar e validar a PERFORMANCE do processamento em lote gerenciado pelo `PixelMestre`.
  - O QUE FAZEM: Executam um ou mais pipelines de processamento em todas as imagens de entrada, mas distribuindo o trabalho entre múltiplas threads.
  - COMO INTERPRETAR: A saída do console mostrará as threads trabalhando de forma intercalada, provando o paralelismo. O tempo de execução de um lote paralelo deve ser significativamente menor que o tempo de um teste sequencial equivalente.

* E. `TesteSupremo.java` (O Benchmark)
  - PROPÓSITO: Medir, comparar e demonstrar o ganho de velocidade (speedup) do paralelismo.
  - O QUE FAZ: Primeiro, executa um pipeline complexo de forma sequencial para estabelecer um tempo base (baseline). Depois, executa o mesmo pipeline em paralelo com diferentes contagens de threads (1, 2, 4, 8...).
  - COMO INTERPRETAR: O relatório final no console mostra uma tabela comparando os tempos e calculando quantas vezes o processamento paralelo foi mais rápido. É a prova final do valor da nossa arquitetura concorrente.


5. COMO COMPILAR E EXECUTAR
---------------------------

* REQUISITOS: JDK 17 (ou superior) e Apache Maven.

* EXECUTAR OS TESTES:
  Você pode executar qualquer uma das classes de Teste (ex: `TesteSupremo.java`) diretamente pela sua IDE (clicando com o botão direito -> "Run"). Elas lerão as imagens da pasta `src/main/resources/input` e salvarão os resultados em pastas dentro de `output/`.

## Como executar o programa

1 - Clone o repositório

```git clone https://github.com/Seabroso42/EcliPixel.git```

2 - Instale os requisitos
```
cd gui
pip install -r  requirements.txt
```

3 - Execute a aplicação
```
streamlit run app.py
```
