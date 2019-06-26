package ru.iitdgroup.tests.apidriver;

import ru.iitdgroup.intellinx.dbo.common.ObjectFactory;
import ru.iitdgroup.intellinx.dbo.common.ResultType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class DBOAntiFraudWS {

    private final String urlIC;
    private final String basicAuth;
    private final Unmarshaller responseUnmarshaller;

    private Integer lastResponseCode;
    private ResultType lastResponse;

    public DBOAntiFraudWS(String urlIC, String username, String password) {
        this.urlIC = urlIC;

        // собираем byte[] логина:пароля
        byte[] rawBasicAuth = new byte[username.getBytes().length + 1 + password.getBytes().length];
        int i = 0;
        for (byte usernameByte : username.getBytes()) {
            rawBasicAuth[i] = usernameByte;
            i++;
        }
        rawBasicAuth[i] = ":".getBytes()[0];
        i++;
        for (byte passwordByte : password.getBytes()) {
            rawBasicAuth[i] = passwordByte;
            i++;
        }
        this.basicAuth = Base64.getEncoder().encodeToString(rawBasicAuth);

        try {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
            this.responseUnmarshaller = jc.createUnmarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    public DBOAntiFraudWS send(Template template) throws IOException {
        lastResponseCode = null;
        lastResponse = null;

        URL url = new URL(urlIC);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "IITD Tester");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
        conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        conn.setRequestProperty("SOAPAction", "");
        conn.setRequestProperty("Authorization", "Basic " + basicAuth);
        conn.setRequestProperty("Host", "localhost:8080");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("User-Agent", "IITD-Tester");

        //====================================================================
        //conn.setRequestProperty("Content-Length","2152");
        //====================================================================

        conn.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            SOAPMessage requestMessage = MessageFactory.newInstance().createMessage();
            requestMessage.getSOAPBody().addDocument(template.marshalToDocument());
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                requestMessage.writeTo(outputStream);
                wr.writeBytes(outputStream.toString());
            }
            lastResponseCode = conn.getResponseCode();
            SOAPMessage responseMessage = MessageFactory
                    .newInstance()
                    .createMessage(new MimeHeaders(), conn.getInputStream());
            lastResponse = (ResultType) ((JAXBElement) this.responseUnmarshaller
                    .unmarshal(responseMessage.getSOAPBody().getFirstChild())).getValue();
        } catch (SOAPException | JAXBException | ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }

        return this;
    }

    public Integer getResponseCode() {
        return lastResponseCode;
    }

    public ResultType getResponse() {
        return lastResponse;
    }

    public boolean isSuccessResponse() {
        return getResponse().isSuccess();
    }

}