package com.farmlink;
import java.sql.*;
import java.util.*;
public final class DefaultTradeService implements TradeService {
    private final TradeRepository trades;private final HarvestRepository harvests;
    public DefaultTradeService(TradeRepository trades,HarvestRepository harvests){this.trades=trades;this.harvests=harvests;}
    private void buyer(Account u){Inputs.allowed(u!=null&&u.role().equals("BUYER"),"Buyer account required");}
    private Map<String,Object> authorized(Account u,int id)throws SQLException{
        if(u==null)throw new ApiException(401,"Please log in");
        Map<String,Object> r=trades.findRequest(id);
        if(r==null)throw new ApiException(404,"Request not found");
        Inputs.allowed(((Number)r.get("buyerId")).intValue()==u.id() || ((Number)r.get("farmerId")).intValue()==u.id(),"This conversation is private");return r;
    }
    @Override public List<Map<String,Object>> requests(Account u)throws SQLException{return trades.listRequests(u);}
    @Override public Map<String,Object> request(Account u,int harvestId,double quantity,String message)throws SQLException{
        buyer(u);Harvest h=harvests.find(harvestId);if(h==null||!h.status().equals("active"))throw new ApiException(404,"Available harvest not found");
        if(quantity<=0||!Double.isFinite(quantity)||quantity>h.quantity())throw new ApiException(400,"Requested quantity exceeds available stock");
        if(message.length()>2000)throw new ApiException(400,"Message too long");
        return trades.createRequest(u.id(),h,quantity,message);
    }
    @Override public Map<String,Object> requestDetails(Account u,int id)throws SQLException{return authorized(u,id);}
    @Override public Map<String,Object> changeStatus(Account u,int id,String next)throws SQLException{
        Map<String,Object> r=authorized(u,id);String current=(String)r.get("status");
        boolean farmer=((Number)r.get("farmerId")).intValue()==u.id();
        boolean allowed=(farmer && ((current.equals("pending") && (next.equals("accepted")||next.equals("declined"))) || (current.equals("accepted")&&next.equals("completed"))))
                     || (!farmer && (current.equals("pending")||current.equals("accepted")) && next.equals("cancelled"));
        if(!allowed)throw new ApiException(409,"Invalid status transition for your account");
        trades.status(id,next);return trades.findRequest(id);
    }
    @Override public List<Map<String,Object>> messages(Account u,int id)throws SQLException{authorized(u,id);return trades.messages(id);}
    @Override public Map<String,Object> message(Account u,int id,String body)throws SQLException{
        Map<String,Object> r=authorized(u,id);Inputs.require(Inputs.map("message",body),"message",1,2000);
        if(Set.of("cancelled","declined").contains(r.get("status")))throw new ApiException(409,"This request is closed");
        return trades.send(id,u.id(),body.trim());
    }
    @Override public List<Map<String,Object>> saved(Account u)throws SQLException{
        buyer(u);List<Map<String,Object>> out=new ArrayList<>();for(Harvest h:trades.saved(u.id()))out.add(h.publicView());return out;
    }
    @Override public void save(Account u,int id)throws SQLException{buyer(u);Harvest h=harvests.find(id);if(h==null||!h.status().equals("active"))throw new ApiException(404,"Harvest unavailable");trades.save(u.id(),id);}
    @Override public void unsave(Account u,int id)throws SQLException{buyer(u);trades.unsave(u.id(),id);}
}
