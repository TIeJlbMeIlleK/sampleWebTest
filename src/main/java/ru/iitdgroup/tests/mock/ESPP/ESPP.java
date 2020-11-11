package ru.iitdgroup.tests.mock.ESPP;

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

public class ESPP implements Closeable {

    private final static Path RESOURCES = Paths.get("resources/mock");
    private static final String DEFAULT_ESPP_PATH = "/ESPP/ESPP.xml";
    public static final int DEFAULT_ESPP_PORT = 3010;
    private final CountDownLatch latch = new CountDownLatch(1);

    private String esppPath = DEFAULT_ESPP_PATH;
    private String esppResponse;
    private int port = DEFAULT_ESPP_PORT;
    private Server server;

    private Thread thread;

    private ESPP(int port) {
        this.port = port;
        withEsppResponse(DEFAULT_ESPP_PATH);
    }


    public ESPP run() {
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

    public static ESPP create() {
        return new ESPP(DEFAULT_ESPP_PORT);
    }


    public ESPP withEsppResponse(String esppResponse) {
        try {
            this.esppResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + esppResponse), StandardCharsets.UTF_8)
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
                    .when(request().withMethod("POST").withPath(esppPath))
                    .respond(HttpResponse.response(esppResponse).withHeader(header));
        }

        private void checkRequirements() {
            Objects.requireNonNull(esppResponse, "Необходимо установить ответ от ЕСПП");
        }

        public void stop() {
            clientAndServer.stop();
        }
    }
}
