package com.farmlink;
import java.util.*;
/** Immutable domain object. Clients see accessors, never writable fields. */
public final class Harvest {
    private final int id,farmerId; private final String title,category,district,unit,status,description,image,farmerName,createdAt;
    private final double price,quantity;
    public Harvest(int id,int farmerId,String title,String category,String district,double price,double quantity,
        String unit,String status,String description,String image,String farmerName,String createdAt) {
        this.id=id;this.farmerId=farmerId;this.title=title;this.category=category;this.district=district;
        this.price=price;this.quantity=quantity;this.unit=unit;this.status=status;
        this.description=description;this.image=image;this.farmerName=farmerName;this.createdAt=createdAt;
    }
    public int id(){return id;} public int farmerId(){return farmerId;} public String status(){return status;}
    public double quantity(){return quantity;} public double price(){return price;}
    public Map<String,Object> publicView(){return Inputs.map("id",id,"farmerId",farmerId,"title",title,
        "category",category,"district",district,"price",price,"quantity",quantity,"unit",unit,
        "status",status,"description",description,"image",image,"farmerName",farmerName,"createdAt",createdAt);}
}
