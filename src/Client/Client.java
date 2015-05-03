/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
                commandString = (requestString.contains(" ") ? requestString.substring(0, requestString.indexOf(" ")) : requestString).toUpperCase();
                
                // Request
                switch(commandString)
                {
                    case "LIST":
                    case "STATISTICS":
                    case "HELP":
                    case "QUIT":
                        // Build the request packet
                        requestData = requestString.toUpperCase().getBytes("UTF-8");
                        requestPacket = new DatagramPacket(requestData, requestData.length, this.serverAddress, this.serverPort);
                        
                        // Then, send it
                        this.socket.send(requestPacket);
                    break;
                        
                    case "DOWNLOAD":
                        // Build the request packet
                        requestData = (commandString + " " + requestString.substring(9)).getBytes("UTF-8");
                        requestPacket = new DatagramPacket(requestData, requestData.length, this.serverAddress, this.serverPort);
                        
                        // Then, send it
                        this.socket.send(requestPacket);
                    break;
                    
                    case "UPLOAD":
                        File uploadingFile = new File(requestString.substring(7));
                        
                        if(uploadingFile.exists())
                        {
                            if(uploadingFile.isFile() && uploadingFile.canRead())
                            {
                                // Build the request packet
                                requestData = (commandString + " " + uploadingFile.length() + " " + uploadingFile.getName()).getBytes("UTF-8");
                                requestPacket = new DatagramPacket(requestData, requestData.length, this.serverAddress, this.serverPort);
                                
                                // Then, send it
                                this.socket.send(requestPacket);
                            }
                            else
                            {
                                System.err.println("Error: \"" + uploadingFile.getName() + "\" isn't a valid file.");
                                commandString = "";
                            }
                        }
                        else
                        {
                            System.err.println("Error: \"" + uploadingFile.getName() + "\" doesn't exist.");
                            commandString = "";
                        }
                    break;
                        
                    default:
                        System.err.println("Error: command " + commandString + " unknown.");
                }
                
                // Response
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
                        
                        // Then print it
                        System.out.println("There " + (filesList.length != 1 ? "are" : "is") + " " + filesList.length + " file" + (filesList.length != 1 ? "s" : "") + " available.");
                        System.out.println(filesBuilder.toString());
                    break;
                    
                    case "DOWNLOAD":
                        // Wait for the file size
                        responseData = new byte[512];
                        responsePacket = new DatagramPacket(responseData, 512);
                        this.socket.receive(responsePacket);
                        
                        // Then, decode it
                        responseString = new String(responseData, "UTF-8").trim();
                        
                        if(responseString.startsWith("SIZE"))
                        {
                            // Extract the file's size
                            int totalBytes = Integer.parseInt(responseString.substring(5));
                            
                            // Prepare for the download
                            File finalFile = new File(requestString.substring(9));
                            File tempFile = new File(requestString.substring(9) + ".part");
                            BufferedOutputStream output = null;
                            boolean errorHappened = false;
                            
                            try
                            {
                                output = new BufferedOutputStream(new FileOutputStream(tempFile));
                                int totalReadBytes = 0;
                                
                                while(totalReadBytes < totalBytes)
                                {
                                    int remainingBytes = totalBytes - totalReadBytes;
                                    
                                    // Wait for a file part
                                    responseData = new byte[remainingBytes >= 1024 ? 1024 : remainingBytes];
                                    responsePacket = new DatagramPacket(responseData, responseData.length);
                                    this.socket.receive(responsePacket);
                                    
                                    // Memorize the size
                                    totalReadBytes += responseData.length;
                                    
                                    // Then, put it in the temporary file
                                    output.write(responseData);
                                    
                                    // Acknowledge the data
                                    requestData = ("ACK " + responseData.length).getBytes("UTF-8");
                                    requestPacket = new DatagramPacket(requestData, requestData.length, this.serverAddress, this.serverPort);
                                    this.socket.send(requestPacket);
                                }
                            }
                            catch(IOException e)
                            {
                                errorHappened = true;
                                
                                // Inform the user
                                System.err.println("Error: " + e.getMessage());
                            }
                            finally
                            {
                                output.close();
                            }
                            
                            if(!errorHappened)
                            {
                                // If no error happened, rename the temporary file
                                tempFile.renameTo(finalFile);
                                
                                // And inform the user
                                System.out.println("\"" + finalFile.getName() +"\" downloaded successfully.");
                            }
                            {
                                tempFile.delete();
                            }
                            
                        }
                        else if(responseString.startsWith("ERROR"))
                        {
                            System.err.println("Error: " + responseString.substring(6));
                        }
                    break;
                        
                    case "UPLOAD":
                        // Wait for the server to be ready
                        responseData = new byte[512];
                        responsePacket = new DatagramPacket(responseData, 512);
                        this.socket.receive(responsePacket);
                        
                        // Then, decode it
                        responseString = new String(responseData, "UTF-8").trim();
                        
                        if(responseString.startsWith("READY"))
                        {
                            File uploadingFile = new File(requestString.substring(7));
                            
                            try
                            {
                                BufferedInputStream input = null;
                                
                                try
                                {
                                    input = new BufferedInputStream(new FileInputStream(uploadingFile));
                                    int totalReadBytes = 0;
                                    boolean errorHappened = false;
                                    this.socket.setSoTimeout(10);

                                    while(totalReadBytes < uploadingFile.length() && !errorHappened)
                                    {
                                        int remainingBytes = (int) uploadingFile.length() - totalReadBytes;
                                        requestData = new byte[remainingBytes >= 1024 ? 1024 : remainingBytes];
                                        int readBytes = input.read(requestData, 0, requestData.length);

                                        if(readBytes > 0)
                                        {
                                            totalReadBytes += readBytes;

                                            // Send the file part
                                            requestPacket = new DatagramPacket(requestData, requestData.length, this.serverAddress, this.serverPort);
                                            this.socket.send(requestPacket);

                                            // Wait for the acknowledgment
                                            responseData = new byte[512];
                                            responsePacket = new DatagramPacket(responseData, responseData.length);
                                            this.socket.receive(responsePacket);

                                            // Is the sent data's size the same as the read one ?
                                            responseString = new String(responseData, "UTF-8").trim();

                                            if(responseString.startsWith("ACK") && Integer.parseInt(responseString.substring(4)) != readBytes)
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
                                System.err.println("Error: \"" + uploadingFile.getName() + "\" doesn't exist anymore.");
                            }
                        }
                    break;
                        
                    case "STATISTICS":
                        // Wait for a response
                        responseData = new byte[512];
                        responsePacket = new DatagramPacket(responseData, 512);
                        this.socket.receive(responsePacket);
                        
                        // Then, decode it and extract informations
                        responseString = new String(responseData, "UTF-8").trim();
                        
                        String serverUptime = responseString.substring(11, 30);
                        String connectionUptime = responseString.substring(31, 50);
                        String[] statisticsNumbers = responseString.substring(51).split(" ");
                        int filesNumber = Integer.parseInt(statisticsNumbers[0]);
                        int usersNumber = Integer.parseInt(statisticsNumbers[1]);
                        
                        // Print the statistics
                        System.out.println(
                            "Server has been running since " + serverUptime + ", you have been connected since " + connectionUptime + "."
                        );
                        System.out.println(
                            "There " + (filesNumber != 1 ? "are" : "is") + " " + filesNumber + " " + (filesNumber != 1 ? "files" : "file") +
                            " available and " + usersNumber + " " + (usersNumber > 1 ? "users" : "user") + " " + (usersNumber > 1 ? "are" : "is") + " connected."
                        );
                    break;
                        
                    case "HELP":
                        // Wait for a response
                        responseData = new byte[512];
                        responsePacket = new DatagramPacket(responseData, 512);
                        this.socket.receive(responsePacket);
                        
                        // Then, decode it
                        responseString = new String(responseData, "UTF-8").trim();
                        
                        // Print the help
                        System.out.println(responseString.substring(5));
                    break;
                        
                    case "QUIT":
                        // Stop looping
                        keepListening = false;
                    break;
                }
            }
        }
        catch(IOException e)
        {
            Logger.getLogger(Client.class.getName()).log(
                Level.SEVERE,
                "I/O error: " + e.getMessage(),
                e
            );
        }
        
        // Close the socket
        this.socket.close();
    }
    
    protected String prompt()
    {
        String command;
        
        do
        {
            System.out.print("> ");
            command = this.scanner.nextLine().trim();
        }
        while(command.length() == 0);
        
        return command;
    }
}
