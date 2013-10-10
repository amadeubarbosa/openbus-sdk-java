package demo.interceptor;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.InvalidSlot;

import tecgraf.openbus.exception.OpenBusInternalException;

/**
 * Classe responsável por manipular as informações extras enviadas através do
 * {@link ServiceContext} nas chamadas CORBA.
 * 
 * @author Tecgraf
 */
public class ContextInspector extends LocalObject {

  /** Instância de logging */
  private static final Logger logger = Logger.getLogger(ContextInspector.class
    .getName());

  /**
   * Nome referência para acessar este objeto através do {@link ORB}.
   */
  public static final String INITIAL_REFERENCE_ID = ContextInspector.class
    .getSimpleName();

  /**
   * O ORB associado.
   */
  private ORB orb;

  /**
   * O slot reservado para transmitir a informação extra.
   */
  private int mySlotId;

  /**
   * O codificador
   */
  private Codec codec;

  /**
   * O código de identificador de contexto a ser utilizado para transmitir as
   * informações nas chamadas CORBA.
   */
  private int serviceContextId;

  /**
   * Construtor.
   * 
   * @param mySlotId o identidicador de slot alocado.
   * @param codec o codificador gerado.
   */
  ContextInspector(int mySlotId, Codec codec) {
    this.mySlotId = mySlotId;
    this.codec = codec;
  }

  /**
   * Recupera o identificador de slot alocado.
   * 
   * @return o identificador de slot.
   */
  int getMySlotId() {
    return mySlotId;
  }

  /**
   * Recupera o codificador a ser utilizado.
   * 
   * @return o codificador.
   */
  Codec getCodec() {
    return codec;
  }

  /**
   * Configura o ORB ao qual esta classe esta associada
   * 
   * @param orb o ORB
   */
  void setORB(ORB orb) {
    this.orb = orb;
  }

  /**
   * Recupera o ORB ao qual esta classe esta associada
   * 
   * @return o ORB
   */
  public ORB getORB() {
    return this.orb;
  }

  /**
   * Define o identificador de contexto a ser utilizado.
   * 
   * @param id o identificador de contexto.
   */
  void setContextId(int id) {
    this.serviceContextId = id;
  }

  /**
   * Recupera o identificador de contexto a ser utilizado.
   * 
   * @return id o identificador de contexto.
   */
  int getContextId() {
    return this.serviceContextId;
  }

  /**
   * Recupera o {@link Current} da thread em execução do ORB associado.
   * 
   * @param orb o orb utilizado.
   * @return o {@link Current}.
   * @throws OpenBusInternalException
   */
  private Current getPICurrent(ORB orb) throws OpenBusInternalException {
    org.omg.CORBA.Object obj;
    try {
      obj = orb.resolve_initial_references("PICurrent");
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o PICurrent";
      throw new OpenBusInternalException(message, e);
    }
    return CurrentHelper.narrow(obj);
  }

  /**
   * Recupera a informação que foi embutida no contexto da chamada.
   * 
   * @return a informação.
   */
  public String getContextInformation() {
    Current current = getPICurrent(orb);
    try {
      Any any = current.get_slot(mySlotId);
      if (any.type().kind().value() == TCKind._tk_null) {
        return null;
      }
      return any.extract_string();
    }
    catch (InvalidSlot e) {
      throw new IllegalStateException(
        "Não foi possível recuperar informação no slotId", e);
    }
  }

  /**
   * Configura a informação a ser embutida no contexto da chamada. Caso a
   * informação sendo configurada for nula, então a informação é removida do
   * contexto de chamadas.
   * 
   * @param info a informação.
   */
  public void setContextInformation(String info) {
    Current current = getPICurrent(orb);
    Any any = orb.create_any();
    if (info != null) {
      any.insert_string(info);
    }
    try {
      current.set_slot(mySlotId, any);
    }
    catch (InvalidSlot e) {
      throw new IllegalStateException(
        "Não foi possível salvar informação no slotId", e);
    }
  }

  /**
   * Limpa a informação a ser associada ao contexto das chamadas CORBA.
   */
  public void clearContextInformation() {
    setContextInformation(null);
  }

  /**
   * Recupera a referência para o {@link ContextInspector} à partir do ORB.
   * 
   * @param orb o ORB,
   * @return o {@link ContextInspector}
   */
  public static ContextInspector getContextInspector(ORB orb) {
    org.omg.CORBA.Object obj;
    try {
      obj =
        orb.resolve_initial_references(ContextInspector.INITIAL_REFERENCE_ID);
    }
    catch (InvalidName e) {
      String message = "Falha inesperada ao obter o inspetor de contexto";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    if (obj == null) {
      String message = "O inspetor de contexto não foi encontrado";
      logger.severe(message);
      throw new INITIALIZE(message);
    }
    return (ContextInspector) obj;
  }

}
