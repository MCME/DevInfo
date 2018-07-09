package com.mcmiddleearth.devinfo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class HttpServer {

    final HashMap<String, Player> registeredUsers = new HashMap<>();

    private void send403(HttpExchange exchange) throws IOException {
        String response = "Invalid login";
        exchange.sendResponseHeaders(403, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void send500(HttpExchange exchange) throws IOException {
        String response = "Parameter parse error";
        exchange.sendResponseHeaders(500, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void writeLines(HttpExchange exchange, int lineCount) throws IOException {
        Scanner s = new Scanner(new File("logs/latest.log"));
        Deque<String> lines = new LinkedList<>();
        while(s.hasNextLine()) {
            lines.addFirst(s.nextLine());
            if(lines.size() > lineCount) {
                lines.removeLast();
            }
        }
        int length = 0;
        OutputStream out = exchange.getResponseBody();
        while(lines.size() > 0){
            String ln = lines.removeFirst();
            length += ln.getBytes().length;
            out.write(ln.getBytes());
        }
        exchange.sendResponseHeaders(200, length);
        out.close();
    }

    public void start() throws IOException {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String[] args = exchange.getRequestURI().getPath().split("/");
                Player user = registeredUsers.get(args[0]);

                if(user == null || !user.isOnline()) {
                    send403(exchange);
                    return;
                }

                if(args.length == 2) {
                    try {
                        int lines = Integer.parseInt(args[1]);
                        writeLines(exchange, lines);
                    } catch(NumberFormatException ex) {
                        send500(exchange);
                        return;
                    }
                }
                writeLines(exchange, 75);
            }
        });
        server.setExecutor(null); // creates a default executor
        server.start();
    }
}
