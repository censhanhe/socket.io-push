package com.yy.httpproxy.emitter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.apache.commons.codec.binary.Base64;
import org.redisson.Redisson;
import org.redisson.core.MessageListener;
import org.redisson.core.RTopic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by xuduo on 11/13/15.
 */
public class PacketServer {

    private Emitter emitter;
    private Map<String, PacketHandler> handlerMap = new HashMap<>();
    private Redisson redisson = Redisson.create();

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type",
            defaultImpl = PackProxy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PackProxy {
        public String path;
        public String data;
        public String sequenceId;
        public String uid;
        public String pushId;
    }

    public PacketServer() {
        emitter = new Emitter(redisson);
        RTopic<PackProxy> topic = redisson.getTopic("packetProxy", new JsonJacksonCodecWithClass(PackProxy.class));
        topic.addListener(new MessageListener<PackProxy>() {

            public void onMessage(String channel, PackProxy message) {
                PacketHandler handler = handlerMap.get(message.path);
                if (handler != null) {
                    handler.handle(message.uid, message.pushId, message.sequenceId, message.path, null, Base64.decodeBase64(message.data));
                }
            }
        });
    }

    public void addHandler(String path, PacketHandler handler) {
        handler.setEmitter(emitter);
        handlerMap.put(path, handler);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        PacketServer server = new PacketServer();

        server.addHandler("/addDot", new PacketHandler() {
            @Override
            void handle(String uid, String pushId, String sequenceId, String path, Map<String, String> headers, byte[] body) {
                broadcast("/addDot", body);
                reply(sequenceId, pushId, headers, body);
            }
        });

        server.addHandler("/endLine", new PacketHandler() {
            @Override
            void handle(String uid, String pushId, String sequenceId, String path, Map<String, String> headers, byte[] body) {
                broadcast("/endLine", body);
            }
        });

        server.addHandler("/clear", new PacketHandler() {
            @Override
            void handle(String uid, String pushId, String sequenceId, String path, Map<String, String> headers, byte[] body) {
                broadcast("/clear", null);
            }
        });


        Thread.sleep(100000L);
    }
}