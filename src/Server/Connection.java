/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
        this.dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    }
    
    public void run()
    {
        // Inform the client of the new server port to use
        try
        {
            byte[] connectionData = ("SERVER " + this.socket.getLocalPort()).getBytes("UTF-8");
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
                commandString = requestString.contains(" ") ? requestString.substring(0, requestString.indexOf(" ")) : requestString;
                
                // Print the request
                System.out.println(String.format(
                    "%s:%d %s",
                    requestPacket.getAddress(),
                    requestPacket.getPort(),
                    requestString
                ));
                
                // Deal with the request
                switch(commandString)
                {
                    case "LIST":
                        // Build the response packet
                        StringBuilder filesListBuilder = new StringBuilder("LIST "); 
                        File[] filesList = this.getFilesList();
                        
                        for(File f : filesList)
                            filesListBuilder.append(f.getName()).append(' ');
                        
                        responseData = filesListBuilder.toString().trim().getBytes("UTF-8");
                        responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                        
                        // Then, send it
                        this.socket.send(responsePacket);
                    break;
                        
                    case "DOWNLOAD":
                        File downloadedFile = new File(this.server.getFilesDirectory(), requestString.substring(9));
                        
                        if(downloadedFile.exists())
                        {
                            if(downloadedFile.isFile())
                            {
                                // First, send the client the file size so that they can prepare for the download
                                responseData = ("SIZE " + downloadedFile.length()).getBytes("UTF-8");
                                responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                                this.socket.send(responsePacket);
                                
                                // Then, send the contents of the file
                                try
                                {
                                    BufferedInputStream input = null;
                                    
                                    try
                                    {
                                        input = new BufferedInputStream(new FileInputStream(downloadedFile));
                                        int totalReadBytes = 0;
                                        boolean errorHappened = false;
                                        this.socket.setSoTimeout(10);
                                        
                                        while(totalReadBytes < downloadedFile.length() && !errorHappened)
                                        {
                                            int remainingBytes = (int) downloadedFile.length() - totalReadBytes;
                                            responseData = new byte[remainingBytes >= 1024 ? 1024 : remainingBytes];
                                            int readBytes = input.read(responseData, 0, responseData.length);
                                            
                                            if(readBytes > 0)
                                            {
                                                totalReadBytes += readBytes;
                                                
                                                // Send the file part
                                                responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                                                this.socket.send(responsePacket);
                                                
                                                // Wait for the acknowledgment
                                                requestData = new byte[512];
                                                requestPacket = new DatagramPacket(requestData, 512);
                                                this.socket.receive(requestPacket);
                                                
                                                // Is the sent data's size the same as the read one
                                                requestString = new String(requestData, "UTF-8").trim();
                                                
                                                if(Integer.parseInt(requestString.substring(4)) != readBytes)
                                                    errorHappened = true;
                                            }
                                        }
                                    }
                                    finally
                                    {
                                        input.close();
                                        this.socket.setSoTimeout(0);
                                    }
                                }
                                catch(FileNotFoundException e)
                                {
                                    // Send the client an error response
                                    responseData = ("ERROR file \"" + downloadedFile.getName() + "\" doesn't exist").getBytes("UTF-8");
                                    responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                                    this.socket.send(responsePacket);
                                }
                            }
                            else
                            {
                                // Send the client an error response
                                responseData = ("ERROR \"" + downloadedFile.getName() + "\" isn't a valid file").getBytes("UTF-8");
                                responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                                this.socket.send(responsePacket);
                            }
                        }
                        else
                        {
                            // Send the client an error response
                            responseData = ("ERROR file \"" + downloadedFile.getName() + "\" doesn't exist").getBytes("UTF-8");
                            responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                            this.socket.send(responsePacket);
                        }
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
                            this.getFilesList().length +
                            " " +
                            this.server.getConnectionsNumber()
                        ).getBytes("UTF-8");
                        responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                        
                        // Then, send it
                        this.socket.send(responsePacket);
                    break;
                    
                    case "HELP":
                        // Build the response packet
                        StringBuilder helpBuilder = new StringBuilder("HELP ");
                        
                        if(requestString.equals(commandString))
                        {
                            helpBuilder.append("LIST DOWNLOAD UPLOAD STATISTICS HELP QUIT");
                        }
                        else
                        {
                            String command = requestString.substring(5).toUpperCase();
                            
                            switch(command)
                            {
                                case "LIST":
                                    helpBuilder
                                        .append("LIST")
                                        .append("\n")
                                        .append("Prints a list of every available file on the server.");
                                break;
                                    
                                case "DOWNLOAD":
                                    helpBuilder
                                        .append("DOWNLOAD <file name>")
                                        .append("\n")
                                        .append("Downloads file <file name> from the server.");
                                break;
                                    
                                case "UPLOAD":
                                    helpBuilder
                                        .append("UPLOAD <file name>")
                                        .append("\n")
                                        .append("Uploads file <file name> on the server.");
                                break;
                                    
                                case "STATISTICS":
                                    helpBuilder
                                        .append("STATISTICS")
                                        .append("\n")
                                        .append("Prints statistics about the server.");
                                break;
                                    
                                case "HELP":
                                    helpBuilder
                                        .append("HELP [command]")
                                        .append("\n")
                                        .append("Prints the list of available commands or informations about [command].");
                                break;
                                    
                                case "QUIT":
                                    helpBuilder
                                        .append("QUIT")
                                        .append("\n")
                                        .append("Closes the connection to the server.");
                                break;
                                    
                                default:
                                    helpBuilder
                                        .append("Error: command " + command + " unknown");
                            }
                        }
                        
                        responseData = helpBuilder.toString().getBytes("UTF-8");
                        responsePacket = new DatagramPacket(responseData, responseData.length, this.clientAddress, this.clientPort);
                        
                        // Then, send it
                        this.socket.send(responsePacket);
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
                    "I/O error: " + e.getMessage(),
                    e
                );
            }
        }
        
        // Close the socket
        this.socket.close();
        
        // Decrease connections number
        this.server.decreaseConnectionsNumber();
    }
    
    protected File[] getFilesList()
    {
        return this.server.getFilesDirectory().listFiles(new DownloadableFileFilter());
    }
}
