package com.farmlink;
import java.sql.*;
import java.util.*;
public final class JdbcAccountRepository implements AccountRepository {
    private final Database db;
    public JdbcAccountRepository(Database db){this.db=db;}
    private Account decode(ResultSet r)throws SQLException {
        String role=r.getString("role");
        if("FARMER".equals(role)) return new Farmer(r.getInt("id"),r.getString("first_name"),r.getString("last_name"),r.getString("nic"),r.getString("email"),r.getString("phone"),r.getString("district"),r.getString("province"),r.getString("address"));
        if("BUYER".equals(role)) return new Buyer(r.getInt("id"),r.getString("first_name"),r.getString("last_name"),r.getString("nic"),r.getString("email"),r.getString("phone"),r.getString("district"),r.getString("province"),r.getString("address"));
        if("ADMIN".equals(role)) return new Admin(r.getInt("id"),r.getString("first_name"),r.getString("last_name"),r.getString("nic"),r.getString("email"),r.getString("phone"),r.getString("district"),r.getString("province"),r.getString("address"));
        throw new SQLException("Unsupported account role: " + role);
    }
    @Override public Account find(int id)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("SELECT * FROM users WHERE id=?")){
        Database.bind(p,id);try(ResultSet r=p.executeQuery()){return r.next()?decode(r):null;}}}
    @Override public Account findByNic(String nic)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("SELECT * FROM users WHERE UPPER(nic)=UPPER(?)")){
        Database.bind(p,nic);try(ResultSet r=p.executeQuery()){return r.next()?decode(r):null;}}}
    @Override public String passwordHash(String nic)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("SELECT password_hash FROM users WHERE UPPER(nic)=UPPER(?)")){
        Database.bind(p,nic);try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):null;}}}
    @Override public Account create(Map<String,Object> v,String passHash)throws SQLException{
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("INSERT INTO users (role,first_name,last_name,nic,email,phone,district,province,address,password_hash) VALUES (?,?,?,?,?,?,?,?,?,?)")){
            Database.bind(p,Inputs.str(v,"role"),Inputs.str(v,"firstName"),Inputs.str(v,"lastName"),Inputs.str(v,"nic"),Inputs.str(v,"email"),Inputs.str(v,"phone"),Inputs.str(v,"district"),Inputs.str(v,"province"),Inputs.str(v,"address"),passHash);
            p.executeUpdate();return find(Database.id(c));
        }
    }
    @Override public Account update(int id,Map<String,Object> v)throws SQLException{
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("UPDATE users SET first_name=?,last_name=?,phone=?,email=?,district=?,province=?,address=? WHERE id=?")){
            Database.bind(p,Inputs.str(v,"firstName"),Inputs.str(v,"lastName"),Inputs.str(v,"phone"),Inputs.str(v,"email"),Inputs.str(v,"district"),Inputs.str(v,"province"),Inputs.str(v,"address"),id);
            p.executeUpdate();return find(id);
        }
    }
    @Override public void session(String tokenHash,int userId,Timestamp expires)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("INSERT INTO sessions(token_hash,user_id,expires_at) VALUES(?,?,?)")){
        Database.bind(p,tokenHash,userId,expires);p.executeUpdate();}}
    @Override public Account bySession(String digest)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("SELECT u.* FROM users u JOIN sessions s ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>CURRENT_TIMESTAMP")){
        Database.bind(p,digest);try(ResultSet r=p.executeQuery()){return r.next()?decode(r):null;}}}
    @Override public void invalidate(String digest)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("DELETE FROM sessions WHERE token_hash=?")){
        Database.bind(p,digest);p.executeUpdate();}}
}
