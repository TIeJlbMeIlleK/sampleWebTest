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
        VesMock vesMock = new VesMock()
                .withVesPath("/")
                .withVesExtendPath("/extend")
                .withVesResponse("ves/ves-data.json")
                .withVesExtendResponse("ves/ves-extended.json");
        vesMock.run();

        assertEquals(getRequest("/"), "{  \"status\": 200,  \"data\": {    \"alerts\": [      7,      10,      46,      111,      129    ],    \"info\": []  }}");
        assertEquals(getRequest("/extend"), "{  \"status\": \"success\",  \"error\": null,  \"data\": {    \"major\": 1,    \"minor\": 0,    \"count\": 4,    \"fromEventId\": \"348332009\",    \"toEventId\": \"348332014\",    \"events\": [      {        \"id\": \"348332009\",        \"priority\": \"MEDIUM\",        \"type_id\": \"10\",        \"type\": \"????? ?????????? ? ???????\",        \"created\": \"2019-04-02T13:04:48+03:00\",        \"published\": \"2019-04-02T13:04:53+03:00\",        \"remote_addr\": \"195.26.169.61\",        \"remote_addr_info\": {          \"country\": \"RU\",          \"city\": \"??????\",          \"isp\": \"ZAO Laborotory of New Information Technologies LAN\"        },        \"sid\": \"1325843881000\",        \"csid\": \"NETdgiDku6H3exK3negsU2G_C7WRh4aKh3XuvbhXXasu_kc-k3hb!1297066326!1554199486692\",        \"uid\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\",        \"login_hash\": \"246aace1ef37723d05a0b2cb031265935b236ec3\",        \"login_rsa\": \"\",        \"channel\": {          \"type\": \"web\",          \"version\": \"1.0\",          \"name\": null        },        \"device\": {          \"id\": \"448395921\",          \"platform\": \"Win32\",          \"isMobile\": \"N\",          \"browser\": \"Chrome\"        },        \"screen\": {          \"id\": \"3463685950\",          \"width\": \"1366\",          \"height\": \"768\",          \"color_depth\": \"24\"        },        \"referer\": \"https://online.rshb.ru/ib6/wf2/retail/ib/loginretaildefault;jsessionid=NETdgiDku6H3exK3negsU2G_C7WRh4aKh3XuvbhXXasu_kc-k3hb!1297066326\",        \"user_agent\": \"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 YaBrowser/19.3.1.828 Yowser/2.5 Safari/537.36\",        \"comment\": \"\",        \"score_device\": 10,        \"score_events\": 50,        \"virus_detected\": \"\",        \"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\",        \"details\": {}      },      {        \"id\": \"348332011\",        \"priority\": \"LOW\",        \"type_id\": \"7\",        \"type\": \"????? ???? ???????? ? ?????? ??????????\",        \"created\": \"2019-04-02T13:04:48+03:00\",        \"published\": \"2019-04-02T13:04:53+03:00\",        \"remote_addr\": \"195.26.169.61\",        \"remote_addr_info\": {          \"country\": \"RU\",          \"city\": \"??????\",          \"isp\": \"ZAO Laborotory of New Information Technologies LAN\"        },        \"sid\": \"1325843881000\",        \"csid\": \"NETdgiDku6H3exK3negsU2G_C7WRh4aKh3XuvbhXXasu_kc-k3hb!1297066326!1554199486692\",        \"uid\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\",        \"login_hash\": \"246aace1ef37723d05a0b2cb031265935b236ec3\",        \"login_rsa\": \"\",        \"channel\": {          \"type\": \"web\",          \"version\": \"1.0\",          \"name\": null        },        \"device\": {          \"id\": \"448395921\",          \"platform\": \"Win32\",          \"isMobile\": \"N\",          \"browser\": \"Chrome\"        },        \"screen\": {          \"id\": \"3463685950\",          \"width\": \"1366\",          \"height\": \"768\",          \"color_depth\": \"24\"        },        \"referer\": \"https://online.rshb.ru/ib6/wf2/retail/ib/loginretaildefault;jsessionid=NETdgiDku6H3exK3negsU2G_C7WRh4aKh3XuvbhXXasu_kc-k3hb!1297066326\",        \"user_agent\": \"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 YaBrowser/19.3.1.828 Yowser/2.5 Safari/537.36\",        \"comment\": \"\",        \"score_device\": 10,        \"score_events\": 0,        \"virus_detected\": \"\",        \"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\",        \"details\": {}      },      {        \"id\": \"348332013\",        \"priority\": \"MEDIUM\",        \"type_id\": \"111\",        \"type\": \"????? ?????????? ? ??????? (?????????? + ?????????)\",        \"created\": \"2019-04-02T13:04:48+03:00\",        \"published\": \"2019-04-02T13:04:53+03:00\",        \"remote_addr\": \"195.26.169.61\",        \"remote_addr_info\": {          \"country\": \"RU\",          \"city\": \"??????\",          \"isp\": \"ZAO Laborotory of New Information Technologies LAN\"        },        \"sid\": \"1325843881000\",        \"csid\": \"NETdgiDku6H3exK3negsU2G_C7WRh4aKh3XuvbhXXasu_kc-k3hb!1297066326!1554199486692\",        \"uid\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\",        \"login_hash\": \"246aace1ef37723d05a0b2cb031265935b236ec3\",        \"login_rsa\": \"\",        \"channel\": {          \"type\": \"web\",          \"version\": \"1.0\",          \"name\": null        },        \"device\": {          \"id\": \"448395921\",          \"platform\": \"Win32\",          \"isMobile\": \"N\",          \"browser\": \"Chrome\"        },        \"screen\": {          \"id\": \"3463685950\",          \"width\": \"1366\",          \"height\": \"768\",          \"color_depth\": \"24\"        },        \"referer\": \"https://online.rshb.ru/ib6/wf2/retail/ib/loginretaildefault;jsessionid=NETdgiDku6H3exK3negsU2G_C7WRh4aKh3XuvbhXXasu_kc-k3hb!1297066326\",        \"user_agent\": \"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 YaBrowser/19.3.1.828 Yowser/2.5 Safari/537.36\",        \"comment\": \"\",        \"score_device\": 20,        \"score_events\": 50,        \"virus_detected\": \"\",        \"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\",        \"details\": {}      },      {        \"id\": \"348332014\",        \"priority\": \"LOW\",        \"type_id\": \"46\",        \"type\": \"???? ? ???\",        \"created\": \"2019-04-02T13:04:48+03:00\",        \"published\": \"2019-04-02T13:04:53+03:00\",        \"remote_addr\": \"195.26.169.61\",        \"remote_addr_info\": {          \"country\": \"RU\",          \"city\": \"??????\",          \"isp\": \"ZAO Laborotory of New Information Technologies LAN\"        },        \"sid\": \"1325843881000\",        \"csid\": \"NETdgiDku6H3exK3negsU2G_C7WRh4aKh3XuvbhXXasu_kc-k3hb!1297066326!1554199486692\",        \"uid\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\",        \"login_hash\": \"246aace1ef37723d05a0b2cb031265935b236ec3\",        \"login_rsa\": \"\",        \"channel\": {          \"type\": \"web\",          \"version\": \"1.0\",          \"name\": null        },        \"device\": {          \"id\": \"448395921\",          \"platform\": \"Win32\",          \"isMobile\": \"N\",          \"browser\": \"Chrome\"        },        \"screen\": {          \"id\": \"3463685950\",          \"width\": \"1366\",          \"height\": \"768\",          \"color_depth\": \"24\"        },        \"referer\": \"https://online.rshb.ru/ib6/wf2/retail/ib/loginretaildefault;jsessionid=NETdgiDku6H3exK3negsU2G_C7WRh4aKh3XuvbhXXasu_kc-k3hb!1297066326\",        \"user_agent\": \"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 YaBrowser/19.3.1.828 Yowser/2.5 Safari/537.36\",        \"comment\": \"\",        \"score_device\": 20,        \"score_events\": 0,        \"virus_detected\": \"\",        \"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\",        \"details\": {}      }    ]  }}");
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