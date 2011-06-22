package org.umit.ns.mobile.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


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
}
