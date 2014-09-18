package tecgraf.openbus.core;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;

/**
 * Classe utilizada para guardar informações úteis para os interceptadores.
 * 
 * @author Tecgraf
 */
final class ORBMediator extends LocalObject {
  /** Identificador do ORB Mediator */
  public static final String INITIAL_REFERENCE_ID = "openbus.ORBMediator";

  /** Fábrica de codificadores. */
  private Codec codec;
  /** Identificador do slot da cadeia assinada */
  private final int SIGNED_CHAIN_SLOT_ID;
  /** Identificador do slot de joined chain */
  private final int JOINED_CHAIN_SLOT_ID;
  /** Identificador do slot do target da joined chain */
  private final int JOINED_CHAIN_TARGET_SLOT_ID;
  /** Identificador do slot de barramento ao qual joined chain pertence */
  private final int JOINED_BUS_SLOT_ID;
  /** Identificador do slot da caller chain */
  private final int BUS_SLOT_ID;
  /** Identificador de slot do identificador do request */
  private int REQUEST_ID_SLOT_ID;
  /** o ORB */
  private ORB orb;
  /** o multiplexador */
  private OpenBusContextImpl connections;

  /** Contador gerador de IDs únicos */
  private int counter = 0;
  /** Tamanho máximo do contador */
  private static final int MAX_COUNTER = 0x0000ffff;

  /**
   * Gera um identificador a partir do incremento de um contador.
   * 
   * @return o identificador gerado.
   */
  public synchronized int getUniqueId() {
    counter = (counter + 1) & MAX_COUNTER;
    return counter;
  }

  /**
   * Construtor.
   * 
   * @param codec a fábrica de codificadores
   * @param signedChainSlotId identificador de slot.
   * @param chainSlotId identificaor de slot.
   * @param chainTargetSlotId identificador de slot.
   * @param joinedBusSlotId identificador de slot.
   * @param busSlotId identificador de slot
   * @param requestingConnSlotId identificador de slot.
   * @param connections gerente de conexões associado.
   */
  ORBMediator(Codec codec, int signedChainSlotId, int chainSlotId,
    int chainTargetSlotId, int joinedBusSlotId, int busSlotId,
    int requestingConnSlotId, OpenBusContextImpl connections) {
    this.codec = codec;
    this.SIGNED_CHAIN_SLOT_ID = signedChainSlotId;
    this.JOINED_CHAIN_SLOT_ID = chainSlotId;
    this.JOINED_CHAIN_TARGET_SLOT_ID = chainTargetSlotId;
    this.JOINED_BUS_SLOT_ID = joinedBusSlotId;
    this.BUS_SLOT_ID = busSlotId;
    this.REQUEST_ID_SLOT_ID = requestingConnSlotId;
    this.connections = connections;
  }

  /**
   * Recupera o ORB associado.
   * 
   * @return o ORB
   */
  ORB getORB() {
    return this.orb;
  }

  /**
   * Salva o ORB que esta associado aos interceptadores.
   * 
   * @param orb o orb associado aos interceptadores.
   */
  void setORB(ORB orb) {
    this.orb = orb;
  }

  /**
   * Recupera a fábrica de codificadores.
   * 
   * @return a fábrica de codificadores.
   */
  Codec getCodec() {
    return this.codec;
  }

  /**
   * Recupera o identificador do slot onde se guarda a cadeia assinada associada
   * ao request recebido. Utilizado pelo lado servidor.
   * 
   * @return identificador do slot
   */
  int getSignedChainSlotId() {
    return this.SIGNED_CHAIN_SLOT_ID;
  }

  /**
   * Recupera o indentificador do slot onde se guarda a cadeia assinada que a
   * requisição de saída esta "joined". Utilizado pelo lado cliente.
   * 
   * @return identificador do slot
   */
  int getJoinedChainSlotId() {
    return this.JOINED_CHAIN_SLOT_ID;
  }

  /**
   * Recupera o indentificador do slot onde se guarda o targer da cadeia
   * assinada que a requisição de saída esta "joined". Utilizado pelo lado
   * cliente.
   * 
   * @return identificador do slot
   */
  int getJoinedChainTargetSlotId() {
    return this.JOINED_CHAIN_TARGET_SLOT_ID;
  }

  /**
   * Recupera o identificador do slot onde se informa qual o identificador do
   * barramento pelo qual a chamada se realiza. Utilizado pelo lado servidor.
   * 
   * @return identificador do slot
   */
  int getBusSlotId() {
    return this.BUS_SLOT_ID;
  }

  /**
   * Recupera o identificador do slot onde se informa qual o identificador do
   * barramento a cadeia em que esta encadeado ('joined').
   * 
   * @return identificador do slot
   */
  int getJoinedBusSlotId() {
    return this.JOINED_BUS_SLOT_ID;
  }

  /**
   * Recupera o identificador do slot onde se informa qual requisição associada.
   * 
   * @return identificador do slot
   */
  int getUniqueIdSlot() {
    return this.REQUEST_ID_SLOT_ID;
  }

  /**
   * Recupera o gerente de conexões do ORB.
   * 
   * @return o gerente de conexões.
   */
  public OpenBusContextImpl getContext() {
    return connections;
  }

}
