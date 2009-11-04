package tecgraf.openbus.util;

public class Host
{

   public String hostName;
   
   
   public String getHostName() {
	   return hostName;
   }
   
   public int getHostPort() {
	return hostPort;
   }
   
   
   public int hostPort;
   
   /**
    * @param hostName
    * @param hostPort
    */
   public Host(String hostName, int hostPort)
   {
      this.hostName = hostName;
      this.hostPort = hostPort;
   }
}
