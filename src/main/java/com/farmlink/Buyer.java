package com.farmlink;
public final class Buyer extends Account {
    public Buyer(int id,String first,String last,String nic,String email,String phone,String district,String province,String address){super(id,first,last,nic,email,phone,district,province,address);}
    @Override public String role(){return "BUYER";}
}
