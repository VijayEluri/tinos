package rina.cdap.echotarget;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rina.cdap.api.CDAPException;
import rina.cdap.api.CDAPSessionManager;
import rina.cdap.api.message.CDAPMessage;
import rina.cdap.api.message.CDAPMessage.AuthTypes;
import rina.delimiting.api.Delimiter;
import rina.delimiting.api.DelimiterFactory;
import rina.encoding.api.Encoder;
import rina.encoding.api.EncoderFactory;

/**
 * Client of the CDAP Echo Server
 * @author eduardgrasa
 *
 */
public abstract class CDAPClient {
	
	private static final Log log = LogFactory.getLog(CDAPClient.class);
	
	/**
	 * The cdap session manager
	 */
	protected CDAPSessionManager cdapSessionManager = null;
	
	/**
	 * The delimiter for the sessions
	 */
	protected Delimiter delimiter = null;
	
	/**
	 * The encoder (to marshall/unmarshall the payload of CDAP messages)
	 */
	protected Encoder encoder = null;
	
	/**
	 * The TCP port where the CDAP Echo server is listening
	 */
	private int port = 0;
	
	/**
	 * The host where the CDAP Echo Server is running
	 */
	private String host = null;
	
	/**
	 * The socket to connect to the server
	 */
	protected Socket clientSocket = null;
	
	/**
	 * Tells when to stop listening the socket 
	 * for incoming bytes
	 */
	protected boolean end = false;
	
	public CDAPClient(CDAPSessionManager cdapSessionManager, DelimiterFactory delimiterFactory, 
			EncoderFactory encoderFactory, String host, int port){
		this.cdapSessionManager = cdapSessionManager;
		this.delimiter = delimiterFactory.createDelimiter(DelimiterFactory.DIF);
		if (encoderFactory != null){
			encoder = encoderFactory.createEncoderInstance();
		}
		this.host = host;
		this.port = port;
	}
	
	public void run(){
		try {
			clientSocket = new Socket(host, port);
			
			//1 Create an M_CONNECT message, delimit it and send it to the CDAP Echo Target
			CDAPMessage message = cdapSessionManager.getOpenConnectionRequestMessage(clientSocket.getLocalPort(), AuthTypes.AUTH_NONE, null, null, 
					"default_enrollment", "1","i2CAT-Barcelona", null, null , "1", "TSSG-Waterford");
			sendCDAPMessage(message);
			
			//2 Enter the loop to wait for response messages, and continue the message exchange while possible
			byte nextByte = 0;
			boolean lookingForSduLength = true;
			byte[] lastSduLengthCandidate = new byte[0];
			byte[] currentSduLengthCandidate = null;
			byte[] serializedCDAPMessage = null;
			int length = 0;
			int index = 0;
			while(!end){
				try{
					nextByte = (byte) clientSocket.getInputStream().read();
					if (lookingForSduLength){
						currentSduLengthCandidate = new byte[lastSduLengthCandidate.length + 1];
						for(int i=0; i<lastSduLengthCandidate.length; i++){
							currentSduLengthCandidate[i] = lastSduLengthCandidate[i];
						}
						currentSduLengthCandidate[lastSduLengthCandidate.length] = nextByte;
						length = delimiter.readVarint32(currentSduLengthCandidate);
						if (length == -2){
							lastSduLengthCandidate = currentSduLengthCandidate;
						}else{
							lastSduLengthCandidate = new byte[0];
							if (length > 0){
								log.info("Found a delimited CDAP message, of length " + length);
								lookingForSduLength = false;
							}
						}
					}else{
						if (index < length){
							if (serializedCDAPMessage == null){
								serializedCDAPMessage = new byte[length];
							}
							serializedCDAPMessage[index] = nextByte;
							index ++;
							if (index == length){
								processCDAPMessage(serializedCDAPMessage);
								index = 0;
								length = 0;
								lookingForSduLength = true;
								serializedCDAPMessage = null;
							}
						}
					}
				}catch(IOException ex){
					ex.printStackTrace();
				}
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CDAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected String printBytes(byte[] message){
		String result = "";
		for(int i=0; i<message.length; i++){
			result = result + String.format("%02X", message[i]) + " ";
		}
		
		return result;
	}
	
	protected synchronized void sendCDAPMessage(CDAPMessage cdapMessage) throws CDAPException, IOException{
		byte[] serializedCDAPMessageToBeSend = null;
		byte[] delimitedSdu = null;
		
		serializedCDAPMessageToBeSend = cdapSessionManager.encodeNextMessageToBeSent(cdapMessage, clientSocket.getLocalPort());
		delimitedSdu = delimiter.getDelimitedSdu(serializedCDAPMessageToBeSend);
		clientSocket.getOutputStream().write(delimitedSdu);
		cdapSessionManager.messageSent(cdapMessage, clientSocket.getLocalPort());
		log.info("Sent CDAP Message: "+ cdapMessage.toString());
		log.info("Sent SDU:" + printBytes(delimitedSdu));
	}

	protected abstract void processCDAPMessage(byte[] serializedCDAPMessage);
}
