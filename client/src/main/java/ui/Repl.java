package ui;

import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.NotificationHandler;

import java.util.Scanner;

import static ui.EscapeSequences.*;

public class Repl implements NotificationHandler {

    private final ChessClient client;

    public Repl(String serverUrl) {
        client = new ChessClient(serverUrl);
    }

    public void run() {
        System.out.println("Welcome to the Chess Game");
        System.out.print(SET_TEXT_COLOR_BLUE + client.help());

        Scanner scanner = new Scanner(System.in);
        var result = "";
        while (!result.equals("quit")) {
            printPrompt();
            String line = scanner.nextLine();
            try {
                result = client.eval(line);
                System.out.print(SET_TEXT_COLOR_BLUE + result);
            }
            catch (Throwable e) {
                System.out.print("Unable to execute that command");
            }
        }
        System.out.println();
    }

    private void printPrompt() {
        System.out.print("\n" + RESET_TEXT_COLOR + ">>> ");
    }

    @Override
    public HandlerResult handleNotification(Notification notification, Object attachment) {
        return null;
    }
}
