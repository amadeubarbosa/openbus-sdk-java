package tecgraf.openbus.core;

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
 * Classe internar para inicialização o {@link ORB} do JacORB.
 * <p>
 * Esta classe não deveria ser visível fora deste pacote, mas o JacORB obriga
 * que ela seja pública.
 * 
 * @author Tecgraf
 */
public final class InternalJacORBInitializer extends LocalObject implements
  ORBInitializer {
  /** Instância de logging */
  private static final Logger logger = Logger
    .getLogger(InternalJacORBInitializer.class.getName());
  /** Major da versão do codificador. */
  private static final byte ENCODING_CDR_ENCAPS_MAJOR_VERSION = 1;
  /** Minor da versão do codificador. */
  private static final byte ENCODING_CDR_ENCAPS_MINOR_VERSION = 2;

  /**
   * {@inheritDoc}
   */
  @Override
  public void pre_init(ORBInitInfo info) {
    Codec codec = this.createCodec(info);
    int signedChainSlotId = info.allocate_slot_id();
    int currentThreadSlotId = info.allocate_slot_id();
    int ignoreThreadSlotId = info.allocate_slot_id();
    int joinedChainSlotId = info.allocate_slot_id();
    int requestIdSlotId = info.allocate_slot_id();
    int invLoginSlotId = info.allocate_slot_id();
    OpenBusContextImpl context =
      new OpenBusContextImpl(currentThreadSlotId, ignoreThreadSlotId,
        invLoginSlotId);
    try {
      info.register_initial_reference("OpenBusContext", context);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao registrar o multiplexador";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    ORBMediator mediator =
      new ORBMediator(codec, signedChainSlotId, joinedChainSlotId,
        requestIdSlotId, context);
    try {
      info.register_initial_reference(ORBMediator.INITIAL_REFERENCE_ID,
        mediator);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao registrar o mediador";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    this.addClientInterceptor(info, mediator);
    this.addServerInterceptors(info, mediator);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void post_init(ORBInitInfo info) {
    try {
      OpenBusContextImpl context = (OpenBusContextImpl) info
        .resolve_initial_references("OpenBusContext");
      context.POA(null);
    } catch (InvalidName e) {
      String message = "Falha inesperada ao registrar o POA no multiplexador";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
  }

  /**
   * Inclui o interceptador cliente.
   * 
   * @param info informação do ORB
   * @param mediator mediador do ORB
   */
  private void addClientInterceptor(ORBInitInfo info, ORBMediator mediator) {
    try {
      info.add_client_request_interceptor(new ClientRequestInterceptorImpl(
        "ClientRequestInterceptor", mediator));
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
   * @param mediator mediador do ORB
   */
  private void addServerInterceptors(ORBInitInfo info, ORBMediator mediator) {
    try {
      info.add_server_request_interceptor(new ServerRequestInterceptorImpl(
        "ServerRequestInterceptor", mediator));
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
}
