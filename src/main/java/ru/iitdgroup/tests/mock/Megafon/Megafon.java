package ru.iitdgroup.tests.mock.Megafon;

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

public class Megafon implements Closeable {

    private final static Path RESOURCES = Paths.get("resources/mock");
    private static final String DEFAULT_ADD_PROFILE_MEGAFON_PATH = "/megafon/megafon.json";
    private static final String DEFAULT_DELETE_MEGAFON_PATH = "/megafon/megafon.json";
    public static final int DEFAULT_MEGAFON_PORT = 3007;
    private final CountDownLatch latch = new CountDownLatch(1);

    private String addProfilePath = DEFAULT_ADD_PROFILE_MEGAFON_PATH;
    private String deleteProfilePath = DEFAULT_DELETE_MEGAFON_PATH;
    private String addProfileResponse;
    private String deleteProfileResponse;
    private int port = DEFAULT_MEGAFON_PORT;
    private Server server;

    private Thread thread;

    private Megafon(int port) {
        this.port = port;
        withAddProfileResponse(DEFAULT_ADD_PROFILE_MEGAFON_PATH);
        withDeleteProfileResponse(DEFAULT_DELETE_MEGAFON_PATH);
    }


    public Megafon run() {
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

    public static Megafon create() {
        return new Megafon(DEFAULT_MEGAFON_PORT);
    }


    public Megafon withAddProfileResponse(String addProfileResponse) {
        try {
            this.addProfileResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + addProfileResponse), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Megafon withDeleteProfileResponse(String deleteProfileResponse) {
        try {
            this.deleteProfileResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + deleteProfileResponse), StandardCharsets.UTF_8)
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
                    .when(request().withMethod("POST").withPath(addProfilePath))
                    .respond(HttpResponse.response(addProfileResponse).withHeader(header));
            this.clientAndServer
                    .when(request().withMethod("POST").withPath(deleteProfilePath))
                    .respond(HttpResponse.response(deleteProfileResponse).withHeader(header));
        }

        private void checkRequirements() {
            Objects.requireNonNull(addProfileResponse, "Необходимо установить ответ на добавление подписки Мегафон");
            Objects.requireNonNull(deleteProfileResponse, "Необходимо установить ответ на удаление подписки Мегафон");
        }

        public void stop() {
            clientAndServer.stop();
        }
    }
}
