package ru.iitgroup.tests.apidriver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
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

    public static void main(String[] args) throws IOException {

        //TODO: Добавлять и удалять теги

        Random r = new Random();

        DBOAntiFraudWS ws = new DBOAntiFraudWS("http://192.168.7.151:7780/InvestigationCenter/api/webservices/DBOAntiFraudWS");

        Transaction t = Transaction.fromFile("tran1.xml");
        t.withDBOId( 2);
        t.withCIFId( 1);
        t.withTransactionId( 5_000_000+r.nextInt(100000));

        ws.send(t);

        System.out.println(String.format("Response code: %d\n",ws.getResponseCode()));
        System.out.println(String.format("Code: %s, Error: %s, ErrorMessage: %s.",
                ws.getSuccessCode(),
                ws.getErrorCode(),
                ws.getErrorMessage()));
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

    public Integer getResponseCode() {
        return lastResponseCode;
    }

    public String getResponse() {
        return lastResponse;
    }

    public String getSuccessCode(){
        verifyHTTPanswer();
        return getValue("com:Success");
    }

    public String getErrorCode(){
        verifyHTTPanswer();
        return getValue("com:ErrorCode");
    }

    public String getErrorMessage(){
        verifyHTTPanswer();
        return getValue("com:ErrorMessage");
    }

    private void verifyHTTPanswer() {
        if (lastResponseCode != 200 || lastResponse == null) throw
                new ICMalfunctionError(String.format("No data, IC Respone code %d", lastResponse));
    }


    private  String getValue( String tagName){
        final String regex = String.format(".+<%s>(.+)<.%s>.+",tagName,tagName);

        final Pattern pattern = Pattern.compile(regex);

        final Matcher matcher = pattern.matcher(lastResponse);

        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new Error(String.format("No tag [%s] in response: %s" ,tagName,lastResponse));
        }

    }

}
