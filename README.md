# Spatial Join

Antes de compilar o projeto, instale o **maven3** na sua máquina, **caso já não tenha instalado**, com o comando abaixo.
~~~
sudo apt-get update && sudo apt-get install maven3
~~~

## Execução
Quando qualquer linha for alterada no código, este deve ser compilado para que estas alterações estejam presentes na execução. Quando o código não for alterado, basta apenas executar o arquivo **local-0.0.1-SNAPSHOT.jar**, contendo o código compilado.

### **Para compilar**: 
Gera o arquivo local-0.0.1-SNAPSHOT.jar com o código compilado.
  - Entre na pasta *local*
  - Digite o comando abaixo: 
    ~~~
    mvn package
    ~~~

### **Para executar**:
Entre na pasta *target* que está dentro da pasta *local*. Entre o comando abaixo: 
~~~	
java -jar local-0.0.1-SNAPSHOT.jar [caminho para o arquivo de propriedades] [opção númerica(0, 1 ou 2) do join:  0 - Welder Join; 1 - RSJoin; 2 - Verdade]
~~~

O arquivo de propriedade pode ser encontrado na pasta *local/config*, onde existe o arquivo rtree.properties. Este arquivo é configurado a partir dos parâmetros abaixo:
~~~
#normal, chisquared, exponencial
distribution = [uma das três distribuições possíveis: normal, chisquared, exponencial]

layer1_path=[caminho para o arquivo .shp do primeiro layer na junção]
layer2_path=[caminho para o arquivo .shp do segundo layer na junção]

cache_layer1_geometries_path=[caminho para a pasta que contém os polígonos já gerados do primeiro layer]
cache_layer2_geometries_path=[caminho para a pasta que contém os polígonos já gerados do segundo layer]

num_cache_geometries=[número inteiro que representa o número de geometrias na cache]

num_threads_system=[número inteiro que representa quantas threads serão utilizadas. Este número deve ser configurado com o valor de núcleos que tiver na máquina]

error_in_meters=[erro em metros]

#Join Configurations
max_cache_entries=[número máximo de geometrias na memória RAM]
num_join_iteratios=[número de vezes que o spatial join será executado]

layer1_name=[nome do primeiro layer]
layer1_capacity=[capacidade do primeiro layer]
layer2_name=[nome do segundo layer]
layer2_capacity=[capacidade do segundo layer]

result_join_file_path=[caminho para a pasta onde serão colocados os resultados do join]
join_process_time_file_path=[caminho para o arquivo que irá conter o tempo de execução das junções]

num_join_executions=[número de execuções do join a cada iteração]
num_join_iterations_by_step=[número de loops do join a cada execução]

gamma=[valores de gamma]
p=[valor de p]
sd=[valor de sd]
~~~