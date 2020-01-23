package com.dudar.utils;

import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ImageAnalyzer {

    final static Logger logger = Logger.getLogger(ImageAnalyzer.class);

    public static String imageType(String filePath) throws IOException {

        String resourceId = uploadResource(filePath);
        if(!Strings.isNullOrEmpty(resourceId))
        {
            List<String> similarTags = getTagsForImage(resourceId);
            if(similarTags != null)
            {
                logger.info("Image details (first 3 tags and confidence): " + similarTags);
            }
            deleteResource(resourceId);
        }

        //TODO return correct value
        return "UNDEFINED";
    }

    private static List<String> getTagsForImage(String image_url) throws IOException{
        try{
            String endpoint_url = "https://api.imagga.com/v2/tags";

            String url = endpoint_url + "?image_upload_id=" + image_url;
            URL urlObject = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();

            connection.setRequestProperty("Authorization", "Basic " + getAuthString());

            BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String jsonResponse = connectionInput.readLine();

            connectionInput.close();

            JSONObject obj = new JSONObject(jsonResponse);

            JSONArray arr = obj.getJSONObject("result").getJSONArray("tags");
            List<String> resultTags =new ArrayList<>();
            //Limit to only top 3 tags for image
            for (int i = 0; i < ((arr.length() > 3) ? 3 : arr.length()); i++)
            {
                resultTags.add(arr.getJSONObject(i).getJSONObject("tag").getString("en"));
                resultTags.add(arr.getJSONObject(i).get("confidence").toString());
            }

            return resultTags;
        }
        catch (IOException exception){
            logger.debug("Can't get response with tags");
            return null;
        }
    }

    private static String uploadResource(String imageFilepath) throws IOException {
        try {
            String filepath = imageFilepath;
            File fileToUpload = new File(filepath);

            String endpoint = "/uploads";

            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary = "Image Upload";

            URL urlObject = new URL("https://api.imagga.com/v2" + endpoint);
            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setRequestProperty("Authorization", "Basic " + getAuthString());
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

            responseStream.close();
            connection.disconnect();

            JSONObject obj = new JSONObject(response);
            String pageId = obj.getJSONObject("result").getString("upload_id");
            return pageId;
        }
        catch (IOException ex){
            logger.debug("Can't upload resource");
            return null;
        }
    }

    private static void deleteResource(String resourceId) throws IOException {
        String upload_id = resourceId;
        String url = "https://api.imagga.com/v2/uploads/" + upload_id;

        URL urlObject = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();

        connection.setRequestProperty("Authorization", "Basic " + getAuthString());
        connection.setRequestMethod("DELETE");

        BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        connectionInput.close();
    }

    private static String getAuthString() {
        String credentialsToEncode = Utilities.getImaggaApiKey() + ":" + Utilities.getImaggaApiSecret();
        return Base64.getEncoder().encodeToString(credentialsToEncode.getBytes(StandardCharsets.UTF_8));
    }

}
