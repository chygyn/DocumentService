import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.BodyHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class VerticleMain {

    private HashMap<String, URLObject> urlMap;
    private Vertx vertx;
    private String base64Auth;

    private VerticleMain() {
        this.vertx = Vertx.vertx();

    }

    public void run() {
        urlMap = new HashMap<String, URLObject>();
        /*
         * Authorisation part. Read config.properties for user and password values
         * */
        InputStream inputStream = null;
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
            String user = prop.getProperty("auth.user");
            String password = prop.getProperty("auth.password");
            System.out.println("Auth: user "+user+", password "+password);
            base64Auth = Base64.getEncoder().encodeToString(new StringBuilder(user).append(":").append(password).toString().getBytes());
            System.out.println("Autehtication encoded: "+base64Auth);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>POST /createUrl</h1>");
        });
        /* router.route(HttpMethod.POST, "/documents/:configuration*").handler(BodyHandler.create());*/
        router.post("/documents/:configuration").handler(BodyHandler.create()).handler(this::createURL);
        router.get("/documents/:configuration/:documentId").handler(this::checkURL).blockingHandler(this::getDocument).handler(this::finalCheck);

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router::accept).listen(8080);

        /*vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(result.cause());
                            }
                        }
                );*/
    }

    private void createURL(RoutingContext ctx) {
        //сделать ручками декодирование
        URLObject newUrl = Json.decodeValue(ctx.getBodyAsString(), URLObject.class);
        UUID id;

        do {
            id = UUID.randomUUID();
        } while (urlMap.containsKey(id));
        newUrl.setExternalUUID(id);
        urlMap.put(id.toString(), newUrl);
        System.out.println("updated urlMap ");
        urlMap.forEach((uuid, urlObject) -> System.out.println("UUID: " + uuid.toString() + " Object: " + urlObject.toString()));
        //ручной енкод
        String urlObjectJSON = newUrl.createJSON().toString();
        ctx.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
                .end(urlObjectJSON);
    }

    private void checkURL(RoutingContext ctx) {
        //доделать
        String id = ctx.request().getParam("documentId");
        URLObject urlInMemory = urlMap.get(id);
        if (urlInMemory != null) {
            long time = new Date().getTime();
            if (checkRequestsCount(urlInMemory) & urlInMemory.getCreatedTime() + urlInMemory.getLifeTime() <= time ) {
                ctx.next();
            } else {
                ctx.response().putHeader("content-type", "text/html")
                        .end("<h1>Invalid URL. Required new URL.</h1>");
            }
        }else{
            ctx.response().putHeader("content-type", "text/html")
                    .end("<h1>Invalid URL. Required new URL.</h1>");
        }
    }

    private Boolean checkRequestsCount(URLObject urlObj){
        if (urlObj.getRequests()>0)
            return true;
        else
            return false;
    }

    private void getDocument(RoutingContext ctx) {
        //retrieve object by id
        String id = ctx.request().getParam("documentId");
        //добавить параметр в контекст
        URLObject urlInMemory = urlMap.get(id);
        ctx.put("object", urlInMemory);
        String internalURL = urlInMemory.getInternalUrl();
        /*todo
         * add param with document id
         * */
        WebClient client = WebClient.create(vertx);
        client
                // .getAbs("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                .get(8001, "localhost", "/getDocument")
                //auth header
                //.putHeader(HttpHeaders.AUTHORIZATION.toString(),"Basic "+base64Auth)
                .as(BodyCodec.buffer())
                //  .addQueryParam("url_id", id)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        Buffer body = response.body();
                        //decrease requests by -1
                        urlInMemory.setRequests(urlInMemory.getRequests() - 1);
                        ctx.response().putHeader("Content-type", response.getHeader("Content-type")).end(Buffer.buffer(body.getBytes()));
                        ctx.put("success",true);
                        ctx.next();
                    } else {
                        ctx.response().setStatusCode(400).putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily("Bad request"));
                    }
                });
    }

    private void finalCheck(RoutingContext ctx) {
        URLObject urlInMemory = ctx.get("object");
        Boolean success = ctx.get("success");

        if (!checkRequestsCount(urlInMemory)&& success) {
            urlMap.remove(urlInMemory.getExternalUUID().toString());
        }
    }

    public static void main(String[] args) {

        VerticleMain vm = new VerticleMain();
        vm.run();
    }

}
