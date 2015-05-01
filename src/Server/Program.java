/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author PtitBlond
 */
public abstract class Program
{
    public static void main(String[] args)
    {
        try
        {
            Server s = new Server(
                    args.length >= 1 ? Integer.parseInt(args[0]) : 25000,
                    40000,
                    50000,
                    ""
            );
            s.run();
        }
        catch(SocketException e)
        {
            Logger.getLogger(Program.class.getName()).log(
                Level.SEVERE,
                "Couldn't start server on port .",
                e
            );
        }
    }
}
