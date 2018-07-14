package com.mcmiddleearth.devinfo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

public class HttpServer {

    final HashSet<String> deployPasswords = new HashSet<>();

    final HashMap<String, Player> registeredUsers = new HashMap<>();

    final int port;

    public HttpServer(int port) {
        this.port = port;
    }

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
        String response = "Requested function not found. This could be in error.";
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

    private void handleConfigGetRequest(HttpExchange exchange, String[] args) throws IOException {
        File target = new File("plugins/" + String.join("/", args));

        if(target.isFile()) {
            byte[] buffer = new byte[1024];
            FileInputStream fis = new FileInputStream(target);
            OutputStream out = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, (int) target.length());
            try {
                int byteRead = 0;
                while ((byteRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, byteRead);

                }
                out.flush();
            } catch (Exception excp) {
                excp.printStackTrace();
            } finally {
                out.close();
                fis.close();
            }
        } else {
            StringBuilder sb = new StringBuilder();
            Arrays.stream(Objects.requireNonNull(target.listFiles())).map(File::getName).forEach(e -> {
                sb.append(e);
                sb.append("\n");
            });
            OutputStream out = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, sb.toString().getBytes().length);
            out.write(sb.toString().getBytes());
            out.close();
        }

    }

    private void handleRebootRequest(HttpExchange exchange, String[] args) throws IOException {
        String conf = "Sending reboot signal";
        OutputStream out = exchange.getResponseBody();
        exchange.sendResponseHeaders(200, conf.getBytes().length);
        out.write(conf.getBytes());
        out.close();
        Bukkit.shutdown();
    }

    public void start() throws IOException {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(this.port), 0);
        System.out.println("Info server started on port: " + this.port);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String[] args = exchange.getRequestURI().getPath().split("/");
                Player user = registeredUsers.get(args[1]);

                if(!deployPasswords.contains(args[1]) && (user == null || !user.isOnline())) {
                    send403(exchange);
                    return;
                }

                String[] argz = Arrays.copyOfRange(args, 3, args.length);

                if(args[2].equalsIgnoreCase("logs")) {
                    handleLogRequest(exchange, argz);
                    return;
                }
                if(args[2].equalsIgnoreCase("config")) {
                    handleConfigGetRequest(exchange, argz);
                }
                if(args[2].equalsIgnoreCase("reboot")) {
                    handleRebootRequest(exchange, argz);
                }
                send404(exchange);
            }
        });
        server.setExecutor(null); // creates a default executor
        server.start();
    }
}
