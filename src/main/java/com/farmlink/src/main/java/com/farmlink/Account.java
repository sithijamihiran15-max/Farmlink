package com.farmlink;
import java.util.*;
/** Abstraction + encapsulation: identity fields are private; role comes from a subtype. */
public abstract class Account {
    private final int id;
    private final String firstName,lastName,nic,email,phone,district,province,address;
    protected Account(int id,String firstName,String lastName,String nic,String email,String phone,String district,String province,String address) {
        this.id=id;this.firstName=firstName;this.lastName=lastName;this.nic=nic;this.email=email;
        this.phone=phone;this.district=district;this.province=province;this.address=address;
    }
    public final int id(){return id;}
    public final String firstName(){return firstName;}
    public final String fullName(){return firstName+" "+lastName;}
    public abstract String role(); // polymorphic: Farmer and Buyer answer differently
    public Map<String,Object> publicView(){return Inputs.map("id",id,"firstName",firstName,"lastName",lastName,
        "fullName",fullName(),"nic",nic,"email",email,"phone",phone,"district",district,"province",province,
        "address",address,"role",role());}
}
