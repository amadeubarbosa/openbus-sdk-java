package tecgraf.openbus.infrastructure;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Executa um processo Java.
 * 
 * @author Tecgraf
 */
public class JavaProccess {

  /**
   * Classe que será executada.
   */
  private Class<?> mainClass;

  /**
   * O nome do processo.
   */
  private String name;

  /**
   * O diretório onde a <i>mainClass</i> se encontra.
   */
  private File executionDirectory;

  /**
   * Argumentos que serão passados para o processo.
   */
  private String[] args;

  /**
   * O processo inicializado.
   */
  private Process proccess;

  /**
   * Thread que envia as saídas do <i>process</i> para o processo pai.
   */
  private Pipe inputStream;

  /**
   * Thread que envia as saídas de erro do <i>process</i> para o processo pai.
   */
  private Pipe errStream;

  /**
   * Construtor.
   * 
   * @param mainClass A classe que irá gerar o processo.
   * @param name O nome do processo.
   */
  public JavaProccess(Class<?> mainClass, String name) {
    this.mainClass = mainClass;
    this.args = new String[0];
    this.executionDirectory = new File(".");
    this.name = name;
  }

  /**
   * Construtor.
   * 
   * @param mainClass A classe que irá gerar o processo.
   * @param executionDirectory O diretório onde a classe se encontra.
   */
  public JavaProccess(Class<?> mainClass, File executionDirectory) {
    this.mainClass = mainClass;
    this.args = new String[0];
    this.executionDirectory = executionDirectory;
  }

  /**
   * Define os argumentos que serão passados para o processo.
   * 
   * @param args
   */
  public void setArgs(String... args) {
    this.args = Arrays.copyOf(args, args.length);
  }

  /**
   * Executa a <i>mainClass</i> passando os argumentos definidos.
   * 
   * @throws IOException
   */
  public void exec() throws IOException {
    List<String> commands = new LinkedList<String>();
    commands.add("java");
    commands.add("-cp");
    commands.add(getClassPath());
    commands.add(this.mainClass.getName());
    for (String arg : this.args) {
      commands.add(arg);
    }
    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    processBuilder.directory(this.executionDirectory);
    this.proccess = processBuilder.start();
  }

  /**
   * Fornece o ClassPath do processo atual.
   * 
   * @return o ClassPath do processo atual.
   */
  private String getClassPath() {
    return System.getProperty("java.class.path");
  }

  /**
   * Fornece a saída do processo.
   * 
   * @return Retorna o valor de saída, onde 0 indica que o processo terminou sem
   *         erros. Retorna null caso o processo não exista ou não tenha sido
   *         terminado.
   */
  public Integer getExitCode() {
    try {
      if (this.proccess == null)
        return null;

      return this.proccess.exitValue();
    }
    catch (IllegalThreadStateException e) {
      return null;
    }
  }

  /**
   * Informa se o processo esteja em execução.
   * 
   * @return Retorna <i>true</i> caso o processo esteja em execução ou
   *         <i>false</i> caso contrário.
   */
  public boolean isRunning() {
    return (getExitCode() == null);
  }

  /**
   * Termina o processo abruptamente.
   * 
   * @throws InterruptedException
   */
  public void kill() throws InterruptedException {
    if (this.proccess != null) {
      this.proccess.destroy();
    }
  }

  /**
   * Redireciona a saida padrão para um PrintStream do processo pai.
   * 
   * @param printStream
   */
  public void redirectOut(PrintStream printStream) {
    inputStream =
      new Pipe(this.name, this.proccess.getInputStream(), printStream);
    inputStream.setName("RedirectOut");
    inputStream.start();
  }

  /**
   * Redireciona a saida de erro para um PrintStream do processo pai.
   * 
   * @param printStream
   */
  public void redirectErr(PrintStream printStream) {
    errStream =
      new Pipe(this.name, this.proccess.getErrorStream(), printStream);
    errStream.setName("RedirectErr");
    errStream.start();
  }
}
