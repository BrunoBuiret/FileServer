/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author PtitBlond
 */
public class DownloadableFileFilter implements FileFilter
{
    public boolean accept(File pathname)
    {
        return pathname.isFile();
    }
}
