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
  private Codec codec;
  /** Identificador do slot da cadeia assinada */
  private final int SIGNED_CHAIN_SLOT_ID;
  /** Identificador do slot de joined chain */
  private final int JOINED_CHAIN_SLOT_ID;
  /** Identificador do slot da caller chain */
  private final int CONNECTION_SLOT_ID;
  /** o ORB */
  private ORB orb;
  /** o multiplexador */
  private ConnectionManagerImpl connections;

  /**
   * Construtor.
   * 
   * @param codec a f�brica de codificadores
   * @param signedChainSlotId identificador de slot.
   * @param chainSlotId identificaor de slot.
   * @param connectionSlotId identificador de slot
   * @param connections gerente de conex�es associado.
   */
  ORBMediator(Codec codec, int signedChainSlotId, int chainSlotId,
    int connectionSlotId, ConnectionManagerImpl connections) {
    this.codec = codec;
    this.SIGNED_CHAIN_SLOT_ID = signedChainSlotId;
    this.JOINED_CHAIN_SLOT_ID = chainSlotId;
    this.CONNECTION_SLOT_ID = connectionSlotId;
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
   * Recupera o identificador do slot onde se informa qual a conex�o que
   * realizou o dispatch e por isso � detentora da Cadeia. Utilizado pelo lado
   * servidor.
   * 
   * @return identificador do slot
   */
  int getConnectionSlotId() {
    return this.CONNECTION_SLOT_ID;
  }

  /**
   * Recupera o gerente de conex�es do ORB.
   * 
   * @return o gerente de conex�es.
   */
  public ConnectionManagerImpl getConnectionManager() {
    return connections;
  }

}
