package com.farmlink;
import java.sql.*;
import java.util.*;
public interface MarketplaceService {
    List<Map<String,Object>> browse(Map<String,String> filters) throws SQLException;
    Map<String,Object> detail(int id) throws SQLException;
    Map<String,Object> create(Account user,Map<String,Object> data) throws SQLException;
    Map<String,Object> update(Account user,int id,Map<String,Object> data) throws SQLException;
    void unlist(Account user,int id) throws SQLException;
}
