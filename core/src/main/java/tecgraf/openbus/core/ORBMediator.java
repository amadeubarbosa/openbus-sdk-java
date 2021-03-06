package tecgraf.openbus.core;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;

/**
 * Classe utilizada para guardar informa��es �teis para os interceptadores.
 * 
 * @author Tecgraf
 */
final class ORBMediator extends LocalObject {
  /** Identificador do ORB Mediator */
  public static final String INITIAL_REFERENCE_ID = "openbus.ORBMediator";

  /** F�brica de codificadores. */
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

  /** Contador gerador de IDs �nicos */
  private int counter = 0;
  /** Tamanho m�ximo do contador */
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
   * @param codec a f�brica de codificadores
   * @param signedChainSlotId identificador de slot.
   * @param chainSlotId identificaor de slot.
   * @param requestingConnSlotId identificador de slot.
   * @param connections gerente de conex�es associado.
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
   * Recupera a f�brica de codificadores.
   * 
   * @return a f�brica de codificadores.
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
   * requisi��o de sa�da esta "joined". Utilizado pelo lado cliente.
   * 
   * @return identificador do slot
   */
  int getJoinedChainSlotId() {
    return this.JOINED_CHAIN_SLOT_ID;
  }

  /**
   * Recupera o identificador do slot onde se informa qual requisi��o associada.
   * 
   * @return identificador do slot
   */
  int getUniqueIdSlot() {
    return this.REQUEST_ID_SLOT_ID;
  }

  /**
   * Recupera o gerente de conex�es do ORB.
   * 
   * @return o gerente de conex�es.
   */
  OpenBusContextImpl getContext() {
    return connections;
  }

}
