package com.farmlink;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/** API controller: translates HTTP requests into service calls, not SQL. */
public final class ApiHandler implements HttpHandler {
    private final AuthService auth; private final MarketplaceService marketplace;private final TradeService trades;private final AdminService admin;
    public ApiHandler(AuthService auth,MarketplaceService marketplace,TradeService trades,AdminService admin){this.auth=auth;this.marketplace=marketplace;this.trades=trades;this.admin=admin;}
    @Override public void handle(HttpExchange x)throws IOException{
        try {doHandle(x);}catch(ApiException e){reply(x,e.status(),Inputs.map("error",e.getMessage()));}
        catch(SQLException e){System.err.println("Database failure: "+e);reply(x,500,Inputs.map("error","A database error occurred"));}
        catch(Exception e){System.err.println("Unexpected API failure: "+e);e.printStackTrace();reply(x,500,Inputs.map("error","Unexpected server error"));}
        finally{x.close();}
    }
    private void doHandle(HttpExchange x)throws Exception{
        String method=x.getRequestMethod();String path=x.getRequestURI().getPath();
        x.getResponseHeaders().set("Cache-Control","no-store");
        if(method.equals("OPTIONS"))throw new ApiException(405,"OPTIONS is not enabled");
        if(!Set.of("GET","POST","PUT","PATCH","DELETE").contains(method))throw new ApiException(405,"Unsupported method");
        if(!method.equals("GET"))verifyOrigin(x);
        String token=cookie(x);Account current=auth.currentUser(token);
        Map<String,String> query=parameters(x.getRequestURI().getRawQuery());
        if(path.equals("/api/health")&&method.equals("GET")){reply(x,200,Inputs.map("status","ok","database","HSQLDB embedded"));return;}
        if(path.equals("/api/auth/register")&&method.equals("POST")){
            Account a=auth.register(body(x));setCookie(x,auth.newSession(a));reply(x,201,Inputs.map("user",a.publicView()));return;
        }
        if(path.equals("/api/auth/login")&&method.equals("POST")){
            Map<String,Object> d=body(x);Account a=auth.authenticate(Inputs.str(d,"nic"),Inputs.str(d,"password"));
            setCookie(x,auth.newSession(a));reply(x,200,Inputs.map("user",a.publicView()));return;
        }
        if(path.equals("/api/auth/me")&&method.equals("GET")){reply(x,200,Inputs.map("user",current==null?null:current.publicView()));return;}
        if(path.equals("/api/auth/logout")&&method.equals("POST")){
            auth.signOut(token);x.getResponseHeaders().add("Set-Cookie","FL_SESSION=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0");reply(x,200,Inputs.map("ok",true));return;
        }
        // Privileged routes: role is checked by AdminService against the authenticated account.
        if(path.equals("/api/admin/overview")&&method.equals("GET")){reply(x,200,Inputs.map("overview",admin.overview(required(current))));return;}
        if(path.equals("/api/admin/users")&&method.equals("GET")){reply(x,200,Inputs.map("users",admin.users(required(current))));return;}
        if(path.equals("/api/admin/harvests")&&method.equals("GET")){reply(x,200,Inputs.map("harvests",admin.harvests(required(current))));return;}
        if(path.matches("/api/admin/users/\\d+/delete-impact")&&method.equals("GET")) {
            int userId=Inputs.id(path.substring("/api/admin/users/".length(),path.length()-"/delete-impact".length()));
            reply(x,200,Inputs.map("impact",admin.deleteImpact(required(current),userId)));return;
        }
        if(path.matches("/api/admin/users/\\d+")&&method.equals("DELETE")) {
            int userId=Inputs.id(path.substring("/api/admin/users/".length()));
            admin.deleteUser(required(current),userId);reply(x,200,Inputs.map("ok",true));return;
        }
        if(path.matches("/api/admin/harvests/\\d+/unlist")&&method.equals("POST")){
            int harvestId=Inputs.id(path.substring("/api/admin/harvests/".length(),path.length()-"/unlist".length()));
            admin.unlist(required(current),harvestId);reply(x,200,Inputs.map("ok",true));return;
        }
        if(path.equals("/api/harvests")&&method.equals("GET")){
            // Viewing someone else's inactive harvests is not allowed.
            if(query.containsKey("farmerId") && (current==null || !query.get("farmerId").equals(String.valueOf(current.id())) || !current.role().equals("FARMER")))
                throw new ApiException(403,"You can only view your own private listings");
            reply(x,200,Inputs.map("harvests",marketplace.browse(query)));return;
        }
        if(path.equals("/api/harvests")&&method.equals("POST")){
            reply(x,201,Inputs.map("harvest",marketplace.create(required(current),body(x))));return;
        }
        if(path.matches("/api/harvests/\\d+")){
            int id=Inputs.id(path.substring("/api/harvests/".length()));
            if(method.equals("GET")){Map<String,Object> h=marketplace.detail(id);
                if(!h.get("status").equals("active") && (current==null||current.id()!=((Number)h.get("farmerId")).intValue()))throw new ApiException(404,"Harvest not found");
                reply(x,200,Inputs.map("harvest",h));return;
            }
            if(method.equals("PUT")){reply(x,200,Inputs.map("harvest",marketplace.update(required(current),id,body(x))));return;}
            if(method.equals("DELETE")){marketplace.unlist(required(current),id);reply(x,200,Inputs.map("ok",true));return;}
        }
        if(path.equals("/api/profile")){
            Account a=required(current);
            if(method.equals("GET")){reply(x,200,Inputs.map("user",a.publicView()));return;}
            if(method.equals("PUT")){reply(x,200,Inputs.map("user",auth.updateProfile(a,body(x)).publicView()));return;}
        }
        if(path.equals("/api/saved" )&&method.equals("GET")){reply(x,200,Inputs.map("harvests",trades.saved(required(current))));return;}
        if(path.matches("/api/saved/\\d+")){
            int id=Inputs.id(path.substring("/api/saved/".length()));Account a=required(current);
            if(method.equals("POST")){trades.save(a,id);reply(x,200,Inputs.map("ok",true));return;}
            if(method.equals("DELETE")){trades.unsave(a,id);reply(x,200,Inputs.map("ok",true));return;}
        }
        if(path.equals("/api/requests")){
            Account a=required(current);
            if(method.equals("GET")){reply(x,200,Inputs.map("requests",trades.requests(a)));return;}
            if(method.equals("POST")){Map<String,Object> d=body(x);
                reply(x,201,Inputs.map("request",trades.request(a,Inputs.idValue(d.get("harvestId")),Inputs.number(d,"quantity",0.01,10_000_000),Inputs.str(d,"message"))));return;
            }
        }
        if(path.matches("/api/requests/\\d+")){
            int id=Inputs.id(path.substring("/api/requests/".length()));Account a=required(current);
            if(method.equals("GET")){reply(x,200,Inputs.map("request",trades.requestDetails(a,id)));return;}
            if(method.equals("PATCH")){reply(x,200,Inputs.map("request",trades.changeStatus(a,id,Inputs.str(body(x),"status"))));return;}
        }
        if(path.matches("/api/requests/\\d+/messages")){
            String raw=path.substring("/api/requests/".length(),path.length()-"/messages".length());int id=Inputs.id(raw);Account a=required(current);
            if(method.equals("GET")){reply(x,200,Inputs.map("messages",trades.messages(a,id)));return;}
            if(method.equals("POST")){reply(x,201,Inputs.map("message",trades.message(a,id,Inputs.str(body(x),"text"))));return;}
        }
        if(path.equals("/api/dashboard")&&method.equals("GET")){
            Account a=required(current);List<Map<String,Object>> reqs=trades.requests(a);
            long pending=reqs.stream().filter(r->"pending".equals(r.get("status"))).count();
            long completed=reqs.stream().filter(r->"completed".equals(r.get("status"))).count();
            int mainCount=a.role().equals("FARMER")?(int)marketplace.browse(Map.of("farmerId",String.valueOf(a.id()))).stream().filter(h->"active".equals(h.get("status"))).count():trades.saved(a).size();
            reply(x,200,Inputs.map("user",a.publicView(),"totalRequests",reqs.size(),"pending",pending,"completed",completed,
                "mainCount",mainCount,"requests",reqs,"harvests",a.role().equals("FARMER")?marketplace.browse(Map.of("farmerId",String.valueOf(a.id()))):marketplace.browse(Map.of())));return;
        }
        throw new ApiException(404,"API route not found");
    }
    private static Account required(Account a){if(a==null)throw new ApiException(401,"Please log in to continue");return a;}
    private static Map<String,Object> body(HttpExchange x)throws IOException{
        String contentType=x.getRequestHeaders().getFirst("Content-Type");
        if(contentType==null||!contentType.toLowerCase(Locale.ROOT).startsWith("application/json"))throw new ApiException(415,"Content-Type application/json required");
        byte[] bytes=x.getRequestBody().readNBytes(1_048_577);if(bytes.length>1_048_576)throw new ApiException(413,"Request is too large");
        return Json.object(new String(bytes,StandardCharsets.UTF_8));
    }
    private static void verifyOrigin(HttpExchange x){
        String origin=x.getRequestHeaders().getFirst("Origin");if(origin==null)return; // nonbrowser clients
        try {URI u=URI.create(origin);String host=x.getRequestHeaders().getFirst("Host");
            if(host!=null && (u.getRawAuthority().equalsIgnoreCase(host)))return;
        }catch(Exception ignored){}
        throw new ApiException(403,"Cross-origin request denied");
    }
    private static Map<String,String> parameters(String q){Map<String,String> m=new HashMap<>();if(q==null)return m;
        for(String pair:q.split("&")){if(pair.isEmpty())continue;String[] p=pair.split("=",2);
            m.put(URLDecoder.decode(p[0],StandardCharsets.UTF_8),URLDecoder.decode(p.length==2?p[1]:"",StandardCharsets.UTF_8));}
        return m;
    }
    private static String cookie(HttpExchange x){String header=x.getRequestHeaders().getFirst("Cookie");if(header==null)return null;
        for(String item:header.split(";")){String[] p=item.trim().split("=",2);if(p.length==2 && p[0].equals("FL_SESSION"))return p[1];}return null;
    }
    private static void setCookie(HttpExchange x,String token){x.getResponseHeaders().add("Set-Cookie","FL_SESSION="+token+"; HttpOnly; SameSite=Lax; Path=/; Max-Age=604800");}
    private static void reply(HttpExchange x,int status,Object object)throws IOException{
        byte[] bytes=Json.stringify(object).getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
        x.getResponseHeaders().set("X-Content-Type-Options","nosniff");
        x.sendResponseHeaders(status,bytes.length);try(OutputStream o=x.getResponseBody()){o.write(bytes);}
    }
}
