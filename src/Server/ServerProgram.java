/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author PtitBlond
 */
public abstract class ServerProgram
{
    public static void main(String[] args)
    {
        if(args.length >= 4)
        {
            try
            {
                Server s = new Server(
                    Integer.parseInt(args[0]),
                    Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]),
                    args[3]
                );
                s.run();
            }
            catch(SocketException e)
            {
                Logger.getLogger(ServerProgram.class.getName()).log(
                    Level.SEVERE,
                    "Couldn't start server on port " + args[0] + " : " + e.getMessage() +".",
                    e
                );
            }
            catch(FileNotFoundException|RuntimeException e)
            {
                Logger.getLogger(ServerProgram.class.getName()).log(
                    Level.SEVERE,
                    e.getMessage(),
                    e
                );
            }
        }
        else
        {
            System.err.println("Error: you must provide server port, ports range and files directory.");
        }
    }
}
