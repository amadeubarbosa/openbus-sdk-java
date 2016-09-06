package tecgraf.openbus.offers;

import scs.core.Facet;
import scs.core.IComponent;
import scs.core.IMetaInterface;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;

/**
 * Classe utilizada como índice de referência de propriedades nativas do
 * barramento (propriedades automáticas).
 * <p>
 * As constantes da classe são utilizadas pelo núcleo da biblioteca Openbus,
 * durante a publicação, e por consumidores na atividade de busca de serviços no
 * barramento.
 * <p>
 * O usuário pode definir propriedades com chaves próprias que vão além das
 * chaves apresentadas nesta classe.
 * 
 * @see ServiceProperty
 * 
 * @author Tecgraf
 */
public class ServiceProperties {

  /**
   * Identificador único de uma oferta de serviço no barramento.
   * <p>
   * <b>Literal:</b> {@code openbus.offer.id}
   */
  public final static String ID = "openbus.offer.id";

  /**
   * Identificador do login utilizado na publicação do serviço no barramento.
   * <p>
   * <b>Literal:</b> {@code openbus.offer.login}
   */
  public final static String LOGIN = "openbus.offer.login";

  /**
   * Nome da entidade que publicou o serviço no barramento.
   * <p>
   * <i>A partir da versão do OpenBus 2.0, este valor é referente a entidades
   * autenticadas unicamente através de certificados de segurança.</i>
   * 
   * <p>
   * <b>Literal:</b> {@code openbus.offer.entity}
   */
  public final static String ENTITY = "openbus.offer.entity";

  /**
   * Marca temporal. Cadeia de caracteres que denota a data em que o serviço foi
   * publicado. Equivalente ao método {@code Date.getTimeInMillis()}
   * <p>
   * <b>Literal:</b> {@code openbus.offer.timestamp}
   * 
   * @see java.util.Date
   */
  public final static String TIMESTAMP = "openbus.offer.timestamp";

  /**
   * Ano de publicação do serviço no barramento.
   * 
   * <p>
   * <b>Literal:</b> {@code openbus.offer.year}
   */
  public final static String YEAR = "openbus.offer.year";

  /**
   * Mês em que o serviço foi publicado no barramento (1-12). O número 1 é
   * referente ao mês de janeiro, o número 2 ao mês de fevereiro, e assim
   * consecutivamente.
   * 
   * <p>
   * <b>Literal:</b> {@code openbus.offer.month}
   */
  public final static String MONTH = "openbus.offer.month";

  /**
   * Dia do mês em que o serviço foi publicado no barramento (1-31). O número 1
   * é referente ao primeiro dia do mês, e assim consecutivamente.
   * <p>
   * <b>Literal:</b> {@code openbus.offer.day}
   */
  public final static String DAY = "openbus.offer.day";

  /**
   * Hora do dia em que o serviço foi publicado no barramento (0-23).
   * <p>
   * <b>Literal:</b> {@code openbus.offer.hour}
   */
  public final static String HOUR = "openbus.offer.hour";

  /**
   * Minutos da hora em que o serviço foi publicado no barramento (0-59).
   * <p>
   * <b>Literal:</b> {@code openbus.offer.minute}
   */
  public final static String MINUTE = "openbus.offer.minute";

  /**
   * Segundos do minuto em que o serviço foi publicado no barramento (0-59).
   * <p>
   * <b>Literal:</b> {@code openbus.offer.second}
   */
  public final static String SECOND = "openbus.offer.second";

  /**
   * Nome do componente publicado no barramento.
   * 
   * <p>
   * <b>Literal:</b> {@code openbus.component.name}
   */
  public final static String COMPONENT_NAME = "openbus.component.name";

  /**
   * Interfaces publicadas pelo componente no barramento. Este valor é
   * qualificado de acordo com o contrato em IDL.
   * <p>
   * <i>Ex.: IDL:scs/core/IReceptacles:1.0</i>
   * <p>
   * Pode ser obtido a partir do {@code id} da classe Helper gerada a
   * partir do contrato IDL.
   * <p>
   * <b>Literal:</b> {@code openbus.component.interface}
   * 
   */
  public final static String COMPONENT_INTERFACE =
    "openbus.component.interface";

  /**
   * Nomes de facetas fornecidas pelo componente, segundo o modelo de
   * componentes SCS.
   * <p>
   * <i>Ex.: IMetaInterface, IComponent, UOService...</i>
   * <p>
   * Os nomes das facetas podem ser definidos pelo desenvolvedor.
   * <p>
   * <b>Literal:</b> {@code openbus.component.facet}
   * 
   * @see Facet
   * @see IComponent
   * @see IMetaInterface
   */
  public final static String COMPONENT_FACET = "openbus.component.facet";

  /**
   * Número principal de versionamento, indica incrementos significativos em
   * funcionalidade.
   * <p>
   * <b>Literal:</b> {@code openbus.component.version.major}
   */
  public final static String MAJOR_VERSION = "openbus.component.version.major";

  /**
   * Número secundário de versionamento, indica alteração de funcionalidades
   * menores ou correção de bugs que impactam na solução.
   * <p>
   * <b>Literal:</b> {@code openbus.component.version.minor}
   */
  public final static String MINOR_VERSION = "openbus.component.version.minor";

  /**
   * Número de <i>patch</i> de versionamento, indica correções de bugs e
   * alterações que não impactam na solução.
   * <p>
   * <b>Literal:</b> {@code openbus.component.version.patch}
   */
  public final static String PATCH_VERSION = "openbus.component.version.patch";
}
