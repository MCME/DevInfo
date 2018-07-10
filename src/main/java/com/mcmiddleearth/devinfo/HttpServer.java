package com.mcmiddleearth.devinfo;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

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

    private void send404(HttpExchange exchange) throws IOException {
        String response = "Requested function not found";
        exchange.sendResponseHeaders(404, response.getBytes().length);
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

        StringBuilder sb = new StringBuilder();
        while(lines.size() > 0){
            sb.append(lines.removeFirst());
            sb.append("\n");
        }
        OutputStream out = exchange.getResponseBody();
        exchange.sendResponseHeaders(200, sb.toString().getBytes().length);
        out.write(sb.toString().getBytes());
        out.close();
    }

    private void handleLogRequest(HttpExchange exchange, String[] args) throws IOException {
        if(args.length == 1) {
            try {
                int lines = Integer.parseInt(args[0]);
                writeLines(exchange, lines);
            } catch(NumberFormatException ex) {
                send500(exchange);
                return;
            }
        }
        writeLines(exchange, 75);
    }

    private void handleConfigRequest(HttpExchange exchange, String[] args) throws IOException {
        StringBuilder sb = new StringBuilder();
        File target = new File("plugins/" + String.join("/", args));
        if(target.isFile()) {
            Scanner s = new Scanner(target);
            while(s.hasNextLine()) {
                sb.append(s.nextLine());
                sb.append("\n");
            }
        } else {
            Arrays.stream(Objects.requireNonNull(target.listFiles())).map(File::getName).forEach(e -> {
                sb.append(e);
                sb.append("\n");
            });
        }
        OutputStream out = exchange.getResponseBody();
        exchange.sendResponseHeaders(200, sb.toString().getBytes().length);
        out.write(sb.toString().getBytes());
        out.close();
    }

    private void handlePluginRequest(HttpExchange exchange, String[] args) throws IOException {

    }

    public void start() throws IOException {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String[] args = exchange.getRequestURI().getPath().split("/");
                Player user = registeredUsers.get(args[1]);

                if(user == null || !user.isOnline()) {
                    send403(exchange);
                    return;
                }

                String[] argz = new String[args.length - 3];
                if(argz.length != 0){
                    System.arraycopy(args, 2, argz, 0, argz.length - 3);
                }

                if(args[2].equalsIgnoreCase("logs")) {
                    handleLogRequest(exchange, argz);
                    return;
                }
                if(args[2].equalsIgnoreCase("config")) {
                    handleConfigRequest(exchange, argz);
                }
//                if(args[2].equalsIgnoreCase("plugin")) {
//                    handlePluginRequest(exchange, argz);
//                }
                send404(exchange);
            }
        });
//        server.setExecutor(null); // creates a default executor
        server.start();
    }
}
