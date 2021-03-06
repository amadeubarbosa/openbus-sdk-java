package tecgraf.openbus.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
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
import tecgraf.openbus.OfferRegistry;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * M�todos utilit�rios sobre uso da biblioteca ou sobre conceitos OpenBus
 *
 * @author Tecgraf/PUC-Rio
 */
public class LibUtils {

  /**
   * Converte uma cadeia para uma representa��o textual.
   * 
   * @param chain a cadeia
   * @return uma representa��o textual da mesma.
   */
  static public String chain2str(CallerChain chain) {
    StringBuilder buffer = new StringBuilder();
    for (LoginInfo loginInfo : chain.originators()) {
      buffer.append(loginInfo.entity);
      buffer.append("->");
    }
    buffer.append(chain.caller().entity);
    return buffer.toString();
  }

  /**
   * Constr�i uma inst�ncia de {@link Codec}
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
   * Thread para execu��o do {@link ORB#run()}
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
   * Thread de finaliza��o do ORB que poderia ser inclu�da no
   * {@link Runtime#addShutdownHook(Thread)} para realizar limpezas necess�rias
   *
   * @author Tecgraf/PUC-Rio
   */
  public static class ShutdownThread extends Thread {
    /** o orb */
    private ORB orb;
    /** lista de conex�es a serem liberadas */
    private List<Connection> conns = new ArrayList<>();
    /** lista de ofertas a serem liberadas */
    private List<RemoteOffer> offers = new ArrayList<>();

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

      for (RemoteOffer offer : this.offers) {
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
     * Inclui uma conex�o na lista de conex�es a serem liberadas pela thread
     * 
     * @param conn a conex�o
     */
    public void addConnetion(Connection conn) {
      this.conns.add(conn);
    }

    /**
     * Inclui uma oferta na lista de ofertas a serem liberadas pela thread
     * 
     * @param offer a oferta
     */
    public void addOffer(RemoteOffer offer) {
      this.offers.add(offer);
    }

  }

  /**
   * Realiza um busca por ofertas com as propriedades solicitadas, repeitando as
   * regras de retentativas e espera entre tentativas. Caso as ofertas n�o sejam
   * encontradas lan�a-se uma exce��o de {@link IllegalStateException}
   * 
   * @param offers servi�o de registro de ofertas do barramento
   * @param search propriedades da busca
   * @param count n�mero m�nimo de ofertas que se espera encontrar
   * @param tries n�mero de tentativas
   * @param interval intervalo de espera entre as tentativas (em segundos)
   * @return ofertas encontradas na busca.
   * @throws ServiceFailure
   */
  public static List<RemoteOffer> findOffer(OfferRegistry offers,
    ArrayListMultimap<String, String> search, int count, int tries,
    int interval) throws ServiceFailure {
    List<RemoteOffer> found = new ArrayList<>();
    for (int i = 0; i < tries; i++) {
      found.clear();
      try {
        Thread.sleep(interval * 1000);
      }
      catch (InterruptedException e1) {
        // continue...
      }
      List<RemoteOffer> services = offers.findServices(search);
      if (services.size() > 0) {
        for (RemoteOffer offer : services) {
          try {
            if (!offer.service()._non_existent()) {
              found.add(offer);
            }
          }
          catch (Exception ignored) {
          }
        }
      }
      if (found.size() >= count) {
        return found;
      }
    }
    StringBuilder buffer = new StringBuilder();
    for (RemoteOffer offer : found) {
      ArrayListMultimap<String, String> props = offer.properties();
      String name = props.get("openbus.offer.entity").get(0);
      String login = props.get("openbus.offer.login").get(0);
      buffer.append(String.format("\n - %s (%s)", name, login));
    }
    String msg =
      String
        .format(
          "n�o foi poss�vel encontrar ofertas: found (%d) expected(%d) tries (%d) time (%d)%s",
          found.size(), count, tries, tries * interval, buffer.toString());
    throw new IllegalStateException(msg);
  }

  /**
   * L� um arquivo de IOR e retorna a linha que representa o IOR
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
