package tecgraf.openbus.util;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;

public class TestLogger extends Thread{
  
  static TestLogger instance = null;
    
  public void run() {
    try {
      while (!isInterrupted()){
      sleep(2000);
      logFile.flush();
      }
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  
  protected boolean exit=false;
  protected LinkedList<String> messages = new LinkedList<String>();
  
  private FileWriter logFile;
  SimpleDateFormat fmt = new SimpleDateFormat("dd/MM; k:m:s");
  
  public static TestLogger getInstance() {
    return instance;
  }
  
  public TestLogger(String token){
    String logFileName = token+".log";
    try {
      logFile = new FileWriter(logFileName, true);
      //this.start();
      instance = this;
    }
    catch (Exception e) {
      System.out.println("WARNNING: unable to create Log file."+e.getMessage());
      e.printStackTrace();
    }
  }
  
  public void write(String event, String msg){
    try {
      messages.add(String.format("[%s] %s ; %s\n", event, fmt.format(new Date()),msg));
      //logFile.write(String.format("[%s] %s %s\n", event, fmt.format(new Date()),msg));
      //String str = String.format("[%s] %s; %s\n", event, fmt.format(new Date()),msg);
       /* if (messages.size() > 18) {
        synchronized (messages) {
          messages.notify();
        }
        }*/
    }
    catch (Exception e) {
      System.out.println("WARNNING: unable to write to interceptor Log file."+e.getMessage());
      e.printStackTrace();
    }
  }
  
  public void close() { 
      System.out.println("22-"+messages.size());
      while (!messages.isEmpty()) {
        try {  
          String msg = messages.removeFirst();
          logFile.write(msg);
        }
        catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          break;
        }
      }
      try {
        logFile.close();
      }
      catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
  }
  
  public void flush(){
    try {
      while (!messages.isEmpty())
        logFile.write(messages.removeFirst());
    logFile.flush();
    }
    catch(Exception e) {}
  }

   public static void main(String[] args) throws InterruptedException {
     final TestLogger log = new TestLogger("teste");;
     Runtime.getRuntime().addShutdownHook(
       new Thread() {
         public void run(){
           System.out.println("Flushing logfile");
           log.close();
         }
       });
     long x = System.nanoTime();
     for (int i=0; i < 21;i++) log.write("SendRequest", "bla bla bla bla bla");

     for (int i=0; i < 20; i++) log.write("SendRequest", "88888888888888888888888");
     System.out.println(System.nanoTime() - x);
     System.out.println("finished");
     System.exit(0);
   }
   

}
