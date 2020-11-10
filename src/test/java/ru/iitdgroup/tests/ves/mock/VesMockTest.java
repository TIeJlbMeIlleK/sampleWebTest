package ru.iitdgroup.tests.ves.mock;

import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import static org.testng.AssertJUnit.assertEquals;

public class VesMockTest {

    @Test
    public void testMock() {
        try (VesMock ignored = VesMock.create()
                .withVesPath("/")
                .withVesExtendPath("/extend")
                .withVesResponse("ves/ves-data.json")
                .withVesExtendResponse("ves/ves-extended.json")
                .run()) {

            assertEquals("{\"status\":200,\"data\":{\"alerts\":[7],\"info\":[]}}", getRequest("/"));
            assertEquals("{  \"uuid\": \"9265faf0-8974-11ea-be28-e52c21e9dc49\",  \"customer_id\": 122,  \"type_id\": 4,  \"type_title\": \"Вход в личный кабинет\",  \"risk_level\": \"LOW\",  \"time\": \"2020-05-28T20:20:35+03:00\",  \"ip\": \"83.219.147.193\",  \"ip_info\": {    \"country\": \"RU\",    \"city\": \"Калининград\",    \"isp\": \"TIS Dialog LLC\"  },  \"agent_id\": \"9bc1dde2-d1f3-48b5-a1e1-9881f4725844\",  \"login_hash\": \"305\",  \"login\": \"ilushka305\",  \"score_alert\": 0,  \"score_device\": 0,  \"session_id\": \"dfgjnsdfgnfdkjsgnlfdgfdhkjdf\",  \"screen\": {    \"width\": 892,    \"height\": 412,    \"bit_depth\": 24  },  \"device\": {    \"platform\": \"Linux armv8l\",    \"browser\": \"Netscape Mozilla\",    \"mobile\": true  },  \"device_hash\": \"dfgjnsdfgnfdkjsgnlfdgfdhkjdf\",  \"user_agent\": \"Mozilla/5.0 (Linux; Android 9; SM-A505FN Build/PPR1.180610.011; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/81.0.4044.117 Mobile Safari/537.36\",  \"referer\": \"https://online.rshb.ru/\",  \"channel\": \"android\"}", getRequest("/extend"));
        }

    }

    private String getRequest(String url) {
        try {
            URL yahoo = new URL("http://localhost:" + VesMock.DEFAULT_VES_PORT + url);
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