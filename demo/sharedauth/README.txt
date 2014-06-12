A demo Shared Auth tenta demonstrar como se realiza o login por autentica��o 
compartilhada. Um servidor fornece o servant da interface Hello e um cliente 
realiza uma chamada "sayHello" nesse servant. Al�m disso, esse cliente inicia o 
processo de login por autentica��o, recebendo um objeto LoginProcess e um 
segredo, que codifica em um arquivo texto.

Um outro cliente, que far� o login por autentica��o compartilhada, l� ent�o esse 
arquivo para obter o objeto LoginProcess e o segredo. De posse desses dados, 
consegue realizar o login, procurar pelo servi�o Hello e realizar tamb�m uma 
chamada "sayHello", com a mesma entidade mas um identificador de login 
diferente.

� importante notar que o tempo entre a inicia��o do processo de login por 
autentica��o compartilhada do primeiro cliente e o login de fato do segundo 
cliente deve ser menor que o tempo de lease. Caso contr�rio, o login expirar� e 
uma exce��o ser� recebida.

------------------------------
-------- DEPEND�NCIAS---------
------------------------------

As depend�ncias de software s�o fornecidas j� compiladas, em conjunto com a 
demo.

ant-1.8.2.jar
ant-launcher-1.8.2.jar
jacorb-3.5-SNAPSHOT.jar
jacorb-omgapi-3.5-SNAPSHOT.jar
openbus-sdk-core-2.0-SNAPSHOT.jar
openbus-sdk-demo-util-2.0-SNAPSHOT.jar
openbus-sdk-legacy-2.0-SNAPSHOT.jar
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
5) caminho para o arquivo onde ser�o escritos os dados da autentica��o compartilhada (opcional - gera um arquivo no diretorio corrente)

Cliente SharedAuth:
1) host do barramento
2) porta do barramento
3) caminho para o arquivo com os dados da autentica��o compartilhada (opcional 
- busca pelo arquivo padr�o no diret�rio corrente)


------------------------------
---------- EXECU��O ----------
------------------------------

Para que a demo funcione, pode ser necess�rio que as devidas permiss�es sejam cadastradas no barramento. Consulte o administrador do barramento e altere o arquivo admin\demo_sharedauth.adm conforme necess�rio.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente
3) Cliente SharedAuth


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende est�o em um diret�rio chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-hello-2.0-SNAPSHOT.jar demo.HelloServer localhost 2089 demo_hello_java DemoHello.key

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-sharedauth-2.0-SNAPSHOT.jar demo.Client localhost 2089 demo_sharedauth_java_client minhasenha

3) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-sharedauth-2.0-SNAPSHOT.jar demo.SharedAuthClient localhost 2089 

