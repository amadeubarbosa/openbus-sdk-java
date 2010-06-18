package tecgraf.openbus.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Thread responsável por enviar o <i>InputStream</i> para um <i>PrinStream</i>
 * 
 * @author Tecgraf
 */
public class Pipe extends Thread {
  /**
   * O identificador do Pipe.
   */
  private String label;
  /**
   * O produtor da informação
   */
  private InputStream inputStream;

  /**
   * O consumidor da informação
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
       * Necessário porque o Proccess.destroy() retorna uma exceção
       * (IOException) caso o ShutdownHook tente imprimir a saída no console.
       * Quando estivermos direcionando o log para um arquivo, não precisaremos
       * chamar explicitamente o Thread.interrupt. Consequentemente, não
       * recebemos o log do disconnect() e do destroy()
       */
    }
  }
}
