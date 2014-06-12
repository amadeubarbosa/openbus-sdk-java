A demo DedicatedClock tenta demonstrar um servi�o de rel�gio que n�o pode 
funcionar sem estar conectado a um barramento. O servidor s� funciona ap�s 
conseguir conectar, realizar o login e registrar sua oferta. Caso o login seja 
perdido, sua callback de login inv�lido tenta refazer esse processo eternamente 
at� conseguir.

O cliente, por sua vez, tenta acessar o barramento para buscar e utilizar o 
servidor. Se n�o conseguir ap�s um tempo, falha com uma mensagem de erro.

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
5) tempo de espera entre cada tentativa de acesso ao barramento (em milisegundos 
e opcional - se n�o for fornecido, ser� 1)

Cliente
1) host do barramento
2) porta do barramento
3) nome de entidade
4) senha (opcional - se n�o for fornecida, ser� usado o nome de entidade)
5) tempo de espera entre cada tentativa de acesso ao barramento (em milisegundos 
e opcional - se n�o for fornecido, ser� 1)
6) n�mero m�ximo de tentativas de acesso ao barramento (opcional - se n�o for 
fornecido, ser� 10)


------------------------------
---------- EXECU��O ----------
------------------------------

Para que a demo funcione, pode ser necess�rio que as devidas permiss�es sejam cadastradas no barramento. Consulte o administrador do barramento e altere o arquivo admin\demo_dedicated_clock.adm conforme necess�rio.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende est�o em um diret�rio chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-dedicatedclock-2.0-SNAPSHOT.jar demo.DedicatedClockServer localhost 2089 demo_dedicatedclock_java DemoDedicatedClock.key

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-dedicatedclock-2.0-SNAPSHOT.jar demo.DedicatedClockClient localhost 2089 demo_dedicatedclock_java_client minhasenha
