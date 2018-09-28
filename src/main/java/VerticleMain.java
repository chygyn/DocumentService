import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
    private long timerId;

    private VerticleMain() {
        this.vertx = Vertx.vertx();

    }

    public void run() {
        urlMap = new HashMap<String, URLObject>();
        init();
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>POST /createUrl</h1>");
        });
        router.post("/documents/:configuration").handler(BodyHandler.create()).handler(this::createURL);
        router.get("/documents/:configuration/:documentId").handler(this::checkURL).blockingHandler(this::getDocument).handler(this::finalCheck);

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router::accept).listen(8080);
    }

    /*
    * Prepare service to work.
    * */
    private void init(){
        encodeBase64access();
        setTimerCleaner();
    }
    /*
    * Check life time of each object in map. If !checkUrlTime -> remove object from map.
    * */
    private void setTimerCleaner(){
        String s= getProperty("timerDelay");

            Integer days = Integer.valueOf(s);
        long delay = days*24*3600*1000;
       // String timerUnit = getProperty("timerUnit");
        timerId = vertx.setPeriodic(delay, new Handler<Long>() {
            @Override
            public void handle(Long aLong) {
                for (Map.Entry<String, URLObject> e:urlMap.entrySet()){
                    URLObject currentObj = e.getValue();
                    if (!checkUrlTime(currentObj))
                        urlMap.remove(currentObj);
                }
            }
        });
    }
    /*
    * Get any property by @name from config.properties.
    * @name - String
    * */
    private String getProperty(String name) {
        String result=null;
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
            result = prop.getProperty(name);
            System.out.println("get property from config: " + name + " " + result);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null && result != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /*
    * Set any property by @name and @value into config.properties.
    * @name - String
    * @value - String
    * */
    private void setProperty(String name, String value) {
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
            prop.setProperty(name,value);
            System.out.println("set property to config: " + name + " " + value);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null ) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /*
    * Convert login and password from config.properties to base64.
    * */
    private void encodeBase64access() {
        /*
         * Authorisation part. Read config.properties for user and password values
         * */
        String user = getProperty("user");
        String password = getProperty("password");
        String base64Auth = Base64.getEncoder().encodeToString(new StringBuilder(user).append(":").append(password).toString().getBytes());
        setProperty("base64access",base64Auth);
        System.out.println("Autehtication encoded: " + base64Auth);
    }

    /*
    * Check available requests count for @urlObj
    * @urlObj - URLObject
    * */
    private Boolean checkRequestsCount(URLObject urlObj) {
        if (urlObj.getRequests() > 0)
            return true;
        else
            return false;
    }

    /*
     * Check life time for @urlObj
     * @urlObj - URLObject
     * */
    private Boolean checkUrlTime(URLObject urlObj) {
        long time = new Date().getTime();
        if (urlObj.getCreatedTime() + urlObj.getLifeTime() >= time)
            return true;
        else
            return false;
    }

    /*
     * Create new entry in urlMap for incoming json object.
     * @ctx - RoutingContext
     * */
    private void createURL(RoutingContext ctx) {
        URLObject newUrl = URLObject.parseJson(ctx.getBodyAsJson());
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (urlMap.containsKey(id));
        newUrl.setExternalUUID(id);
        urlMap.put(id.toString(), newUrl);
        System.out.println("updated urlMap ");
        urlMap.forEach((uuid, urlObject) -> System.out.println("UUID: " + uuid.toString() + " Object: " + urlObject.toString()));
        String urlObjectJSON = newUrl.createJSON().toString();
        ctx.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
                .end(urlObjectJSON);
    }

    /*
    * Check URL before send document to user
    * @ctx - RoutingContext
    * */
    private void checkURL(RoutingContext ctx) {
        String id = ctx.request().getParam("documentId");
        URLObject urlInMemory = urlMap.get(id);
        if (urlInMemory != null) {
            long time = new Date().getTime();
            if (checkRequestsCount(urlInMemory) & checkUrlTime(urlInMemory)) {
                ctx.next();
            } else {
                ctx.response().putHeader("content-type", "text/html").setStatusCode(404)
                        .end("<h1>Invalid URL. Required new URL.</h1>");
            }
        } else {
            ctx.response().putHeader("content-type", "text/html").setStatusCode(404)
                    .end("<h1>Invalid URL. Required new URL.</h1>");
        }
    }

    /*
     * Send document to user from content server.
     * @ctx - RoutingContext
     * */
    private void getDocument(RoutingContext ctx) {
        //retrieve object by id
        String id = ctx.request().getParam("documentId");
        URLObject urlInMemory = urlMap.get(id);
        ctx.put("object", urlInMemory);
        String internalURL = urlInMemory.getInternalUrl();
        WebClient client = WebClient.create(vertx);
        client
                //.getAbs("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
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
                        ctx.put("success", true);
                        ctx.next();
                    } else {
                        ctx.response().setStatusCode(400).putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encode("Bad request"));
                    }
                });
    }
    /*
    * Check object in @ctx for available requests. Remove object from urlMap if !checkRequestsCount
    * @ctx - RoutingContext
    * */
    private void finalCheck(RoutingContext ctx) {
        URLObject urlInMemory = ctx.get("object");
        Boolean success = ctx.get("success");

        if (!checkRequestsCount(urlInMemory) && success) {
            urlMap.remove(urlInMemory.getExternalUUID().toString());
        }
    }

    public static void main(String[] args) {
        VerticleMain vm = new VerticleMain();
        vm.run();
    }

}
