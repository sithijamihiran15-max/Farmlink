package com.farmlink;
import java.sql.*;
import java.util.*;
/** JDBC implementation: works with both MySQL and embedded HSQLDB. */
public final class JdbcAdminRepository implements AdminRepository {
    private final Database db;
    public JdbcAdminRepository(Database db){this.db=db;}
    private static long count(Connection c,String sql) throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}
    }
    @Override public Map<String,Object> overview() throws SQLException {
        try(Connection c=db.connect()) {
            return Inputs.map(
                "farmers",count(c,"SELECT COUNT(*) FROM users WHERE role='FARMER'"),
                "buyers",count(c,"SELECT COUNT(*) FROM users WHERE role='BUYER'"),
                "admins",count(c,"SELECT COUNT(*) FROM users WHERE role='ADMIN'"),
                "harvests",count(c,"SELECT COUNT(*) FROM harvests"),
                "activeHarvests",count(c,"SELECT COUNT(*) FROM harvests WHERE status='active'"),
                "requests",count(c,"SELECT COUNT(*) FROM inquiries"));
        }
    }
    @Override public List<Map<String,Object>> users() throws SQLException {
        List<Map<String,Object>> out=new ArrayList<>();
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("SELECT id,first_name,last_name,nic,role,district,created_at FROM users ORDER BY id DESC");ResultSet r=p.executeQuery()){
            while(r.next())out.add(Inputs.map("id",r.getInt("id"),"name",r.getString("first_name")+" "+r.getString("last_name"),"nic",r.getString("nic"),"role",r.getString("role"),"district",r.getString("district"),"createdAt",String.valueOf(r.getTimestamp("created_at"))));
        }return out;
    }
    @Override public List<Map<String,Object>> harvests() throws SQLException {
        List<Map<String,Object>> out=new ArrayList<>();
        String sql="SELECT h.id,h.title,h.category,h.status,h.district,h.quantity,h.unit,u.first_name,u.last_name FROM harvests h JOIN users u ON h.farmer_id=u.id ORDER BY h.id DESC";
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            while(r.next())out.add(Inputs.map("id",r.getInt("id"),"title",r.getString("title"),"category",r.getString("category"),"status",r.getString("status"),"district",r.getString("district"),"quantity",r.getDouble("quantity"),"unit",r.getString("unit"),"farmerName",r.getString("first_name")+" "+r.getString("last_name")));
        }return out;
    }
    @Override public boolean unlistHarvest(int id) throws SQLException {
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("UPDATE harvests SET status='inactive' WHERE id=? AND status='active'")){
            p.setInt(1,id);return p.executeUpdate()==1;
        }
    }
    // Reference counts describe exactly what the permanent deletion will remove.
    private static final String RELATED_INQUIRIES =
        "buyer_id=? OR farmer_id=? OR harvest_id IN (SELECT id FROM harvests WHERE farmer_id=?)";
    private static final String RELATED_MESSAGES =
        "sender_id=? OR inquiry_id IN (SELECT id FROM inquiries WHERE " + RELATED_INQUIRIES + ")";
    private static final String RELATED_SAVED =
        "buyer_id=? OR harvest_id IN (SELECT id FROM harvests WHERE farmer_id=?)";
    private static int countFor(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            Database.bind(p,args);
            try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}
        }
    }
    private static int deleteFor(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            Database.bind(p,args);return p.executeUpdate();
        }
    }
    private static Map<String,Object> impact(Connection c,int userId) throws SQLException {
        String role;String name;String nic;
        try(PreparedStatement p=c.prepareStatement("SELECT role,first_name,last_name,nic FROM users WHERE id=?")) {
            Database.bind(p,userId);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next())throw new ApiException(404,"Account not found");
                role=r.getString(1);name=r.getString(2)+" "+r.getString(3);nic=r.getString(4);
            }
        }
        if(!"BUYER".equals(role)&&!"FARMER".equals(role))
            throw new ApiException(403,"Administrator accounts are protected from deletion");
        return Inputs.map("id",userId,"name",name,"nic",nic,"role",role,
            "harvests",countFor(c,"SELECT COUNT(*) FROM harvests WHERE farmer_id=?",userId),
            "requests",countFor(c,"SELECT COUNT(*) FROM inquiries WHERE "+RELATED_INQUIRIES,userId,userId,userId),
            "messages",countFor(c,"SELECT COUNT(*) FROM messages WHERE "+RELATED_MESSAGES,userId,userId,userId,userId),
            "saved",countFor(c,"SELECT COUNT(*) FROM saved WHERE "+RELATED_SAVED,userId,userId),
            "sessions",countFor(c,"SELECT COUNT(*) FROM sessions WHERE user_id=?",userId));
    }
    @Override public Map<String,Object> deleteImpact(int userId) throws SQLException {
        try(Connection c=db.connect()) {return impact(c,userId);}
    }
    @Override public void deleteUser(int userId) throws SQLException {
        // Explicit dependency order supports the existing RESTRICT foreign keys
        // in both MySQL and HSQLDB, without modifying schema or disabling FK checks.
        try(Connection c=db.connect()) {
            c.setAutoCommit(false);
            try {
                impact(c,userId); // fail before modifying anything if missing/protected
                deleteFor(c,"DELETE FROM messages WHERE "+RELATED_MESSAGES,userId,userId,userId,userId);
                deleteFor(c,"DELETE FROM saved WHERE "+RELATED_SAVED,userId,userId);
                deleteFor(c,"DELETE FROM sessions WHERE user_id=?",userId);
                deleteFor(c,"DELETE FROM inquiries WHERE "+RELATED_INQUIRIES,userId,userId,userId);
                deleteFor(c,"DELETE FROM harvests WHERE farmer_id=?",userId);
                if(deleteFor(c,"DELETE FROM users WHERE id=? AND role IN ('BUYER','FARMER')",userId)!=1)
                    throw new ApiException(409,"Account changed during deletion; refresh and try again");
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }
    }
}
