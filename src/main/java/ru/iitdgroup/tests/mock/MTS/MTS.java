package ru.iitdgroup.tests.mock.MTS;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.mockserver.model.HttpRequest.request;

public class MTS implements Closeable {

    private final static Path RESOURCES = Paths.get("resources/mock");
    private static final String DEFAULT_ADD_PROFILE_MTS_PATH = "/mts/AddProfile/AddProfileMTS.xml";
    private static final String DEFAULT_CHANGE_PROFILE_ROLE_PATH = "/mts/ChangeProfileRole/ChangeProfileRoleMTS.xml";
    private static final String DEFAULT_DELETE_PROFILE_ROLE_PATH = "/mts/DeleteProfile/DeleteProfileMST.xml";
    public static final int DEFAULT_MTS_PORT = 3006;
    private final CountDownLatch latch = new CountDownLatch(1);

    private String mtsAddProfileMtsPath = DEFAULT_ADD_PROFILE_MTS_PATH;
    private String mtsChangeProfileRolePath = DEFAULT_CHANGE_PROFILE_ROLE_PATH;
    private String mtsDeleteProfilePath = DEFAULT_DELETE_PROFILE_ROLE_PATH;
    private String addProfileResponse;
    private String changeProfileRoleResponse;
    private String deleteProfileResponse;
    private int port = DEFAULT_MTS_PORT;
    private Server server;

    private Thread thread;

    private MTS(int port) {
        this.port = port;
        withAddProfileResponse(DEFAULT_ADD_PROFILE_MTS_PATH);
        withChangeProfileRoleResponse(DEFAULT_CHANGE_PROFILE_ROLE_PATH);
        withDeleteProfileRoleResponse(DEFAULT_DELETE_PROFILE_ROLE_PATH);
    }


    public MTS run() {
        this.server = new Server();
        thread = new Thread(server);
        thread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    public void stop() {
        server.stop();
        thread.interrupt();
    }

    public static MTS create() {
        return new MTS(DEFAULT_MTS_PORT);
    }

    public MTS withPort(int port) {
        this.port = port;
        return this;
    }

    public MTS withCommandServicePath(String commandServicePath) {
        this.addProfileResponse = commandServicePath;
        return this;
    }

//    public CommandServiceMock withVesExtendPath(String commandServiceExtendPath) {
//        this.vesExtendPath = commandServiceExtendPath;
//        return this;
//    }

    public MTS withAddProfileResponse(String addProfileResponse) {
        try {
            this.addProfileResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + addProfileResponse), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MTS withChangeProfileRoleResponse(String changeProfileRoleResponse) {
        try {
            this.changeProfileRoleResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + changeProfileRoleResponse), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MTS withDeleteProfileRoleResponse(String deleteProfileRoleResponse) {
        try {
            this.deleteProfileResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + deleteProfileRoleResponse), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public String getVesResponse() {
        return addProfileResponse;
    }

    public void setVesResponse(String commandServiceResponse) {
        this.addProfileResponse = commandServiceResponse;
    }

//    public String getVesExtendResponse() {
//        return vesExtendResponse;
//    }
//
//    public void setVesExtendResponse(String vesExtendResponse) {
//        this.vesExtendResponse = vesExtendResponse;
//    }

    @Override
    public void close() {
        stop();
    }

    private class Server implements Runnable {

        private ClientAndServer clientAndServer;

        @Override
        public void run() {
            try {
                clientAndServer = new ClientAndServer(port);
                initMocks();
                latch.countDown();
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        private void initMocks() {
            checkRequirements();
            Header header = Header.header("Content-Type", "application/xml");
            this.clientAndServer
                    .when(request().withMethod("POST").withPath(mtsAddProfileMtsPath))
                    .respond(HttpResponse.response(addProfileResponse).withHeader(header));
            this.clientAndServer
                    .when(request().withMethod("POST").withPath(mtsChangeProfileRolePath))
                    .respond(HttpResponse.response(changeProfileRoleResponse).withHeader(header));
            this.clientAndServer
                    .when(request().withMethod("POST").withPath(mtsDeleteProfilePath))
                    .respond(HttpResponse.response(deleteProfileResponse).withHeader(header));
        }

        private void checkRequirements() {
            Objects.requireNonNull(addProfileResponse, "Необходимо установить ответ на добавление подписки МТС");
            Objects.requireNonNull(changeProfileRoleResponse, "Необходимо установить ответ на изменении подписки МТС");
            Objects.requireNonNull(deleteProfileResponse, "Необходимо установить ответ на удаление подписки МТС");
        }

        public void stop() {
            clientAndServer.stop();
        }
    }
}
