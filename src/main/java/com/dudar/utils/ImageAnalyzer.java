package com.dudar.utils;

import org.apache.http.client.methods.HttpGet;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class ImageAnalyzer {

    public static void main (String[] args) throws IOException {
        String asd = Utilities.getImaggaApiKey();

        String asd1 = Utilities.getImaggaApiSecret();

        //TODO
        //Store image from web to local storage

        String localImageFilepath = "";
        String resourceId = uploadResource(localImageFilepath);
        List<String> similarTags = getTags(resourceId);
        deleteResource(resourceId);
        deleteResource(resourceId);
    }

    private static String getBasicAuth(){
        String credentialsToEncode = Utilities.getImaggaApiKey() + ":" + Utilities.getImaggaApiSecret();
        return Base64.getEncoder().encodeToString(credentialsToEncode.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> getTags(String image_url) throws IOException{
        String credentialsToEncode = "acc_6c26e59ce122b7f" + ":" + "5718ebc64fc59ce90adcfa0c61016e3c";
        String basicAuth = getBasicAuth();

        String endpoint_url = "https://api.imagga.com/v2/tags";
//        String image_url = "https://imagga.com/static/images/tagging/wind-farm-538576_640.jpg";

        String url = endpoint_url + "?image_upload_id=" + image_url;
        URL urlObject = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();

        connection.setRequestProperty("Authorization", "Basic " + basicAuth);

        int responseCode = connection.getResponseCode();

        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String jsonResponse = connectionInput.readLine();

        connectionInput.close();

        System.out.println(jsonResponse);

        //TODO return tags from json
        return null;
    }

    private static String uploadResource(String imageFilepath) throws IOException {
        String basicAuth = getBasicAuth();

        // Change the file path here
        String filepath = "/Users/admin/Downloads/81183694_2468355153491546_7101281018937838700_n.jpg";
        File fileToUpload = new File(filepath);

        String endpoint = "/uploads";

        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary =  "Image Upload";

        URL urlObject = new URL("https://api.imagga.com/v2" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        connection.setUseCaches(false);
        connection.setDoOutput(true);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty(
                "Content-Type", "multipart/form-data;boundary=" + boundary);

        DataOutputStream request = new DataOutputStream(connection.getOutputStream());

        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + fileToUpload.getName() + "\"" + crlf);
        request.writeBytes(crlf);


        InputStream inputStream = new FileInputStream(fileToUpload);
        int bytesRead;
        byte[] dataBuffer = new byte[1024];
        while ((bytesRead = inputStream.read(dataBuffer)) != -1) {
            request.write(dataBuffer, 0, bytesRead);
        }

        request.writeBytes(crlf);
        request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
        request.flush();
        request.close();

        InputStream responseStream = new BufferedInputStream(connection.getInputStream());

        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

        String line = "";
        StringBuilder stringBuilder = new StringBuilder();

        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();

        String response = stringBuilder.toString();
        System.out.println(response);

        responseStream.close();
        connection.disconnect();

        //TODO parse json and return resource identifier
        return "";
    }

    private static void deleteResource(String resourceId) throws IOException {
        String basicAuth = getBasicAuth();

        String upload_id = resourceId;
        String url = "https://api.imagga.com/v2/uploads/" + upload_id;

        URL urlObject = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();

        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        connection.setRequestMethod("DELETE");

        int responseCode = connection.getResponseCode();

        System.out.println("\nSending 'DELETE' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String jsonResponse = connectionInput.readLine();

        connectionInput.close();

        System.out.println(jsonResponse);
    }

}
