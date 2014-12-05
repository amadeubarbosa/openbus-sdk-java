package demo.interceptor;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName;

/**
 * Classe interna para inicialização do {@link ORB} do JacORB. Esta classe é
 * responsável por inserir os interceptadores cliente e servidor, que permitirão
 * inserir informações extras no contexto das chamadas CORBA através do
 * {@link ContextInspector}.
 * <p>
 * Esta classe não deveria ser visível fora deste pacote, mas o JacORB obriga
 * que ela seja pública.
 * 
 * @author Tecgraf
 */
public final class PersonalInitializer extends LocalObject implements
  ORBInitializer {

  /** Instância de logging */
  private static final Logger logger = Logger
    .getLogger(PersonalInitializer.class.getName());
  /** Major da versão do codificador. */
  private static final byte ENCODING_CDR_ENCAPS_MAJOR_VERSION = 1;
  /** Minor da versão do codificador. */
  private static final byte ENCODING_CDR_ENCAPS_MINOR_VERSION = 2;

  /**
   * {@inheritDoc}
   */
  @Override
  public void pre_init(ORBInitInfo info) {
    int mySlotId = info.allocate_slot_id();
    Codec codec = createCodec(info);
    ContextInspector inspector = new ContextInspector(mySlotId, codec);
    try {
      info.register_initial_reference(ContextInspector.INITIAL_REFERENCE_ID,
        inspector);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao registrar o inspetor de contexto";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    this.addClientInterceptor(info);
    this.addServerInterceptors(info);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void post_init(ORBInitInfo info) {

  }

  /**
   * Inclui o interceptador cliente.
   * 
   * @param info informação do ORB
   */
  private void addClientInterceptor(ORBInitInfo info) {
    try {
      info.add_client_request_interceptor(new ClientInterceptor(
        getInspector(info)));
    }
    catch (DuplicateName e) {
      String message = "Falha inesperada ao registrar o interceptador cliente";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
  }

  /**
   * Inclui o interceptador servidor.
   * 
   * @param info informação do ORB
   */
  private void addServerInterceptors(ORBInitInfo info) {
    try {
      info.add_server_request_interceptor(new ServerInterceptor(
        getInspector(info)));
    }
    catch (DuplicateName e) {
      String message = "Falha inesperada ao registrar o interceptador servidor";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
  }

  /**
   * Cria uma instância do Codec a ser utilizado pelos interceptadores.
   * 
   * @param info informação do ORB
   * @return o Codec.
   */
  private Codec createCodec(ORBInitInfo info) {
    CodecFactory codecFactory = info.codec_factory();

    Encoding encoding =
      new Encoding(ENCODING_CDR_ENCAPS.value,
        ENCODING_CDR_ENCAPS_MAJOR_VERSION, ENCODING_CDR_ENCAPS_MINOR_VERSION);

    try {
      return codecFactory.create_codec(encoding);
    }
    catch (UnknownEncoding e) {
      String message = "Falha inesperada ao criar o codificador";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
  }

  /**
   * Recupera o mediador do ORB
   * 
   * @param info informações do ORB
   * @return o mediador.
   */
  private ContextInspector getInspector(ORBInitInfo info) {
    org.omg.CORBA.Object obj;
    try {
      obj =
        info.resolve_initial_references(ContextInspector.INITIAL_REFERENCE_ID);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o inspetor";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    if (obj == null) {
      String message = "O inspetor não foi encontrado";
      logger.severe(message);
      throw new INITIALIZE(message);
    }
    return (ContextInspector) obj;
  }
}
