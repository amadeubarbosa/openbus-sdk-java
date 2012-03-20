package tecgraf.openbus.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;
import org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName;

import tecgraf.openbus.ConnectionMultiplexer;

final class ORBInitializerImpl extends LocalObject implements ORBInitializer {
  private static final byte ENCODING_CDR_ENCAPS_MAJOR_VERSION = 1;
  private static final byte ENCODING_CDR_ENCAPS_MINOR_VERSION = 2;
  private static final Logger logger = Logger
    .getLogger(ORBInitializerImpl.class.getName());
  private int credentialSlotId;

  @Override
  public void pre_init(ORBInitInfo info) {
    Codec codec = this.createCodec(info);
    this.credentialSlotId = info.allocate_slot_id();
    ORBMediator mediator = new ORBMediator(codec, this.credentialSlotId);
    try {
      info.register_initial_reference(ORBMediator.INITIAL_REFERENCE_ID,
        mediator);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao registrar o mediador";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    ConnectionMultiplexerImpl multiplexer = new ConnectionMultiplexerImpl();
    try {
      info.register_initial_reference(
        ConnectionMultiplexer.INITIAL_REFERENCE_ID, multiplexer);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao registrar o multiplexador";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
  }

  @Override
  public void post_init(ORBInitInfo info) {
    ORBMediator mediator = this.getMediator(info);
    this.addClientInterceptor(info, mediator);
    this.addServerInterceptors(info, mediator);
  }

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

  private ORBMediator getMediator(ORBInitInfo info) {
    org.omg.CORBA.Object obj;
    try {
      obj = info.resolve_initial_references(ORBMediator.INITIAL_REFERENCE_ID);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o mediador";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    if (obj == null) {
      String message = "O mediador não foi encontrado";
      logger.severe(message);
      throw new INITIALIZE(message);
    }
    return (ORBMediator) obj;
  }

  private Codec createCodec(ORBInitInfo info) {
    org.omg.CORBA.Object obj;
    try {
      obj = info.resolve_initial_references("CodecFactory");
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter a fábrica de codificadores";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    CodecFactory codecFactory = CodecFactoryHelper.narrow(obj);

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
