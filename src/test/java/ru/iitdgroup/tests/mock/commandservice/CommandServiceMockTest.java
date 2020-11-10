package ru.iitdgroup.tests.mock.commandservice;

import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import static org.testng.AssertJUnit.assertEquals;

public class CommandServiceMockTest {

    @Test
    public void testMock() {
        try (CommandServiceMock ignored = CommandServiceMock.create()
                .withCommandServiceResponse("commandservice/commandservice.xml")
                .run()) {

            assertEquals("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:prof=\"http://10.241.35.91:8082/soap/profilemanage\">\n" +
                    "<soapenv:Header/>\n" +
                    "<soapenv:Body>\n" +
                    "<prof:AddProfileResponse>\n" +
                    "<prof:return>\n" +
                    "<prof:StatusCode>0000</prof:StatusCode>\n" +
                    "<prof:Description>DESC</prof:Description>\n" +
                    "<prof:ReqId>0000</prof:ReqId>\n" +
                    "</prof:return>\n" +
                    "</prof:AddProfileResponse>\n" +
                    "</soapenv:Body>\n" +
                    "</soapenv:Envelope>", getRequest("/"));
        }

    }

    private String getRequest(String url) {
        try {
            URL yahoo = new URL("http://localhost:" + CommandServiceMock.DEFAULT_COMMAND_SERVICE_PORT + url);
            URLConnection yc = yahoo.openConnection();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()))) {
                StringBuilder result = new StringBuilder();
                String last = "";
                while (last != null) {
                    last = in.readLine();
                    if (last != null) {
                        result.append(last);
                    }
                }
                return result.toString();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}