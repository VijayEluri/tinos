package rina.flowallocator.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rina.cdap.api.BaseCDAPSessionManager;
import rina.cdap.api.CDAPException;
import rina.cdap.api.CDAPSessionManager;
import rina.enrollment.api.Neighbor;
import rina.ipcprocess.api.IPCProcess;
import rina.applicationprocess.api.ApplicationProcessNamingInfo;
import rina.ipcservice.api.IPCException;

/**
 * Different usefull functions
 * @author eduardgrasa
 *
 */
public class Utils {
	
	private static final Log log = LogFactory.getLog(Utils.class);
	
	/**
	 * Maps the destination address to the port id of the N-1 flow that this IPC process can use
	 * to reach the IPC process identified by the address
	 * @return
	 */
	public static synchronized int mapAddressToPortId(long address, IPCProcess ipcProcess) throws IPCException{
		CDAPSessionManager cdapSessionManager = (CDAPSessionManager) ipcProcess.getIPCProcessComponent(BaseCDAPSessionManager.getComponentName());
		List<Neighbor> neighbors = null;
		Neighbor neighbor = null;
		int portId = 0;
		
		try{
			neighbors = ipcProcess.getNeighbors();
			
			for(int i=0; i<neighbors.size(); i++){
				neighbor = neighbors.get(i);
				if (neighbor.getAddress() == address){
					portId = cdapSessionManager.getPortId(neighbor.getApplicationProcessName());
					break;
				}
			}
			
			if (portId == 0){
				String message = "Could not find the application process name of the IPC process whose address is "+address;
				log.error(message);
				throw new IPCException(5, message);
			}
		}catch(CDAPException ex){
			log.error(ex);
			throw new IPCException(5, ex.getMessage());
		}
		
		return portId;
	}
	
	/**
	 * Returns the application process name and application process instance of a remote IPC process, given its address.
	 * @param ipcProcessAddress
	 * @param ipcProcess
	 * @return the application process naming information
	 * @throws IPCException if no matching entry is found
	 */
	public static synchronized ApplicationProcessNamingInfo getRemoteIPCProcessNamingInfo(long ipcProcessAddress, IPCProcess ipcProcess) throws IPCException{
		List<Neighbor> neighbors = null;
		Neighbor neighbor = null;
		ApplicationProcessNamingInfo result = null;

		neighbors = ipcProcess.getNeighbors();

		for(int i=0; i<neighbors.size(); i++){
			neighbor = neighbors.get(i);
			if (neighbor.getAddress() == ipcProcessAddress){
				result = new ApplicationProcessNamingInfo(neighbor.getApplicationProcessName(), neighbor.getApplicationProcessInstance());
				return result;
			}
		}

		String message = "Could not find the application process name of the IPC process whose synonym is "+ipcProcessAddress;
		log.error(message);
		throw new IPCException(5, message);
	}

}