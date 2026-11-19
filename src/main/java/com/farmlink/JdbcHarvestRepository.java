package com.farmlink;
import java.sql.*;
import java.util.*;
public final class JdbcHarvestRepository implements HarvestRepository {
    private final Database db;
    private static final String SELECT="SELECT h.*, u.first_name, u.last_name FROM harvests h JOIN users u ON u.id=h.farmer_id";
    public JdbcHarvestRepository(Database db){this.db=db;}
    private Harvest decode(ResultSet r)throws SQLException{return new Harvest(r.getInt("id"),r.getInt("farmer_id"),r.getString("title"),r.getString("category"),r.getString("district"),r.getDouble("price"),r.getDouble("quantity"),r.getString("unit"),r.getString("status"),r.getString("description"),r.getString("image"),r.getString("first_name")+" "+r.getString("last_name"),r.getString("created_at"));}
    @Override public List<Harvest> findAll(Map<String,String> f)throws SQLException {
        StringBuilder sql=new StringBuilder(SELECT).append(" WHERE 1=1");List<Object> args=new ArrayList<>();
        if(f.containsKey("farmerId")){sql.append(" AND h.farmer_id=?");args.add(Inputs.id(f.get("farmerId")));}
        else sql.append(" AND h.status='active'");
        if(!f.getOrDefault("search","").isBlank()){sql.append(" AND (UPPER(h.title) LIKE ? OR UPPER(h.description) LIKE ?)");String q="%"+f.get("search").toUpperCase(Locale.ROOT)+"%";args.add(q);args.add(q);}
        if(!f.getOrDefault("category","").isBlank()){sql.append(" AND h.category=?");args.add(f.get("category"));}
        if(!f.getOrDefault("district","").isBlank()){sql.append(" AND h.district=?");args.add(f.get("district"));}
        for(String k:List.of("min","max")){if(!f.getOrDefault(k,"").isBlank()){
            try{double n=Double.parseDouble(f.get(k));if(!Double.isFinite(n)||n<0)throw new NumberFormatException();
                sql.append(k.equals("min")?" AND h.price>=?":" AND h.price<=?");args.add(n);}
            catch(NumberFormatException e){throw new ApiException(400,"Invalid "+k+" price");}}}
        sql.append(" ORDER BY h.id DESC");List<Harvest> out=new ArrayList<>();
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement(sql.toString())){
            Database.bind(p,args.toArray());try(ResultSet r=p.executeQuery()){while(r.next())out.add(decode(r));}}
        return out;
    }
    @Override public Harvest find(int id)throws SQLException{try(Connection c=db.connect();PreparedStatement p=c.prepareStatement(SELECT+" WHERE h.id=?")){
        Database.bind(p,id);try(ResultSet r=p.executeQuery()){return r.next()?decode(r):null;}}}
    @Override public Harvest create(int farmerId,Map<String,Object> d)throws SQLException {
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("INSERT INTO harvests(farmer_id,title,category,district,price,quantity,unit,status,description,image) VALUES(?,?,?,?,?,?,?,?,?,?)")){
            Database.bind(p,farmerId,Inputs.str(d,"title"),Inputs.str(d,"category"),Inputs.str(d,"district"),Inputs.number(d,"price",0.01,10_000_000),Inputs.number(d,"quantity",0.01,10_000_000),Inputs.str(d,"unit"),Inputs.str(d,"status"),Inputs.str(d,"description"),Inputs.str(d,"image"));
            p.executeUpdate();return find(Database.id(c));
        }
    }
    @Override public Harvest update(int id,Map<String,Object> d)throws SQLException{
        try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("UPDATE harvests SET title=?,category=?,district=?,price=?,quantity=?,unit=?,status=?,description=?,image=? WHERE id=?")){
            Database.bind(p,Inputs.str(d,"title"),Inputs.str(d,"category"),Inputs.str(d,"district"),Inputs.number(d,"price",0.01,10_000_000),Inputs.number(d,"quantity",0.01,10_000_000),Inputs.str(d,"unit"),Inputs.str(d,"status"),Inputs.str(d,"description"),Inputs.str(d,"image"),id);
            p.executeUpdate();return find(id);
        }
    }
    @Override public void delete(int id)throws SQLException {try(Connection c=db.connect();PreparedStatement p=c.prepareStatement("UPDATE harvests SET status='inactive' WHERE id=?")){
        Database.bind(p,id);p.executeUpdate();}}
}
