package com.farmlink;
import java.sql.*;
import java.util.*;
public interface TradeService {
    List<Map<String,Object>> requests(Account user) throws SQLException;
    Map<String,Object> request(Account user,int harvestId,double quantity,String message) throws SQLException;
    Map<String,Object> requestDetails(Account user,int requestId) throws SQLException;
    Map<String,Object> changeStatus(Account user,int requestId,String nextStatus) throws SQLException;
    List<Map<String,Object>> messages(Account user,int requestId) throws SQLException;
    Map<String,Object> message(Account user,int requestId,String text) throws SQLException;
    List<Map<String,Object>> saved(Account user) throws SQLException;
    void save(Account user,int harvestId) throws SQLException;
    void unsave(Account user,int harvestId) throws SQLException;
}
