package ui;

import websocket.messages.ServerMessage;

public interface ServerMessageObserver {
    void onServerMessage(ServerMessage message);
}
