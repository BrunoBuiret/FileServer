/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author PtitBlond
 */
public class Connection extends Thread
{
    protected Server server;
    
    protected DatagramSocket socket;
    
    protected InetAddress clientAddress;
    
    protected int clientPort;
    
    protected Date startDate;
    
    protected final SimpleDateFormat dateFormat;
    
    public Connection(Server server, int serverPort, InetAddress clientAddress, int clientPort) throws SocketException
    {
        
        this.server = server;
        this.socket = new DatagramSocket(serverPort);
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.startDate = new Date();
        this.dateFormat = new SimpleDateFormat("yyyy/MM/mm HH:mm:ss");
    }
    
    public void run()
    {
        // Inform the client of the new server port to use
        try
        {
            byte[] connectionData = ("SERVER" + this.socket.getLocalPort()).getBytes("UTF-8");
            DatagramPacket connectionPacket = new DatagramPacket(connectionData, connectionData.length, this.clientAddress, this.clientPort);
            this.socket.send(connectionPacket);
        }
        catch(IOException e)
        {
            Logger.getLogger(Connection.class.getName()).log(
                Level.SEVERE,
                "Couldn't establish to client " + this.clientAddress + ":" + this.clientPort + ".",
                e
            );
        }
        
        // Start the connection loop
        boolean keepListening = true;
        
        while(keepListening)
        {
            // UDP exchanges vars
            byte[] requestData, responseData;
            DatagramPacket requestPacket, responsePacket;
            String requestString, commandString;
            
            try
            {
                // Build the request packet
                requestData = new byte[512];
                requestPacket = new DatagramPacket(requestData, 512);
                
                // Listen for a request
                this.socket.receive(requestPacket);
                
                // Decode request packet
                requestString = new String(requestData, "UTF-8").trim();
                commandString = requestString.substring(requestString.indexOf(" "));
                
                switch(commandString)
                {
                    case "LIST":
                        // Build the response packet
                        StringBuilder responseBuilder = new StringBuilder("LIST "); 
                        List<File> filesList = this.getFilesList();
                        
                        for(File f : filesList)
                            responseBuilder.append(f.getName()).append(' ');
                        
                        responseData = responseBuilder.toString().trim().getBytes("UTF-8");
                        responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                        
                        // Then, send it
                        this.socket.send(responsePacket);
                    break;
                        
                    case "DOWNLOAD":
                        
                    break;
                        
                    case "UPLOAD":
                        
                    break;
                        
                    case "STATISTICS":
                        // Build the response packet
                        responseData = (
                            "STATISTICS " +
                            this.dateFormat.format(this.server.getStartDate()) +
                            " " +
                            this.dateFormat.format(this.startDate) +
                            " " +
                            this.getFilesList().size()
                        ).getBytes("UTF-8");
                        responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                        
                        // Then, send it
                        this.socket.send(responsePacket);
                    break;
                    
                    case "HELP":
                        
                    break;
                        
                    case "QUIT":
                        // Stop the listening loop
                        keepListening = false;
                    break;
                        
                    default:
                        // Send the client an error response
                        responseData = ("ERROR command " + commandString + " unknown").getBytes("UTF-8");
                        responsePacket = new DatagramPacket(responseData, responseData.length, requestPacket.getAddress(), requestPacket.getPort());
                        this.socket.send(responsePacket);
                }
            }
            catch(IOException e)
            {
                Logger.getLogger(Connection.class.getName()).log(
                    Level.SEVERE,
                    null,
                    e
                );
            }
        }
        
        // Close the socket
        this.socket.close();
    }
    
    protected List<File> getFilesList()
    {
        return new ArrayList<File>();
    }
}
