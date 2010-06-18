package tecgraf.openbus.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Thread respons�vel por enviar o <i>InputStream</i> para um <i>PrinStream</i>
 * 
 * @author Tecgraf
 */
public class Pipe extends Thread {
  /**
   * O identificador do Pipe.
   */
  private String label;
  /**
   * O produtor da informa��o
   */
  private InputStream inputStream;

  /**
   * O consumidor da informa��o
   */
  private PrintStream printStream;

  /**
   * Construtor
   * 
   * @param label
   * @param inputStream
   * @param printStream
   */
  public Pipe(String label, InputStream inputStream, PrintStream printStream) {
    this.label = label;
    this.inputStream = inputStream;
    this.printStream = printStream;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    try {
      InputStreamReader isr = new InputStreamReader(inputStream);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      while ((line = br.readLine()) != null) {
        printStream.println(String.format("[%s] %s", label, line));
      }
    }
    catch (IOException e) {
      /*
       * Necess�rio porque o Proccess.destroy() retorna uma exce��o
       * (IOException) caso o ShutdownHook tente imprimir a sa�da no console.
       * Quando estivermos direcionando o log para um arquivo, n�o precisaremos
       * chamar explicitamente o Thread.interrupt. Consequentemente, n�o
       * recebemos o log do disconnect() e do destroy()
       */
    }
  }
}
