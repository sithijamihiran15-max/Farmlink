package com.farmlink;
import java.sql.*;
import java.util.*;
public final class DefaultMarketplaceService implements MarketplaceService {
    private final HarvestRepository harvests;
    public DefaultMarketplaceService(HarvestRepository harvests){this.harvests=harvests;}
    @Override public List<Map<String,Object>> browse(Map<String,String> filters)throws SQLException{
        List<Map<String,Object>> out=new ArrayList<>();for(Harvest h:harvests.findAll(filters))out.add(h.publicView());return out;
    }
    @Override public Map<String,Object> detail(int id)throws SQLException{return mustFind(id).publicView();}
    private Harvest mustFind(int id)throws SQLException{Harvest h=harvests.find(id);if(h==null)throw new ApiException(404,"Harvest not found");return h;}
    private void owner(Account u,Harvest h){Inputs.allowed(u!=null&&u.role().equals("FARMER")&&h.farmerId()==u.id(),"Only the listing farmer can modify this harvest");}
    private void validate(Map<String,Object> d){
        Inputs.require(d,"title",5,90);Inputs.oneOf(d,"category","Vegetables","Fruits","Grains","Leafy Greens","Spices","Other");
        Inputs.require(d,"district",2,80);Inputs.number(d,"price",0.01,10_000_000);Inputs.number(d,"quantity",0.01,10_000_000);
        Inputs.oneOf(d,"unit","kg","bundles","pieces","bags","tons");Inputs.oneOf(d,"status","active","inactive");
        Inputs.require(d,"description",12,3000);
        String image=Inputs.str(d,"image");if(image.length()>300_000 || (!image.isBlank()&&!image.matches("(?s)^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$")))
            throw new ApiException(400,"Invalid image: upload PNG, JPEG or WebP under ~225 KB");
    }
    @Override public Map<String,Object> create(Account u,Map<String,Object> d)throws SQLException{
        Inputs.allowed(u!=null&&u.role().equals("FARMER"),"Only farmers can publish harvests");validate(d);return harvests.create(u.id(),d).publicView();
    }
    @Override public Map<String,Object> update(Account u,int id,Map<String,Object> d)throws SQLException{
        Harvest h=mustFind(id);owner(u,h);validate(d);return harvests.update(id,d).publicView();
    }
    @Override public void unlist(Account u,int id)throws SQLException{
        Harvest h=mustFind(id);owner(u,h);harvests.delete(id); // soft delete: keep request history intact
    }
}
