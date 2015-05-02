/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author PtitBlond
 */
public abstract class ClientProgram
{
    public static void main(String[] args)
    {
        if(args.length >= 2)
        {
            try
            {
                Client c = new Client(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
                c.run();
            }
            catch(SocketException e)
            {
                Logger.getLogger(ClientProgram.class.getName()).log(Level.SEVERE, null, e);
            }
            catch(UnknownHostException e)
            {
                Logger.getLogger(ClientProgram.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        else
        {
            System.err.println("Error: you need to provide the server address and port.");
        }
    }
}
