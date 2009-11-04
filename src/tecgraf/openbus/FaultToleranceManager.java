package tecgraf.openbus;

import java.util.ArrayList;

import tecgraf.openbus.util.Host;
import tecgraf.openbus.util.PropertiesLoaderImpl;

public class FaultToleranceManager {
	
	/**
	    * A lista de máquinas e portas que contem uma réplica rodando. 
	    */
		private ArrayList<Host> acsHosts;
		private ArrayList<Host> rsHosts;
		
		private static int NBR_SERVERS = Integer.valueOf(PropertiesLoaderImpl.getValor("numServers"));
		
		/**
		   * A máquina que contem a réplica que está sendo usada. 
		   */
		private Host acsHostInUse;
		
		private final String acsRef = "acs";
		
		private static FaultToleranceManager ftManager;
		
		private FaultToleranceManager() {
			this.acsHosts = new ArrayList<Host>();
			  setHosts();
		}
		
		public static FaultToleranceManager getInstance(){
			if (ftManager == null)
				ftManager = new FaultToleranceManager();
			return ftManager;
		}
		
		
		/**
		   * Popula a lista de hosts que contem as réplicas do Serviço Tolerante a Falhas.
		   * 
		   */
		  private void setHosts() {

			if(this.acsHosts==null)
				this.acsHosts = new ArrayList<Host>();
			
			for (int i = 1; i <= NBR_SERVERS; i++) {
				String[] hostStr = (PropertiesLoaderImpl.getValor(acsRef + "HostAdd-" + i)).split(":");
				String name =  hostStr[0];
				int port = Integer.valueOf(hostStr[1]);
				this.acsHosts.add( new Host(name, port) );
			}
			
			this.acsHostInUse = this.acsHosts.get(0);
		}
		  
		  public ArrayList<Host> getHosts() {
				return acsHosts;
			}


			public void setHosts(ArrayList<Host> hosts) {
				this.acsHosts = hosts;
			}


			public Host getACSHostInUse() {
				return acsHostInUse;
			}


			public void setACSHostInUse(Host hostInUse) {
				this.acsHostInUse = hostInUse;
			}
			
			
			/**
			   * No caso de uma falha de réplica, este método deve ser chamado para atualizar a máquina a ser 
			   * obtida uma réplica.
			   */
			  public boolean updateACSHostInUse(){
			  		int indexCurr = this.acsHosts.indexOf(this.acsHostInUse);
			      	//Se a maquina em uso eh a ultima da lista
			      	if(indexCurr==this.acsHosts.size()-1){
			      		// eu pego a primeira
			      		//this.acsHostInUse = this.hosts.get(0);
			      		return false;
			      	}else{
			      		for (Host host : this.acsHosts) {
			      			if(indexCurr< this.acsHosts.indexOf(host)){
			      				//se eu estou na proxima maquina da list
			      				this.acsHostInUse = host;
			      				return true;
			      			}
			      		}
			      	}
			      	return false;
			  }  
			  

}
