package com.dudar.utils.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProxyProvider {

    public static String getProxyAddr(){
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "get_proxy.py");
//            ProcessBuilder processBuilder = new ProcessBuilder("python3", "--version");
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));
            String s = null;
            while ((s = stdInput.readLine()) != null)
            {
                process.waitFor();
                return s;
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
