package com.farmlink;
/** Administrator account; polymorphic third Account subtype. */
public final class Admin extends Account {
    public Admin(int id,String first,String last,String nic,String email,String phone,String district,String province,String address){super(id,first,last,nic,email,phone,district,province,address);}
    @Override public String role(){return "ADMIN";}
}
