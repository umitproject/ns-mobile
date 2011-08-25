package org.umit.ns.mobile.api;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.umit.ns.mobile.nsandroid;


public class shellUtils {

    public static String runCommand(String command)
    {
        Process p;
        String output = null;
        try{
            Runtime r = Runtime.getRuntime();
            p = r.exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) 
            {
                output +=inputLine;
            }
            in.close();
            p.destroy();
        }
        catch(IOException e)
        {
            return output;
        }
        return output;
    }
    
    public static boolean killProcess(String path)
    {
        nsandroid.resultPublish("Killing " + path);
        Process p;
        StringBuffer output = new StringBuffer();
        
        //A very dirty method of killing the process
        try{
            p = Runtime.getRuntime().exec("su");
            
            DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
            try {
                pOut.writeBytes("ps | grep " + path + "\nexit\n");
                pOut.flush();
            } 
            catch (IOException e1) {
                e1.printStackTrace();
            }
            
            try {
                p.waitFor();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            
            int read;
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            char[] buffer = new char[1024];
            try{
                while ((read = reader.read(buffer)) > 0) {
                    output.append(buffer, 0, read);
                    nsandroid.resultPublish(output.toString());
                    break;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        
        String pid = "";
        for(int i = 0; i<output.length(); i++)
        {
            //look for the process id
            if(output.charAt(i) > 47 && output.charAt(i) < 58)
            {
                pid = output.substring(i, i + output.substring(i).indexOf(' '));
                break;
            }
        }
        
        try{
            p = Runtime.getRuntime().exec("su");
//            p = Runtime.getRuntime().exec("ps | grep " + path);

            DataOutputStream pOut = new DataOutputStream(p.getOutputStream());
            try {
                pOut.writeBytes("kill -9 " + pid + "\nexit\n");
                pOut.flush();
            } 
            catch (IOException e1) {
                e1.printStackTrace();
            }
            
            try {
                p.waitFor();
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            int read;
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            char[] buffer = new char[1024];
            try{
                while ((read = reader.read(buffer)) > 0) {
                    output.append(buffer, 0, read);
                    nsandroid.resultPublish(output.toString());
                    break;
                    //output = new StringBuffer();
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }
 }
