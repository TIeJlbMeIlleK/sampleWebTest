package ru.iitdgroup.tests.apidriver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBOAntiFraudWS {

    private static String urlIC;
    private Integer lastResponseCode;
    private HttpURLConnection conn;
    private String lastResponse;


    public DBOAntiFraudWS(String urlIC) {
        DBOAntiFraudWS.urlIC = urlIC;
    }


    public DBOAntiFraudWS send(Transaction t) throws IOException {

        lastResponseCode = null;
        lastResponse = null;

        URL url = new URL(urlIC);
        conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "IITD Tester");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");


        conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
        conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        conn.setRequestProperty("SOAPAction", "");
        conn.setRequestProperty("Authorization", "Basic d3NVc2VyOndzVXNlcg==");
        conn.setRequestProperty("Host", "localhost:8080");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("User-Agent", "IITD-Tester");

        //====================================================================
        //conn.setRequestProperty("Content-Length","2152");
        //====================================================================

        conn.setDoOutput(true);


        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(t.toString());
        wr.flush();
        wr.close();

        lastResponseCode = conn.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        lastResponse = response.toString();
        return this;
    }

    public void verifyHTTPanswer() {
        if (lastResponseCode != 200 || lastResponse == null) throw
                new ICMalfunctionError(String.format("No data, IC Respone code %s", lastResponse));
    }


    public String getOptional(String tagName) {
        return getValue(tagName, true);
    }

    public String getMandatory(String tagName) {
        return getValue(tagName, false);
    }


    public String getValue(String tagName, boolean optional) {
        final String regex = String.format(".+<%s>(.+)<.%s>.+", tagName, tagName);

        final Pattern pattern = Pattern.compile(regex);

        final Matcher matcher = pattern.matcher(lastResponse);

        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            if (optional) {
                return null;
            } else {
                throw new Error(String.format("No tag [%s] in response: %s", tagName, lastResponse));
            }
        }
    }

    public Integer getResponseCode() {
        return lastResponseCode;
    }

    public String getResponse() {
        return lastResponse;
    }

    public String getSuccessCode() {
        verifyHTTPanswer();
        return getMandatory("com:Success");
    }

    public String getErrorCode() {
        verifyHTTPanswer();
        return getOptional("com:ErrorCode");
    }

    public String getErrorMessage() {
        verifyHTTPanswer();
        return getOptional("com:ErrorMessage");
    }

}