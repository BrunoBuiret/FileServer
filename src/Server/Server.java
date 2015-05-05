/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Bruno Buiret
 */
public class Server
{
    protected DatagramSocket socket;
    
    protected int startPort;
    
    protected int endPort;
    
    protected File filesDirectory;
    
    protected Date startDate;
    
    protected int connectionsNumber;
    
    public Server(int serverPort, int startPort, int endPort, String filesDirectory) throws SocketException, FileNotFoundException, RuntimeException
    {
        this.socket = new DatagramSocket(serverPort);
        this.startPort = startPort;
        this.endPort = endPort;
        this.filesDirectory = new File(filesDirectory);
        this.startDate = new Date();
        this.connectionsNumber = 0;
        
        if(!this.filesDirectory.exists())
            throw new FileNotFoundException("Directory \"" + this.filesDirectory.getName() + "\" doesn't exist.");
        else if(!this.filesDirectory.isDirectory())
            throw new RuntimeException("\"" + this.filesDirectory.getName() + "\" isn't a valid directory.");
    }
    
    public void run()
    {
        boolean keepListening = true;
        
        while(keepListening)
        {
            // UDP exchanges vars
            byte[] requestData, responseData;
            DatagramPacket requestPacket, responsePacket;
            String requestString;
            
            try
            {
                // Build the request packet
                requestData = new byte[512];
                requestPacket = new DatagramPacket(requestData, 512);
                
                // Listen for a request
                this.socket.receive(requestPacket);
                
                // Decode request packet
                requestString = new String(requestData, "UTF-8").trim();
                
                // Print the request
                System.out.println(String.format(
                    "%s:%d %s",
                    requestPacket.getAddress(),
                    requestPacket.getPort(),
                    requestString
                ));
                
                // Deal with the request
                if(requestString.equals("USER"))
                {
                    // Find a free port
                    int serverPort;
                    for(serverPort = this.startPort; serverPort <= this.endPort && !this.isPortOpen(serverPort); serverPort++);
                    
                    if(serverPort <= this.endPort)
                    {
                        Connection c = new Connection(this, serverPort, requestPacket.getAddress(), requestPacket.getPort());
                        c.start();
                        this.connectionsNumber++;
                    }
                    else
                    {
                        // Send the client an error response
                        responseData = "ERROR no ports available at the moment.".getBytes("UTF-8");
                        responsePacket = new DatagramPacket(responseData, responseData.length, requestPacket.getAddress(), requestPacket.getPort());
                        this.socket.send(responsePacket);
                    }
                }
                else
                {
                    // Send the client an error response
                    responseData = ("ERROR command " + requestString + " unknown").getBytes("UTF-8");
                    responsePacket = new DatagramPacket(responseData, responseData.length, requestPacket.getAddress(), requestPacket.getPort());
                    this.socket.send(responsePacket);
                }
            }
            catch(IOException e)
            {
                Logger.getLogger(Server.class.getName()).log(
                    Level.SEVERE,
                    "I/O error: " + e.getMessage(),
                    e
                );
            }
        }
        
        // Close the socket
        this.socket.close();
    }
    
    public Date getStartDate()
    {
        return this.startDate;
    }
    
    public File getFilesDirectory()
    {
        return this.filesDirectory;
    }
    
    public int getConnectionsNumber()
    {
        return this.connectionsNumber;
    }
    
    public void decreaseConnectionsNumber()
    {
        this.connectionsNumber--;
    }
    
    protected boolean isPortOpen(int port)
    {
        try
        {
            DatagramSocket s = new DatagramSocket(port);
            s.close();
            return true;
        }
        catch(SocketException e)
        {
            return false;
        }
    }
}
