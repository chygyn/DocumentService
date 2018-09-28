import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.Date;
import java.util.UUID;

public class URLObject {

    String internalUrl;
    UUID externalUUID;
    Integer lifeTime;
    long createdTime;
    Integer requests;


    public URLObject(String url, Integer lt, long ct, Integer r){
        this.internalUrl=url;
        this.lifeTime=lt;
        this.createdTime=ct;
        this.requests=r;
    }

    public URLObject(){

    }

    public String getInternalUrl() {
        return internalUrl;
    }

    public void setInternalUrl(String url) {
        this.internalUrl = url;
    }

    public Integer getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(Integer lifeTime) {
        this.lifeTime = lifeTime;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public Integer getRequests() {
        return requests;
    }

    public void setRequests(Integer requests) {
        this.requests = requests;
    }

    public UUID getExternalUUID() {
        return externalUUID;
    }

    public void setExternalUUID(UUID externalURL) {
        this.externalUUID = externalURL;
    }

    @Override
    public String toString(){
        return "urlObj: internalUrl "+internalUrl+", UUID "+externalUUID+", requests "+requests+", lifeTime "+lifeTime+", createdTime "+createdTime;
    }

    public JsonObject createJSON (){
        JsonObject result = new JsonObject();
        result.put("internalURL",this.internalUrl)
                .put("externalUUID",this.externalUUID.toString())
                .put("createdTime",this.createdTime)
                .put("lifeTime",this.lifeTime)
                .put("requests",this.requests);
        return result;
    }

    public static URLObject parseJson (JsonObject json){
        URLObject result = new URLObject();
        result.setInternalUrl(json.getString("internalURL"));
        result.setCreatedTime(json.getLong("createdTime"));
        result.setLifeTime(json.getInteger("lifeTime"));
        result.setRequests(json.getInteger("requests"));
        return result;
    }
}
