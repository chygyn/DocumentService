import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    public ObjectNode createJSON (){
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node =  mapper.createObjectNode();
        node.put("internalURL",this.internalUrl);
        node.put("externalUUID",this.externalUUID.toString());
        node.put("createdTime",this.createdTime);
        node.put("lifeTime",this.lifeTime);
        node.put("requests",this.requests);
        System.out.println("URLObject JSON: "+node.toString());
        return node;
    }
}
