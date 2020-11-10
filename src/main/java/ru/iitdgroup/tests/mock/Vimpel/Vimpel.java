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

public class Vimpel implements Closeable {

    private final static Path RESOURCES = Paths.get("resources/mock");
    private static final String DEFAULT_ADD_PROFILE_VIMPEL_PATH = "/vimpel/vimpel.xml";
    private static final String DEFAULT_DELETE_VIMPEL_PATH = "/vimpel/vimpel.xml";
    public static final int DEFAULT_VIMPEL_PORT = 3009;
    private final CountDownLatch latch = new CountDownLatch(1);

    private String addProfilePath = DEFAULT_ADD_PROFILE_VIMPEL_PATH;
    private String deleteProfilePath = DEFAULT_DELETE_VIMPEL_PATH;
    private String addProfileResponse;
    private String deleteProfileResponse;
    private int port = DEFAULT_VIMPEL_PORT;
    private Server server;

    private Thread thread;

    private Vimpel(int port) {
        this.port = port;
        withAddProfileResponse(DEFAULT_ADD_PROFILE_VIMPEL_PATH);
        withDeleteProfileResponse(DEFAULT_DELETE_VIMPEL_PATH);
    }


    public Vimpel run() {
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

    public static Vimpel create() {
        return new Vimpel(DEFAULT_VIMPEL_PORT);
    }


    public Vimpel withAddProfileResponse(String addProfileResponse) {
        try {
            this.addProfileResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + addProfileResponse), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Vimpel withDeleteProfileResponse(String deleteProfileResponse) {
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
            Header header = Header.header("Content-Type", "application/xml");
            this.clientAndServer
                    .when(request().withMethod("POST").withPath(addProfilePath))
                    .respond(HttpResponse.response(addProfileResponse).withHeader(header));
            this.clientAndServer
                    .when(request().withMethod("POST").withPath(deleteProfilePath))
                    .respond(HttpResponse.response(deleteProfileResponse).withHeader(header));
        }

        private void checkRequirements() {
            Objects.requireNonNull(addProfileResponse, "Необходимо установить ответ на добавление подписки VIMPEL");
            Objects.requireNonNull(deleteProfileResponse, "Необходимо установить ответ на удаление подписки VIMPEL");
        }

        public void stop() {
            clientAndServer.stop();
        }
    }

    public static void main(String[] args) {
        Vimpel vimpel = new Vimpel(3009);
        vimpel.run();
    }
}
