package demo;

import java.util.Properties;
import java.util.logging.Level;

import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;

import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.util.Cryptography;

/**
 * Cliente do demo Hello
 * 
 * @author Tecgraf
 */
public final class HelloClient {
  /**
   * Função principal.
   * 
   * @param args argumentos.
   */
  public static void main(String[] args) {
    try {
      // Obtém dados de inicialização através do arquivo de propriedades
      Properties props = Utils.readPropertyFile("/test.properties");
      String host = props.getProperty("bus.host.name");
      int port = Integer.valueOf(props.getProperty("bus.host.port"));
      String entity = props.getProperty("client.entity");
      String password = props.getProperty("client.password");
      String serverEntity = props.getProperty("server.entity");

      // Definindo o nivel de log
      Utils.setLogLevel(Level.parse(props.getProperty("log.level", "OFF")));

      // Cria conexão e a define como conexão padrão tanto para entrada como saída.
      //
      // OBS: O uso exclusivo da conexão padrão (sem uso de requester e dispatcher) 
      // só é recomendado para aplicações que criem apenas uma conexão e desejem 
      // utilizá-la em todos os casos. Para situações diferentes, consulte o 
      // manual do SDK OpenBus e/ou outras demos.
      ORB orb = ORBInitializer.initORB();
      OpenBusContext context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
      Connection connection = context.createConnection(host, port);
      context.setDefaultConnection(connection);

      // Executa o login
      if (login(connection, entity, password)) {
        System.exit(1);
      }

      // Faz busca utilizando propriedades geradas automaticamente e 
      // propriedades definidas pelo serviço específico
      ServiceProperty[] serviceProperties = new ServiceProperty[2];
      // propriedade gerada automaticamente
      serviceProperties[0] =
        new ServiceProperty("openbus.offer.entity", serverEntity);
      // propriedade definida pelo serviço hello
      serviceProperties[1] =
        new ServiceProperty("offer.domain", "OpenBus Demos");
      ServiceOfferDesc[] services = findServices(context, serviceProperties);

      // analiza as ofertas encontradas
      Hello hello = getHello(services);
      if (hello == null) {
        connection.logout();
        System.exit(1);
      }
      else {
        // utiliza o serviço
        hello.sayHello();
      }

      // Faz o logout
      connection.logout();
      System.out.println("Fim.");
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Método auxiliar responsável por procurar ofertas do serviço Hello.
   * 
   * @param context Contexto com o barramento.
   * @param serviceProperties Propriedades do serviço que estamos buscando.
   * @return Descritores com as ofertas dos serviços.
   */
  private static ServiceOfferDesc[] findServices(OpenBusContext context,
    ServiceProperty[] serviceProperties) {
    try {
      return context.getOfferRegistry().findServices(serviceProperties);
    }
    catch (ServiceFailure e) {
      System.out
        .println("Erro ao tentar realizar a busca por um serviço no barramento: Falha no serviço remoto. Causa:");
      e.printStackTrace(System.out);
    }
    catch (Exception e) {
      System.out
        .println("Erro inesperado ao tentar realizar a busca por um serviço no barramento:");
      e.printStackTrace(System.out);
    }
    return null;
  }

  /**
   * Método auxiliar responsável por procurar um serviço ativo entre as ofertas
   * encontradas.
   * 
   * @param services Ofertas de serviço.
   * @return Serviço ativo ou null se não houver nenhum.
   */
  private static Hello getHello(ServiceOfferDesc[] services) {
    if (services == null || services.length < 1) {
      System.err
        .println("O servidor do demo Hello não foi encontrado no barramento.");
      return null;
    }

    if (services.length > 1) {
      System.out
        .println("Existe mais de um serviço Hello no barramento. Tentaremos encontrar uma funcional.");
    }

    for (ServiceOfferDesc offerDesc : services) {
      System.out.println("Testando uma das ofertas recebidas...");

      try {
        org.omg.CORBA.Object helloObj =
          offerDesc.service_ref.getFacet(HelloHelper.id());
        if (helloObj == null) {
          System.out
            .println("Não foi possível encontrar uma faceta Hello na oferta.");
          continue;
        }

        Hello hello = HelloHelper.narrow(helloObj);
        if (hello == null) {
          System.out
            .println("Faceta encontrada na oferta não implementa Hello.");
          continue;
        }
        return hello;
      }
      catch (TRANSIENT e) {
        System.out
          .println("A oferta é de um serviço inativo. Tentando a próxima.");
      }
    }

    System.out
      .println("Não foi encontrada uma oferta com um serviço funcional.");
    return null;
  }

  /**
   * Método Auxiliar responsável por fazer o login por password no barramento.
   * 
   * @param conn Conexão a ser utilizada.
   * @param entity Entidade
   * @param pass Password
   * @return True quando obtiver de sucesso.
   */
  private static boolean login(Connection conn, String entity, String pass) {
    try {
      conn.loginByPassword(entity, pass.getBytes(Cryptography.CHARSET));
      return true;
    }
    catch (AlreadyLoggedIn e) {
      System.out
        .println("Falha ao tentar realizar o login por senha no barramento: a entidade já está com o login realizado. Esta falha será ignorada.");
      return true;
    }
    catch (AccessDenied e) {
      System.out
        .println("Erro ao tentar realizar o login por senha no barramento: a senha fornecida não foi validada para a entidade "
          + entity + ".");
    }
    catch (ServiceFailure e) {
      System.out
        .println("Erro ao tentar realizar o login por senha no barramento: Falha no serviço remoto. Causa:");
      e.printStackTrace(System.out);
    }
    catch (Exception e) {
      System.out
        .println("Erro inesperado ao tentar realizar o login por senha no barramento:");
      e.printStackTrace(System.out);
    }
    return false;
  }
}
