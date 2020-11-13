package ru.iitdgroup.tests.mock.commandservice;

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

public class CommandServiceMock implements Closeable {

    private final static Path RESOURCES = Paths.get("resources");
    private static final String DEFAULT_COMMAND_SERVICE_PATH = "/commandservice/commandservice.xml";
    public static final int DEFAULT_COMMAND_SERVICE_PORT = 3005;
    private final CountDownLatch latch = new CountDownLatch(1);

    private String commandServicePath = DEFAULT_COMMAND_SERVICE_PATH;
    private String commandServiceResponse;
    private int port = DEFAULT_COMMAND_SERVICE_PORT;
    private Server server;

    private Thread thread;

    public CommandServiceMock(int port) {
        this.port = port;
        withCommandServiceResponse(DEFAULT_COMMAND_SERVICE_PATH);
    }


    public CommandServiceMock run() {
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

    public static CommandServiceMock create() {
        return new CommandServiceMock(DEFAULT_COMMAND_SERVICE_PORT);
    }

    public CommandServiceMock withPort(int port) {
        this.port = port;
        return this;
    }

    public CommandServiceMock withCommandServicePath(String commandServicePath) {
        this.commandServicePath = commandServicePath;
        return this;
    }


    public CommandServiceMock withCommandServiceResponse(String commandServiceResponseFile) {
        try {
            this.commandServiceResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + commandServiceResponseFile), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }


    public String getCommandServiceResponse() {
        return commandServiceResponse;
    }

    public void setCommandServiceResponse(String commandServiceResponse) {
        this.commandServiceResponse = commandServiceResponse;
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
                    .when(request().withMethod("POST").withPath(commandServicePath))
                    .respond(HttpResponse.response(commandServiceResponse).withHeader(header));
        }

        private void checkRequirements() {
            Objects.requireNonNull(commandServiceResponse, "Необходимо установить ответ от ДБО");
        }

        public void stop() {
            clientAndServer.stop();
        }
    }
}
