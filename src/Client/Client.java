/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author PtitBlond
 */
public class Client
{
    protected DatagramSocket socket;
    
    protected InetAddress serverAddress;
    
    protected int serverPort;
    
    protected Scanner scanner;
    
    public Client(InetAddress serverAddress, int serverPort) throws SocketException
    {
        this.socket = new DatagramSocket();
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.scanner = new Scanner(System.in);
    }
    
    public void run()
    {
        try
        {
            // UDP exchanges vars
            byte[] requestData, responseData;
            DatagramPacket requestPacket, responsePacket;
            String requestString, responseString, commandString;
            
            // First, request a port on the server
            requestData = "USER".getBytes("UTF-8");
            requestPacket = new DatagramPacket(requestData, requestData.length, this.serverAddress, this.serverPort);
            this.socket.send(requestPacket);
            
            // Wait for a response
            responseData = new byte[512];
            responsePacket = new DatagramPacket(responseData, 512);
            this.socket.receive(responsePacket);
            
            // Then, decode it
            responseString = new String(responseData, "UTF-8").trim();
            
            if(responseString.startsWith("SERVER"))
            {
                // A port was attributed to the client, memorize it
                this.serverPort = Integer.parseInt(responseString.substring(7));
            }
            else if(responseString.startsWith("ERROR"))
            {
                System.err.println("Error: " + responseString.substring(6));
                return;
            }
            else
            {
                System.err.println("Error: unknown response (" + responseString + ").");
            }
            
            // Main client loop
            boolean keepListening = true;
        
            while(keepListening)
            {
                // Prompt the user to know what they want to do
                requestString = this.prompt();
                commandString = requestString.contains(" ") ? requestString.substring(requestString.indexOf(" ")) : requestString;
                
                switch(commandString)
                {
                    case "LIST":
                    case "STATISTICS":
                    case "HELP":
                    case "QUIT":
                        // Build the request packet
                        requestData = requestString.getBytes("UTF-8");
                        requestPacket = new DatagramPacket(requestData, requestData.length, this.serverAddress, this.serverPort);
                        
                        // Then, send it
                        this.socket.send(requestPacket);
                    break;
                    
                    case "DOWNLOAD":
                        System.out.println("Info: not implemented yet.");
                    break;
                        
                    case "UPLOAD":
                        System.out.println("Info: not implemented yet.");
                    break;
                        
                    default:
                        System.err.println("Error: command " + commandString + " unknown.");
                }
                
                switch(commandString)
                {
                    case "LIST":
                        // Wait for a response
                        responseData = new byte[1024];
                        responsePacket = new DatagramPacket(responseData, 1024);
                        this.socket.receive(responsePacket);
                        
                        // Then, decode it
                        responseString = new String(responseData, "UTF-8").trim();
                        
                        // Build the list of files
                        String[] filesList = responseString.contains(" ") ? responseString.substring(5).split(" ") : new String[0];
                        StringBuilder filesBuilder = new StringBuilder();
                        
                        for(String file : filesList)
                            filesBuilder.append(file).append(" ");
                        
                        System.out.println("There " + (filesList.length != 1 ? "are" : "is") + " " + filesList.length + " file" + (filesList.length != 1 ? "s" : "") + " available.");
                        System.out.println(filesBuilder.toString());
                    break;
                        
                    case "STATISTICS":
                        // Wait for a response
                        responseData = new byte[1024];
                        responsePacket = new DatagramPacket(responseData, 1024);
                        this.socket.receive(responsePacket);
                        
                        // Then, decode it
                        responseString = new String(responseData, "UTF-8").trim();
                        
                        System.out.println(responseString);
                    break;
                        
                    case "HELP":
                        
                    break;
                        
                    case "DOWNLOAD":
                        
                    break;
                        
                    case "UPLOAD":
                        
                    break;
                        
                    case "QUIT":
                        // Stop looping
                        keepListening = false;
                    break;
                        
                    default:
                }
            }
        }
        catch(IOException e)
        {
            Logger.getLogger(Client.class.getName()).log(
                Level.SEVERE,
                null,
                e
            );
        }
        
        // Close the socket
        this.socket.close();
    }
    
    protected String prompt()
    {
        System.out.print("> ");
        return this.scanner.nextLine();
    }
}
