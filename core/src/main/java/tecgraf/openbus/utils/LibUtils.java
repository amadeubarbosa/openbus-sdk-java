package tecgraf.openbus.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;

/**
 * Métodos utilitários sobre uso da biblioteca ou sobre conceitos OpenBus
 *
 * @author Tecgraf/PUC-Rio
 */
public class LibUtils {

  /**
   * Busca por uma propriedade dentro da lista de propriedades
   * 
   * @param props a lista de propriedades
   * @param key a chave da propriedade buscada
   * @return o valor da propriedade ou <code>null</code> caso não encontrada
   */
  static public String findProperty(ServiceProperty[] props, String key) {
    for (int i = 0; i < props.length; i++) {
      ServiceProperty property = props[i];
      if (property.name.equals(key)) {
        return property.value;
      }
    }
    return null;
  }

  /**
   * Converte uma cadeia para uma representação textual.
   * 
   * @param chain a cadeia
   * @return uma representação textual da mesma.
   */
  static public String chain2str(CallerChain chain) {
    StringBuffer buffer = new StringBuffer();
    for (LoginInfo loginInfo : chain.originators()) {
      buffer.append(loginInfo.entity);
      buffer.append("->");
    }
    buffer.append(chain.caller().entity);
    return buffer.toString();
  }

  /**
   * Constrói uma instância de {@link Codec}
   * 
   * @param orb o {@link ORB}
   * @return um {@link Codec}
   * @throws UnknownEncoding
   * @throws InvalidName
   */
  public static Codec createCodec(ORB orb) throws UnknownEncoding, InvalidName {
    org.omg.CORBA.Object obj;
    obj = orb.resolve_initial_references("CodecFactory");
    CodecFactory codecFactory = CodecFactoryHelper.narrow(obj);
    byte major = 1;
    byte minor = 2;
    Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, major, minor);
    return codecFactory.create_codec(encoding);
  }

  /**
   * Thread para execução do {@link ORB#run()}
   *
   * @author Tecgraf/PUC-Rio
   */
  public static class ORBRunThread extends Thread {
    /** o orb */
    private ORB orb;

    /**
     * Construtor
     * 
     * @param orb o orb
     */
    public ORBRunThread(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void run() {
      this.orb.run();
    }
  }

  /**
   * Thread de finalização do ORB que poderia ser incluída no
   * {@link Runtime#addShutdownHook(Thread)} para realizar limpezas necessárias
   *
   * @author Tecgraf/PUC-Rio
   */
  public static class ShutdownThread extends Thread {
    /** o orb */
    private ORB orb;
    /** lista de conexões a serem liberadas */
    private List<Connection> conns = new ArrayList<Connection>();
    /** lista de ofertas a serem liberadas */
    private List<ServiceOffer> offers = new ArrayList<ServiceOffer>();

    /**
     * Construtor
     * 
     * @param orb o orb
     */
    public ShutdownThread(ORB orb) {
      this.orb = orb;
    }

    @Override
    public void run() {

      for (ServiceOffer offer : this.offers) {
        try {
          offer.remove();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }

      for (Connection conn : this.conns) {
        try {
          conn.logout();
        }
        catch (ServiceFailure e) {
          e.printStackTrace();
        }
      }
      this.orb.shutdown(true);
      this.orb.destroy();
    }

    /**
     * Inclui uma conexão na lista de conexões a serem liberadas pela thread
     * 
     * @param conn a conexão
     */
    public void addConnetion(Connection conn) {
      this.conns.add(conn);
    }

    /**
     * Inclui uma oferta na lista de ofertas a serem liberadas pela thread
     * 
     * @param offer a oferta
     */
    public void addOffer(ServiceOffer offer) {
      this.offers.add(offer);
    }

  }

  /**
   * Realiza um busca por ofertas com as propriedades solicitadas, repeitando as
   * regras de retentativas e espera entre tentativas. Caso as ofertas não sejam
   * encontradas lança-se uma exceção de {@link IllegalStateException}
   * 
   * @param offers serviço de registro de ofertas do barramento
   * @param search propriedades da busca
   * @param count número mínimo de ofertas que se espera encontrar
   * @param tries número de tentativas
   * @param interval intervalo de espera entre as tentativas (em segundos)
   * @return ofertas encontradas na busca.
   * @throws ServiceFailure
   */
  public static List<ServiceOfferDesc> findOffer(OfferRegistry offers,
    ServiceProperty[] search, int count, int tries, int interval)
    throws ServiceFailure {
    List<ServiceOfferDesc> found = new ArrayList<ServiceOfferDesc>();
    for (int i = 0; i < tries; i++) {
      found.clear();
      try {
        Thread.sleep(interval * 1000);
      }
      catch (InterruptedException e1) {
        // continue...
      }
      ServiceOfferDesc[] services = offers.findServices(search);
      if (services.length > 0) {
        for (ServiceOfferDesc offerDesc : services) {
          try {
            if (!offerDesc.service_ref._non_existent()) {
              found.add(offerDesc);
            }
          }
          catch (Exception e) {
            continue;
          }
        }
      }
      if (found.size() >= count) {
        return found;
      }
    }
    StringBuffer buffer = new StringBuffer();
    for (ServiceOfferDesc desc : found) {
      String name =
        LibUtils.findProperty(desc.properties, "openbus.offer.entity");
      String login =
        LibUtils.findProperty(desc.properties, "openbus.offer.login");
      buffer.append(String.format("\n - %s (%s)", name, login));
    }
    String msg =
      String
        .format(
          "não foi possível encontrar ofertas: found (%d) expected(%d) tries (%d) time (%d)%s",
          found.size(), count, tries, tries * interval, buffer.toString());
    throw new IllegalStateException(msg);
  }

  /**
   * Lê um arquivo de IOR e retorna a linha que representa o IOR
   * 
   * @param iorfile path para arquivo
   * @return a String do IOR
   * @throws IOException
   */
  public static String file2IOR(String iorfile) throws IOException {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(iorfile));
      return in.readLine();
    }
    finally {
      if (in != null) {
        in.close();
      }
    }
  }
}
