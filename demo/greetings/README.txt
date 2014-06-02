A demo Greetings tenta demonstrar o uso de v�rios componentes e facetas. O 
servidor cria 3 componentes, um para cada l�ngua suportada (ingl�s, espanhol e 
portugu�s). Cada componente tem 3 facetas: uma responde "bom dia", uma "boa 
tarde" e outra "boa noite", na l�ngua que estiver em uso. Apenas uma 
implementa��o do servant da interface Greetings � necess�ria.

O cliente pergunta ao usu�rio em qual l�ngua deseja obter sauda��es. Se nenhuma 
l�ngua for especificada, tentar� obter sauda��es em todas. De acordo com o 
hor�rio local, ele ent�o utiliza a faceta adequada para obter a sauda��o 
correta.

------------------------------
-------- DEPEND�NCIAS---------
------------------------------

As depend�ncias de software s�o fornecidas j� compiladas, em conjunto com a 
demo.

ant-1.8.2.jar
ant-launcher-1.8.2.jar
jacorb-3.1.jar
openbus-sdk-core-2.0.1.2.jar
openbus-sdk-demo-util-2.0.1.2.jar
openbus-sdk-legacy-2.0.1.2.jar
scs-core-1.3.0-SNAPSHOT.jar
scs-idl-jacorb-1.3-SNAPSHOT.jar
slf4j-api-1.6.4.jar
slf4j-jdk14-1.6.4.jar


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
5) linguagem (opcional - deve ser uma das op��es: English, Spanish, Portuguese. 
Padr�o � Portuguese)


------------------------------
---------- EXECU��O ----------
------------------------------

Para que a demo funcione, pode ser necess�rio que as devidas permiss�es sejam 
cadastradas no barramento. Consulte o administrador do barramento e altere o 
arquivo admin\demo_greetings.adm conforme necess�rio.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende est�o em um diret�rio chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-greetings-2.0.0.0.jar demo.GreetingsServer localhost 2089 demo_greetings_java DemoGreetings.key 

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-greetings-2.0.0.0.jar demo.GreetingsClient localhost 2089 demo_greetings_java_client minhasenha Spanish
