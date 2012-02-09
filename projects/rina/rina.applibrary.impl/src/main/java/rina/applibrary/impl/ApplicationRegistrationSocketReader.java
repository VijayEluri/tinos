package rina.applibrary.impl;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import rina.delimiting.api.BaseSocketReader;
import rina.delimiting.api.Delimiter;

/**
 * Reads the socket used to register an unregister the application with the 
 * local RINA software stack
 * @author eduardgrasa
 *
 */
public class ApplicationRegistrationSocketReader extends BaseSocketReader{
	
	/**
	 * The queue to send back the M_CREATE_R and M_DELETE_R CDAP messages 
	 * to the ApplicationRegistrationImpl class
	 */
	private BlockingQueue<byte[]> registrationQueue = null;
	
	/**
	 * The applicationRegistrationImplementation object. Will notify it when 
	 * the registration ends (i.e. the registration socket closes)
	 */
	private DefaultApplicationRegistrationImpl appRegImpl = null;
	
	public ApplicationRegistrationSocketReader(Socket socket, Delimiter delimiter, 
			BlockingQueue<byte[]> registrationQueue, DefaultApplicationRegistrationImpl appRegImpl) {
		super(socket, delimiter);
		this.registrationQueue = registrationQueue;
		this.appRegImpl = appRegImpl;
	}

	@Override
	public void processPDU(byte[] pdu) {
		try{
			this.registrationQueue.put(pdu);
		}catch(InterruptedException ex){
			ex.printStackTrace();
			//TODO what to do?
		}
	}

	@Override
	public void socketDisconnected() {
		appRegImpl.registrationSocketClosed();
	}
}
