package ui;

import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.NotificationHandler;

public class Repl implements NotificationHandler {

    @Override
    public HandlerResult handleNotification(Notification notification, Object attachment) {
        return null;
    }
}
