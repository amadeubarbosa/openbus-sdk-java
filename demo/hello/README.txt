A demo Hello tenta demonstrar a aplicação mais simples possível. Um servidor 
fornece o servant da interface Hello e o cliente realiza uma chamada "sayHello" 
nesse servant.

------------------------------
-------- DEPENDÊNCIAS---------
------------------------------

As dependências de software são fornecidas já compiladas, em conjunto com a 
demo.

ant-1.8.2.jar
ant-launcher-1.8.2.jar
jacorb-3.1.jar
openbus-sdk-core-2.0.0-SNAPSHOT.jar
openbus-sdk-demo-util-2.0.0-SNAPSHOT.jar
openbus-sdk-legacy-2.0.0-SNAPSHOT.jar
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
4) senha (opcional - se não for fornecida, será usado o nome de entidade)


------------------------------
---------- EXECUÇÃO ----------
------------------------------

Para que a demo funcione, pode ser necessário que as devidas permissões sejam 
cadastradas no barramento. Consulte o administrador do barramento e altere o 
arquivo admin\demo_hello.adm conforme necessário.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende estão em um diretório chamado 
'dependency':

1) java -Djava.endorsed.dirs=./dependency/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-hello-2.0.0.0.jar demo.HelloServer localhost 2089 demo_hello_java DemoHello.key

2) java -Djava.endorsed.dirs=./dependency/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-hello-2.0.0.0.jar demo.HelloClient localhost 2089 demo_hello_java_client minhasenha
