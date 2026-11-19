package com.farmlink;
import java.sql.*;
import java.util.*;
public final class JdbcTradeRepository implements TradeRepository {
    private final Database db;
    private static final String RSQL="SELECT q.*,h.title,h.price,h.unit,h.image,u1.first_name AS buyer_first,u1.last_name AS buyer_last,u2.first_name AS farmer_first,u2.last_name AS farmer_last FROM inquiries q JOIN harvests h ON h.id=q.harvest_id JOIN users u1 ON u1.id=q.buyer_id JOIN users u2 ON u2.id=q.farmer_id";
    public JdbcTradeRepository(Database db){this.db=db;}
    private Map<String,Object> request(ResultSet r)throws SQLException{return Inputs.map("id",r.getInt("id"),"harvestId",r.getInt("harvest_id"),"buyerId",r.getInt("buyer_id"),"farmerId",r.getInt("farmer_id"),"title",r.getString("title"),"price",r.getDouble("price"),"unit",r.getString("unit"),"image",r.getString("image"),"buyerName",r.getString("buyer_first")+" "+r.getString("buyer_last"),"farmerName",r.getString("farmer_first")+" "+r.getString("farmer_last"),"quantity",r.getDouble("quantity"),"status",r.getString("status"),"createdAt",r.getString("created_at"));}
    @Override public Map<String,Object> createRequest(int buyerId,Harvest h,double qty,String msg)throws SQLException{
        try(Connection c=db.connect()){
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement("INSERT INTO inquiries(harvest_id,buyer_id,farmer_id,quantity,status) VALUES(?,?,?,?,'pending')")){
                Database.bind(p,h.id(),buyerId,h.farmerId(),qty);p.executeUpdate();int id=Database.id(c);
                if(!msg.isBlank())try(PreparedStatement m=c.prepareStatement("INSERT INTO messages(inquiry_id,sender_id,body) VALUES(?,?,?)")){
                    Database.bind(m,id,buyerId,msg);m.executeUpdate();}
                c.commit();return findRequest(id);
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    @Override public List<Map<String,Object>> listRequests(Account u)throws SQLException{
        String field=u.role().equals("FARMER")?"q.farmer_id":"q.buyer_id";
        List<Map<String,Object>> out=new ArrayList<>();
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement(RSQL+" WHERE "+field+"=? ORDER BY q.id DESC")){
            Database.bind(p,u.id());try(ResultSet r=p.executeQuery()){while(r.next())out.add(request(r));}}
        return out;
    }
    @Override public Map<String,Object> findRequest(int id)throws SQLException{
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement(RSQL+" WHERE q.id=?")){
            Database.bind(p,id);try(ResultSet r=p.executeQuery()){return r.next()?request(r):null;}}
    }
    @Override public void status(int id,String next)throws SQLException{
        try(Connection c=db.connect()){
            c.setAutoCommit(false);
            try {
                String previous;int harvestId;double qty;
                try(PreparedStatement q=c.prepareStatement("SELECT status,harvest_id,quantity FROM inquiries WHERE id=?")){
                    Database.bind(q,id);try(ResultSet r=q.executeQuery()){
                        if(!r.next())throw new ApiException(404,"Request not found");
                        previous=r.getString(1);harvestId=r.getInt(2);qty=r.getDouble(3);
                    }
                }
                // Keep inventory consistent when a request is accepted or cancelled.
                if(next.equals("accepted")){
                    try(PreparedStatement p=c.prepareStatement("UPDATE harvests SET quantity=quantity-? WHERE id=? AND status='active' AND quantity>=?")){
                        Database.bind(p,qty,harvestId,qty);
                        if(p.executeUpdate()!=1)throw new ApiException(409,"Not enough stock remains; cannot accept request");
                    }
                }else if(previous.equals("accepted")&&next.equals("cancelled")){
                    try(PreparedStatement p=c.prepareStatement("UPDATE harvests SET quantity=quantity+? WHERE id=?")){
                        Database.bind(p,qty,harvestId);p.executeUpdate();
                    }
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE inquiries SET status=? WHERE id=? AND status=?")){
                    Database.bind(p,next,id,previous);
                    if(p.executeUpdate()!=1)throw new ApiException(409,"Request was updated; refresh and try again");
                }
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
    @Override public List<Map<String,Object>> messages(int id)throws SQLException{
        List<Map<String,Object>> out=new ArrayList<>();
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("SELECT m.*,u.first_name,u.last_name FROM messages m JOIN users u ON u.id=m.sender_id WHERE m.inquiry_id=? ORDER BY m.id")){
            Database.bind(p,id);try(ResultSet r=p.executeQuery()){while(r.next())out.add(Inputs.map("id",r.getInt("id"),"requestId",r.getInt("inquiry_id"),"senderId",r.getInt("sender_id"),"senderName",r.getString("first_name")+" "+r.getString("last_name"),"text",r.getString("body"),"createdAt",r.getString("created_at")));}}
        return out;
    }
    @Override public Map<String,Object> send(int id,int sender,String body)throws SQLException{
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("INSERT INTO messages(inquiry_id,sender_id,body) VALUES(?,?,?)")){
            Database.bind(p,id,sender,body);p.executeUpdate();int mid=Database.id(c);
            return Inputs.map("id",mid,"requestId",id,"senderId",sender,"text",body);
        }
    }
    @Override public List<Harvest> saved(int buyerId)throws SQLException{
        // Keep the SQL layer for each relation behind its repository interface.
        List<Integer> ids=new ArrayList<>();
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("SELECT harvest_id FROM saved WHERE buyer_id=? ORDER BY harvest_id DESC")){
            Database.bind(p,buyerId);try(ResultSet r=p.executeQuery()){while(r.next())ids.add(r.getInt(1));}}
        HarvestRepository harvests=new JdbcHarvestRepository(db);List<Harvest> out=new ArrayList<>();
        for(int id:ids){Harvest h=harvests.find(id);if(h!=null && h.status().equals("active"))out.add(h);}return out;
    }
    @Override public void save(int buyerId,int harvestId)throws SQLException{
        try(Connection c=db.connect();PreparedStatement chk=c.prepareStatement("SELECT 1 FROM saved WHERE buyer_id=? AND harvest_id=?")){
            Database.bind(chk,buyerId,harvestId);try(ResultSet r=chk.executeQuery()){if(r.next())return;}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO saved(buyer_id,harvest_id) VALUES(?,?)")){Database.bind(p,buyerId,harvestId);p.executeUpdate();}
        }
    }
    @Override public void unsave(int buyerId,int harvestId)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("DELETE FROM saved WHERE buyer_id=? AND harvest_id=?")){
        Database.bind(p,buyerId,harvestId);p.executeUpdate();}}
}
