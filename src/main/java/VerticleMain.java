import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class VerticleMain {

    private HashMap<String, URLObject> urlMap;
    private Vertx vertx;

    private VerticleMain() {
        this.vertx = Vertx.vertx();

    }

    public void run() {
        urlMap = new HashMap<String, URLObject>();
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>POST /createUrl</h1>");
        });
        router.route(HttpMethod.POST, "/documents/:configuration*").handler(BodyHandler.create());
        router.post("/documents/:configuration").handler(this::createURL);
        router.get("/documents/:configuration/:documentId").handler(this::checkURL).blockingHandler(this::getDocument);

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
        URLObject newUrl = Json.decodeValue(ctx.getBodyAsString(), URLObject.class);
        UUID id;

        do {
            id = UUID.randomUUID();
        } while (urlMap.containsKey(id));
        newUrl.setExternalUUID(id);
        urlMap.put(id.toString(), newUrl);
        System.out.println("updated urlMap ");
        urlMap.forEach((uuid, urlObject) -> System.out.println("UUID: "+uuid.toString()+" Object: "+urlObject.toString()));
        ctx.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(newUrl));
    }

    private void checkURL(RoutingContext ctx) {

        String id = ctx.request().getParam("documentId");
        URLObject urlInMemory = urlMap.get(id);
        if (urlInMemory == null)
            ctx.response().putHeader("content-type", "text/html")
                    .end("<h1>Invalid URL. Required new URL.</h1>");
        long time = new Date().getTime();
        if (urlInMemory.getRequests() > 0 & urlInMemory.getCreatedTime() + urlInMemory.getLifeTime() <= time) {
            ctx.next();
        } else {
            ctx.response().putHeader("content-type", "text/html")
                    .end("<h1>Invalid URL. Required new URL.</h1>");
        }
    }

    private void getDocument(RoutingContext ctx) {
        //retrieve object by id
        String id = ctx.request().getParam("documentId");
        URLObject urlInMemory = urlMap.get(id);
        /*todo
        * add param with document id
        * */
        WebClient client = WebClient.create(vertx);
        client
               // .getAbs("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                .get(8001, "localhost", "/getDocument")
                .as(BodyCodec.buffer())
              //  .addQueryParam("url_id", id)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        Buffer body = response.body();
                        ctx.response().putHeader("Content-type","application/pdf; charset=UTF-8").end(Buffer.buffer(body.getBytes()));

                    } else {
                        ctx.response().setStatusCode(400).putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily("Bad request"));
                    }
                });
    }

    public static void main(String[] args) {

        VerticleMain vm = new VerticleMain();
        vm.run();
    }

}
