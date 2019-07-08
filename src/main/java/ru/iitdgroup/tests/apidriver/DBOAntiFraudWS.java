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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * Утилитарный класс для обращение по SOAP на основе шаблонов
 */
public class DBOAntiFraudWS {

    private final String urlIC;
    private final String basicAuth;
    private final Unmarshaller responseUnmarshaller;

    private Integer lastResponseCode;
    private ResultType lastResponse;

    /**
     * @param urlIC адрес WS
     * @param username логин
     * @param password пароль
     */
    public DBOAntiFraudWS(String urlIC, String username, String password) {
        this.urlIC = urlIC;
        this.basicAuth = generateBase64Auth(username, password);
        try {
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
            this.responseUnmarshaller = jc.createUnmarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Отправляет SOAP запрос на сервер
     * @param template шаблон запроса
     * @return текущие состояние
     * @throws SOAPException когда ошибка при обращении по SOAP
     * @throws IllegalStateException в случаях {@link IOException}, {@link JAXBException} и {@link ParserConfigurationException}
     */
    public DBOAntiFraudWS send(Template template) throws SOAPException {
        lastResponseCode = null;
        lastResponse = null;

        HttpURLConnection conn;
        try {
            URL url = new URL(urlIC);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "IITD Tester");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
            conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
            conn.setRequestProperty("SOAPAction", "");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Host", "localhost:8080");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("User-Agent", "IITD-Tester");

            //====================================================================
            //conn.setRequestProperty("Content-Length","2152");
            //====================================================================

            conn.setDoOutput(true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        try (OutputStream wr = conn.getOutputStream()) {
            SOAPMessage requestMessage = MessageFactory.newInstance().createMessage();
            requestMessage.getSOAPBody().addDocument(template.marshalToDocument());
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                requestMessage.writeTo(outputStream);
                wr.write(outputStream.toString().getBytes());
            }
            lastResponseCode = conn.getResponseCode();
            SOAPMessage responseMessage = MessageFactory
                    .newInstance()
                    .createMessage(new MimeHeaders(), conn.getInputStream());
            lastResponse = (ResultType) ((JAXBElement) this.responseUnmarshaller
                    .unmarshal(responseMessage.getSOAPBody().getFirstChild())).getValue();
        } catch (IOException | JAXBException | ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }

        return this;
    }

    /**
     * @return последний код ответа (nullable)
     */
    public Integer getResponseCode() {
        return lastResponseCode;
    }

    /**
     * @return последний ответ (nullable)
     */
    public ResultType getResponse() {
        return lastResponse;
    }

    /**
     * @return успешен ли последний ответ. false в случае lastResponse is null
     */
    public boolean isSuccessResponse() {
        return getResponse() != null && getResponse().isSuccess();
    }

    /**
     * @param username логин
     * @param password пароль
     * @return base64 encoded Authorization header
     */
    private String generateBase64Auth(String username, String password) {
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
        return "Basic " + Base64.getEncoder().encodeToString(rawBasicAuth);
    }

}