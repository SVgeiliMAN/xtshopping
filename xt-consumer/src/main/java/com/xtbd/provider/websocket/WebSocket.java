package com.xtbd.provider.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@ServerEndpoint("/websocket")
@Component
@Slf4j
public class WebSocket {
    /**
     * 存放所有在线的客户端
     * 只在单机版本有效，多实例下数字不正确
     */
    private static Map<String, Session> clients = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        //将新用户存入在线的组
        clients.put(session.getId(), session);
        int onlineCount = clients.size();
        this.sendAll(Integer.toString(onlineCount));
    }

    /**
     * 客户端关闭
     * @param session session
     */
    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        log.info("有用户断开了, id为:{}",sessionId );
        //将掉线的用户移除在线的组里
        session = clients.get(sessionId);
        if (session!=null){
            clients.remove(session.getId());
        }
        int onlineCount = clients.size();
        this.sendAll(Integer.toString(onlineCount));
    }

    @OnError
    public void onError(Throwable throwable) {
        log.error(throwable.getMessage());
    }


    @OnMessage
    public void onMessage(String message) {
        log.info("服务端收到客户端发来的消息: {}", message);
    }

    private void sendAll(String message) {
        for (Map.Entry<String, Session> sessionEntry : clients.entrySet()) {
            Session session = sessionEntry.getValue();
            if (!session.isOpen()){
                return;
            }
            session.getAsyncRemote().sendText(message);
        }
    }
    private void sendAll(Map message) {
        for (Map.Entry<String, Session> sessionEntry : clients.entrySet()) {
            sessionEntry.getValue().getAsyncRemote().sendObject(message);
        }
    }
}
