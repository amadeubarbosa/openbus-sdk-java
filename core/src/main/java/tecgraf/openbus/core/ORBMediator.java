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
  private final Codec codec;
  /** Identificador do slot da cadeia assinada */
  private final int SIGNED_CHAIN_SLOT_ID;
  /** Identificador do slot de joined chain */
  private final int JOINED_CHAIN_SLOT_ID;
  /** Identificador de slot do identificador do request */
  private final int REQUEST_ID_SLOT_ID;
  /** o ORB */
  private ORB orb;
  /** o multiplexador */
  private final OpenBusContextImpl connections;

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
   * @param requestingConnSlotId identificador de slot.
   * @param connections gerente de conexões associado.
   */
  ORBMediator(Codec codec, int signedChainSlotId, int chainSlotId,
    int requestingConnSlotId, OpenBusContextImpl connections) {
    this.codec = codec;
    this.SIGNED_CHAIN_SLOT_ID = signedChainSlotId;
    this.JOINED_CHAIN_SLOT_ID = chainSlotId;
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
  OpenBusContextImpl getContext() {
    return connections;
  }

}
