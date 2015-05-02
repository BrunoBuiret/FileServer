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
        try
        {
            Server s = new Server(
                    args.length >= 1 ? Integer.parseInt(args[0]) : 25000,
                    40000,
                    41000,
                    "D:\\Téléchargements"
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
}
