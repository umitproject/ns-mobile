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
 * Helper class to extract Zipfile
 */

package org.umit.ns.mobile.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.umit.ns.mobile.nsandroid;

public class ZipUtils {

    public boolean unzipArchive(File archive, File outputDir) {
        try {
            ZipFile zipfile = new ZipFile(archive);
            for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                unzipEntry(zipfile, entry, outputDir);
            }
        } catch (Exception e) {
            nsandroid.resultPublish("Error while extracting file " + archive + " " +  e.getMessage());
            return false;
        }
        return true;
    }

    private void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {

        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }

        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()){
            createDir(outputFile.getParentFile());
        }

        BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

        try {
            copy(inputStream, outputStream);
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }
    
    public void copy(InputStream in, OutputStream out) throws IOException {
        if (in == null)
            throw new NullPointerException("InputStream is null!");
        if (out == null)
            throw new NullPointerException("OutputStream is null");

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    
    private void createDir(File dir) {
        try {
            Process process = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("mkdir " + dir + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

        } catch (IOException e) {
            nsandroid.resultPublish(e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}