package demo;

import tecgraf.openbus.OpenBusContext;

/**
 * Implementa��o do componente Greetings
 * 
 * @author Tecgraf
 */
public class GreetingsImpl extends GreetingsPOA {

  /**
   * L�nguas em que se realiza o cumprimento.
   * 
   * @author Tecgraf
   */
  static public enum Language {
    /** Ingl�s */
    English,
    /** Espanhol */
    Spanish,
    /** Portugu�s */
    Portuguese;
  };

  /**
   * Per�odos em que se realiza o cumprimento.
   * 
   * @author Tecgraf
   */
  static public enum Period {
    /** Manh� */
    Morning,
    /** Tarde */
    Afternoon,
    /** Noite */
    Night;
  };

  /** Contexto com o barramento. */
  private OpenBusContext context;
  /** O per�odo */
  private Period period;
  /** A l�ngua */
  private Language language;

  /**
   * Construtor.
   * 
   * @param context Conex�o com o barramento.
   * @param language A l�ngua utilizada.
   * @param period O per�odo.
   */
  public GreetingsImpl(OpenBusContext context, Language language, Period period) {
    this.context = context;
    this.language = language;
    this.period = period;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String sayGreetings() {
    String caller = context.getCallerChain().caller().entity;
    switch (language) {
      case English:
        return englishGreetings(caller);
      case Spanish:
        return spanishGreetings(caller);
      case Portuguese:
        return portugueseGreetings(caller);
    }
    return "BUG: lingua n�o especificada";
  }

  /**
   * Cumprimenta��es em ingl�s.
   * 
   * @param caller requisitante da chamada.
   * @return o cumprimento.
   */
  private String englishGreetings(String caller) {
    switch (this.period) {
      case Morning:
        return String.format("Good morning %s", caller);
      case Afternoon:
        return String.format("Good afternoon %s", caller);
      case Night:
        return String.format("Good night %s", caller);
    }
    return "BUG: periodo n�o especificado";
  }

  /**
   * Cumprimenta��es em espanhol.
   * 
   * @param caller requisitante da chamada.
   * @return o cumprimento.
   */
  private String spanishGreetings(String caller) {
    switch (this.period) {
      case Morning:
        return String.format("Buenos d�as %s", caller);
      case Afternoon:
        return String.format("Buenas tardes %s", caller);
      case Night:
        return String.format("Buenas noches %s", caller);
    }
    return "BUG: periodo n�o especificado";
  }

  /**
   * Cumprimenta��es em portugu�s.
   * 
   * @param caller requisitante da chamada.
   * @return o cumprimento.
   */
  private String portugueseGreetings(String caller) {
    switch (this.period) {
      case Morning:
        return String.format("Bom dia %s", caller);
      case Afternoon:
        return String.format("Boa tarde %s", caller);
      case Night:
        return String.format("Boa noite %s", caller);
    }
    return "BUG: periodo n�o especificado";
  }
}
