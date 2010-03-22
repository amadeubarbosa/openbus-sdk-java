package tecgraf.openbus;

import java.util.ArrayList;

import tecgraf.openbus.util.Host;
import tecgraf.openbus.util.PropertiesLoaderImpl;

public class FaultToleranceManager {
	
	/**
	    * A lista de máquinas e portas que contem uma réplica rodando. 
	    */
		private ArrayList<Host> acsHosts;
		
		/**
		   * A máquina que contem a réplica que está sendo usada. 
		   */
		private Host acsHostInUse;
		
		private int currIndex = 0;
		
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
            //Todos os hosts das replicas			
			String[] hostsStr = (PropertiesLoaderImpl.getValor("hosts")).split(",");
			for (int i = 0; i < hostsStr.length; i++) {
				//para cada host de replica
				String[] hostStr = hostsStr[i].split(":");
				String name =  hostStr[0];
				int port = Integer.valueOf(hostStr[1]);
				this.acsHosts.add( new Host(name, port) );	
			}
			
			this.acsHostInUse = this.acsHosts.get(currIndex);
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
			      	//Se a maquina em uso eh a ultima da lista
			      	if(currIndex==this.acsHosts.size()){
			      		// eu pego a primeira
			      		currIndex = 0;
			      	}else{
			      		currIndex += 1;
			      	}
			      	this.acsHostInUse = this.acsHosts.get(currIndex);
			      	return true;
			  }  
			  

}
