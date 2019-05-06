package ru.iitgroup.tests.ves.mock;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import ru.iitdgroup.solution.utils.file.FileUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.mockserver.model.HttpRequest.request;

public class VesMock {

    private static final String DEFAULT_VES_PATH = "/ves";
    private static final String DEFAULT_VES_EXTEND_PATH = "/ves-extend";
    public static final int DEFAULT_VES_PORT = 8010;

    private ClientAndServer clientAndServer;
    private String vesPath = DEFAULT_VES_PATH;
    private String vesExtendPath = DEFAULT_VES_EXTEND_PATH;
    private String vesResponse;
    private String vesExtendResponse;
    private int port = DEFAULT_VES_PORT;

    public VesMock(int port) {
        this.port = port;
    }

    public VesMock() {

    }

    private void initMocks() {
        this.clientAndServer.when(
                request()
                        .withMethod("GET")
                        .withPath(vesPath)
        ).respond(HttpResponse.response(vesResponse));
        this.clientAndServer.when(
                request()
                        .withMethod("GET")
                        .withPath(vesExtendPath)
        ).respond(HttpResponse.response(vesExtendResponse));
    }

    public void run() {
        Objects.requireNonNull(vesResponse, "Необходимо установить ответ ВЭС1");
        Objects.requireNonNull(vesExtendResponse, "Необходимо установить ответ ВЭС2");
        clientAndServer = new ClientAndServer(this.port);
        initMocks();

    }

    public static VesMock onPort(Integer port) {
        if(port == null){
            return new VesMock();
        }
        return new VesMock(port);
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
            String vesResponse = FileUtil.readFile(vesResponseFile, StandardCharsets.UTF_8);
            this.vesResponse = vesResponse;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public VesMock withVesExtendResponse(String vesExtendResponseFile) {
        try {
            String vesExtendResponse = FileUtil.readFile(vesExtendResponseFile, StandardCharsets.UTF_8);
            this.vesExtendResponse = vesExtendResponse;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

}
