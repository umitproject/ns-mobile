/*
Android Network Scanner Umit Project
Copyright (C) 2011 Adriano Monteiro Marques

Author: Angad Singh <angad@angad.sg>

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 */

/**
 * @author angadsg
 * 
 * Exports Database to XML file (similar to nmaps' implementation)
 * 
 * Loads data from XML file.
 * 
 */

package org.umit.ns.mobile.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileManager {

    static String fileName = "/data/local/nsandroid-logs.txt";
    public static void write(String tag, String str)
    {
        FileWriter fstream;
        try {
            fstream = new FileWriter(fileName);
            BufferedWriter out = new BufferedWriter(fstream);
            out.append(tag + ": " + str);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}