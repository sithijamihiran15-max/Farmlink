package com.farmlink;
import com.sun.net.httpserver.*;
import java.nio.file.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Application composition root: configures dependencies and starts the web server. */
public final class Main {
    public static void main(String[] args) throws Exception {
        Path root=Paths.get("").toAbsolutePath();Path web=root.resolve("web");
        if(!Files.isDirectory(web))throw new IllegalStateException("Run from FarmLink project root (./run.sh)");
        Database db=new Database(root.resolve("data"));
        AccountRepository users=new JdbcAccountRepository(db);
        HarvestRepository harvests=new JdbcHarvestRepository(db);
        TradeRepository tradeRepo=new JdbcTradeRepository(db);
        if(!"false".equalsIgnoreCase(System.getenv().getOrDefault("FARMLINK_SEED_DEMO","false")))DemoSeeder.seed(users,harvests,tradeRepo);
        AuthService auth=new DefaultAuthService(users);
        MarketplaceService market=new DefaultMarketplaceService(harvests);
        TradeService trade=new DefaultTradeService(tradeRepo,harvests);
        AdminService admin=new DefaultAdminService(new JdbcAdminRepository(db));
        String configuredPort=System.getenv("FARMLINK_PORT");
        int preferredPort;
        try { preferredPort=Integer.parseInt(configuredPort==null?"8080":configuredPort); }
        catch(NumberFormatException ex){throw new IllegalArgumentException("FARMLINK_PORT must be a number from 1 to 65535.",ex);}
        if(preferredPort<1||preferredPort>65535)throw new IllegalArgumentException("FARMLINK_PORT must be from 1 to 65535.");
        String host=System.getenv().getOrDefault("FARMLINK_HOST","127.0.0.1");
        // If the default port is already taken, automatically try 8081 ... 8090.
        // An explicit FARMLINK_PORT is respected; do not silently change it.
        HttpServer server=null;
        int port=preferredPort;
        int lastPort=configuredPort==null?8090:preferredPort;
        for(int candidate=preferredPort;candidate<=lastPort;candidate++){
            try{
                server=HttpServer.create(new InetSocketAddress(host,candidate),0);
                port=candidate;
                break;
            }catch(java.net.BindException ex){
                if(candidate==lastPort)throw new java.io.IOException("Port "+candidate+" is already in use. Close the other program or set FARMLINK_PORT to a free port.",ex);
                System.out.println("Port "+candidate+" is busy; trying "+(candidate+1)+" ...");
            }
        }
        if(server==null)throw new java.io.IOException("Could not start FarmLink: no free port found.");
        server.createContext("/api/",new ApiHandler(auth,market,trade,admin));
        server.createContext("/",x->serveStatic(x,web));
        server.setExecutor(Executors.newFixedThreadPool(12));
        HttpServer runningServer=server;
        Runtime.getRuntime().addShutdownHook(new Thread(()->{runningServer.stop(0);db.shutdown();}));
        server.start();
        System.out.println();
        System.out.println("-----------------------------------------------------");
        System.out.println("FarmLink is running! Open this URL in your browser:");
        System.out.println("  http://"+host+":"+port+"/");
        System.out.println("Keep this window open. Press Ctrl+C to stop.");
        System.out.println("-----------------------------------------------------");
    }
    private static void serveStatic(HttpExchange x,Path root)throws IOException{
        try {
            if(!x.getRequestMethod().equals("GET")&&!x.getRequestMethod().equals("HEAD")){sendText(x,405,"Method not allowed");return;}
            String path=URLDecoder.decode(x.getRequestURI().getPath(),java.nio.charset.StandardCharsets.UTF_8);
            if(path.equals("/"))path="/index.html";
            Path file=root.resolve(path.substring(1)).normalize();
            if(!file.startsWith(root)||!Files.isRegularFile(file)||Files.isHidden(file)){sendText(x,404,"Page not found");return;}
            String name=file.getFileName().toString().toLowerCase(Locale.ROOT);
            String type=name.endsWith(".html")?"text/html":name.endsWith(".css")?"text/css":name.endsWith(".js")?"text/javascript":name.endsWith(".svg")?"image/svg+xml":name.endsWith(".png")?"image/png":name.endsWith(".jpg")||name.endsWith(".jpeg")?"image/jpeg":"application/octet-stream";
            byte[] bytes=Files.readAllBytes(file);
            x.getResponseHeaders().set("Content-Type",type+((type.startsWith("text/")||type.contains("svg"))?"; charset=utf-8":""));
            x.getResponseHeaders().set("X-Content-Type-Options","nosniff");
            x.getResponseHeaders().set("X-Frame-Options","DENY");
            x.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: blob:; connect-src 'self'; object-src 'none'; frame-ancestors 'none'");
            x.sendResponseHeaders(200,x.getRequestMethod().equals("HEAD")?-1:bytes.length);
            if(!x.getRequestMethod().equals("HEAD"))try(OutputStream out=x.getResponseBody()){out.write(bytes);}
        }catch(Exception e){System.err.println("Static error: "+e);sendText(x,404,"Page not found");}finally{x.close();}
    }
    private static void sendText(HttpExchange x,int code,String message)throws IOException{
        byte[] b=message.getBytes(java.nio.charset.StandardCharsets.UTF_8);x.sendResponseHeaders(code,b.length);
        try(OutputStream out=x.getResponseBody()){out.write(b);}
    }
}
