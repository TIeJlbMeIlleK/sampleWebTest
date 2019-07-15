package ru.iitdgroup.tests.ves.mock;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.mockserver.model.HttpRequest.request;

public class VesMock implements Closeable {

    private final static Path RESOURCES = Paths.get("resources");
    private static final String DEFAULT_VES_PATH = "/ves/ves-data.json";
    private static final String DEFAULT_VES_EXTEND_PATH = "/ves/ves-extended.json";
    public static final int DEFAULT_VES_PORT = 8010;

    private String vesPath = DEFAULT_VES_PATH;
    private String vesExtendPath = DEFAULT_VES_EXTEND_PATH;
    private String vesResponse;
    private String vesExtendResponse;
    private int port = DEFAULT_VES_PORT;

    private Thread thread;

    private VesMock(int port) {
        this.port = port;
    }


    public VesMock run() {
        Server server = new Server();
        thread = new Thread(server);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    public void stop() {
        thread.interrupt();
    }

    public static VesMock create() {
        return new VesMock(DEFAULT_VES_PORT);
    }

    public VesMock withPort(int port) {
        this.port = port;
        return this;
    }

    public VesMock withVesPath(String vesPath) {
        this.vesPath = vesPath;
        return this;
    }

    public VesMock withVesExtendPath(String vesExtendPath) {
        this.vesExtendPath = vesExtendPath;
        return this;
    }

    public VesMock withVesResponse(String vesResponseFile) {
        try {
            this.vesResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + vesResponseFile), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public VesMock withVesExtendResponse(String vesExtendResponseFile) {
        try {
            this.vesExtendResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + vesExtendResponseFile), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
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
                if (vesResponse == null) {
                    withVesResponse(DEFAULT_VES_PATH);
                }
                if (vesExtendResponse == null) {
                    withVesExtendResponse(DEFAULT_VES_EXTEND_PATH);
                }
                clientAndServer = new ClientAndServer(port);

                initMocks();

                notify();

                Thread.currentThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        private void initMocks() {
            checkRequirements();
            this.clientAndServer
                    .when(request().withMethod("GET").withPath(vesPath))
                    .respond(HttpResponse.response(vesResponse));
            this.clientAndServer
                    .when(request().withMethod("GET").withPath(vesExtendPath))
                    .respond(HttpResponse.response(vesExtendResponse));
        }

        private void checkRequirements() {
            Objects.requireNonNull(vesResponse, "Необходимо установить ответ ВЭС1");
            Objects.requireNonNull(vesExtendResponse, "Необходимо установить ответ ВЭС2");
        }
    }
}
