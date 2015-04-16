A demo Multiplexing tenta demonstrar o uso da multiplexa��o de conex�es no SDK 
OpenBus. Ela demonstra a utiliza��o do contexto do OpenBus (OpenBusContext) para 
definir a conex�o de despacho e a escolha de qual conex�o deve ser usada para 
cada requisi��o feita (conceito de conex�o corrente).

Um servidor cria 3 conex�es para 3 componentes, todos com uma faceta Timer. Essa 
faceta, ao receber uma chamada "newTrigger", recebe tamb�m um objeto remoto de 
callback no qual deve chamar o m�todo "notifyTrigger", e o faz em uma nova 
thread.

O cliente implementa esses objetos de callback com o m�todo "notifyTrigger". Ao 
iniciar, o cliente busca por Timers no barramento. Para cada Timer encontrado, 
cria uma nova thread onde chama o m�todo "newTrigger" passando um novo objeto de 
callback, para em seguida realizar o logout do barramento e finalizar essa 
thread. A thread principal do programa � mantida viva para que todas as 
requisi��es sejam atendidas. Ap�s receber todas as notifica��es dos Timers, a 
thread principal � acordada, faz o logout da sua conex�o e o cliente � 
finalizado.

------------------------------
-------- DEPEND�NCIAS---------
------------------------------

As depend�ncias de software s�o fornecidas j� compiladas, em conjunto com a 
demo.

ant-1.8.2.jar
ant-launcher-1.8.2.jar
jacorb-3.5.jar
jacorb-omgapi-3.5.jar
openbus-sdk-core-2.1.0.0-RC1.jar
openbus-sdk-demo-util-2.1.0.0-RC1.jar
openbus-sdk-legacy-2.1.0.0-RC1.jar
scs-core-1.2.1.3.jar
slf4j-api-1.7.6.jar
slf4j-jdk14-1.7.6.jar


------------------------------
--------- ARGUMENTOS ---------
------------------------------

Servidor
1) host do barramento
2) porta do barramento
3) nome de entidade
4) caminho para a chave privada

Cliente
1) host do barramento
2) porta do barramento
3) nome de entidade
4) senha (opcional - se n�o for fornecida, ser� usado o nome de entidade)


------------------------------
---------- EXECU��O ----------
------------------------------

Para que a demo funcione, pode ser necess�rio que as devidas permiss�es sejam 
cadastradas no barramento. Consulte o administrador do barramento e altere o 
arquivo admin\demo_multiplexing.adm conforme necess�rio.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende est�o em um diret�rio chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-multiplexing-2.1.0.0-RC1.jar demo.MultiplexingServer localhost 2089 demo_multiplexing_java DemoMultiplexing.key

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-multiplexing-2.1.0.0-RC1.jar demo.MultiplexingClient localhost 2089 demo_multiplexing_java_client minhasenha

