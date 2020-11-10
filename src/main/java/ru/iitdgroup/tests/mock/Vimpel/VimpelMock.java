package ru.iitdgroup.tests.mock.Vimpel;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;

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

public class VimpelMock implements Closeable {

    private final static Path RESOURCES = Paths.get("resources");
    private static final String DEFAULT_VES_PATH = "/ves/ves-data.json";
    private static final String DEFAULT_VES_EXTEND_PATH = "/ves/ves-extended.json";
    public static final int DEFAULT_VES_PORT = 8010;
    private final CountDownLatch latch = new CountDownLatch(1);

    private String vesPath = DEFAULT_VES_PATH;
    private String vesExtendPath = DEFAULT_VES_EXTEND_PATH;
    private String vesResponse;
    private String vesExtendResponse;
    private int port = DEFAULT_VES_PORT;
    private Server server;

    private Thread thread;

    private VimpelMock(int port) {
        this.port = port;
        withVesResponse(DEFAULT_VES_PATH);
        withVesExtendResponse(DEFAULT_VES_EXTEND_PATH);
    }


    public VimpelMock run() {
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

    public static VimpelMock create() {
        return new VimpelMock(DEFAULT_VES_PORT);
    }

    public VimpelMock withPort(int port) {
        this.port = port;
        return this;
    }

    public VimpelMock withVesPath(String vesPath) {
        this.vesPath = vesPath;
        return this;
    }

    public VimpelMock withVesExtendPath(String vesExtendPath) {
        this.vesExtendPath = vesExtendPath;
        return this;
    }

    public VimpelMock withVesResponse(String vesResponseFile) {
        try {
            this.vesResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + vesResponseFile), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public VimpelMock withVesExtendResponse(String vesExtendResponseFile) {
        try {
            this.vesExtendResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + vesExtendResponseFile), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public String getVesResponse() {
        return vesResponse;
    }

    public void setVesResponse(String vesResponse) {
        this.vesResponse = vesResponse;
    }

    public String getVesExtendResponse() {
        return vesExtendResponse;
    }

    public void setVesExtendResponse(String vesExtendResponse) {
        this.vesExtendResponse = vesExtendResponse;
    }

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
            Header header = Header.header("Content-Type", "application/json");
            this.clientAndServer
                    .when(request().withMethod("GET").withPath(vesPath))
                    .respond(HttpResponse.response(vesResponse).withHeader(header));
            this.clientAndServer
                    .when(request().withMethod("GET").withPath(vesExtendPath))
                    .respond(HttpResponse.response(vesExtendResponse).withHeader(header));
        }

        private void checkRequirements() {
            Objects.requireNonNull(vesResponse, "Необходимо установить ответ ВЭС1");
            Objects.requireNonNull(vesExtendResponse, "Необходимо установить ответ ВЭС2");
        }

        public void stop() {
            clientAndServer.stop();
        }
    }
}
