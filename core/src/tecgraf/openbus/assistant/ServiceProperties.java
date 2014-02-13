package tecgraf.openbus.assistant;

import scs.core.Facet;
import scs.core.IComponent;
import scs.core.IMetaInterface;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;

/**
 * Classe utilizada como �ndice de refer�ncia de propriedades nativas do barramento 
 * (propriedades autom�ticas).
 * <p>
 * As constantes da classe s�o utilizadas pelo core do Openbus, durante a
 * publica��o, e por consumidores na atividade de busca de servi�os no
 * barramento.
 * <p>
 * E finalmente, o usu�rio pode definir propriedades com chaves customizadas que
 * v�o al�m das chaves apresentadas nesta classe.
 * 
 * @see ServiceProperty
 * 
 * @author Tecgraf
 */
public class ServiceProperties {

	/**
	 * Identificador �nico da oferta do servi�o no barramento.
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.id</code>
	 */
	public final static String ID = "openbus.offer.id";

	/**
	 * Identificador do login utilizado na publica��o do servi�o no barramento.
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.login</code>
	 */
	public final static String LOGIN = "openbus.offer.login";

	/**
	 * Nome da entidade que publicou o servi�o no barramento.
	 * <p>
	 * <i>A partir da vers�o do OpenBus 2.0, este valor � referente a entidades
	 * autenticadas unicamente atrav�s de certificados de seguran�a.</i>
	 * 
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.entity</code>
	 */
	public final static String ENTITY = "openbus.offer.entity";

	/**
	 * Marca temporal. Cadeia de caracteres que denota a data em que o servi�o
	 * foi publicado. Equivalente ao m�todo <code>Date.getTimeInMillis()</code>
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.timestamp</code>
	 * 
	 * @see java.util.Date
	 */
	public final static String TIMESTAMP = "openbus.offer.timestamp";

	/**
	 * Ano de publica��o do servi�o no barramento.
	 * 
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.year</code>
	 */
	public final static String YEAR = "openbus.offer.year";

	/**
	 * M�s em que o servi�o foi publicado no barramento (1-12). O n�mero 1 �
	 * referente ao m�s de janeiro, o n�mero 2 ao m�s de fevereiro, e assim
	 * consecutivamente.
	 * 
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.month</code>
	 */
	public final static String MONTH = "openbus.offer.month";

	/**
	 * Dia do m�s em que o servi�o foi publicado no barramento (1-31). O n�mero
	 * 1 � referente ao primeiro dia do m�s, e assim consecutivamente.
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.day</code>
	 */
	public final static String DAY = "openbus.offer.day";

	/**
	 * Hora do dia em que o servi�o foi publicado no barramento (0-23).
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.hour</code>
	 */
	public final static String HOUR = "openbus.offer.hour";

	/**
	 * Minutos da hora em que o servi�o foi publicado no barramento (0-59).
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.minute</code>
	 */
	public final static String MINUTE = "openbus.offer.minute";

	/**
	 * Segundos do minuto em que o servi�o foi publicado no barramento (0-59).
	 * <p>
	 * <b>Literal:</b> <code>openbus.offer.second</code>
	 */
	public final static String SECOND = "openbus.offer.second";

	/**
	 * Nome do componente publicado no barramento.
	 * 
	 * <p>
	 * <b>Literal:</b> <code>openbus.component.name</code>
	 */
	public final static String COMPONENT_NAME = "openbus.component.name";

	/**
	 * Interfaces publicadas pelo componente no barramento. Este valor �
	 * qualificado de acordo com o contrato IDL.
	 * <p>
	 * <i>Ex.: IDL:scs/core/IReceptacles:1.0</i>
	 * <p>
	 * E pode ser obtido a partir do <code>id</code> da classe Helper gerada a
	 * partir do contrato IDL.	 * 
	 *  <p>
	 * <b>Literal:</b> <code>openbus.component.interface</code>
	 * 
	 */
	public final static String COMPONENT_INTERFACE = "openbus.component.interface";

	/**
	 * Nomes de facetas fornecidas pelo componente, segundo o modelo de componentes SCS.
	 * <p>
	 * <i>Ex.: IMetaInterface, IComponent, UOService...</i>
	 * <p>
	 * Os nomes das facetas podem ser definidos pelo desenvolvedor.
	 * <p>	 
	 * <b>Literal:</b> <code>openbus.component.facet</code>
	 * 
	 * @see Facet
	 * @see IComponent
	 * @see IMetaInterface

	 */
	public final static String COMPONENT_FACET = "openbus.component.facet";

	/**
	 * N�mero principal de versionamento, indica incrementos significativos em
	 * funcionalidade.
	 * <p>
	 * <b>Literal:</b> <code>openbus.component.version.major</code>
	 */
	public final static String MAJOR_VERSION = "openbus.component.version.major";

	/**
	 * N�mero secund�rio de versionamento, indica altera��o de funcionalidades
	 * menores ou corre��o de bugs que impactam na solu��o.
	 * <p>
	 * <b>Literal:</b> <code>openbus.component.version.minor</code>
	 */
	public final static String MINOR_VERSION = "openbus.component.version.minor";

	/**
	 * N�mero de patch de versionamento, indica corre��es de bugs e altera��es
	 * que n�o impactam na solu��o.
	 * <p>
	 * <b>Literal:</b> <code>openbus.component.version.patch</code>
	 */
	public final static String PATCH_VERSION = "openbus.component.version.patch";

}
