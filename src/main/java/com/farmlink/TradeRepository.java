package com.farmlink;
import java.sql.*;
import java.util.*;
public interface TradeRepository {
    Map<String,Object> createRequest(int buyerId,Harvest h,double quantity,String message) throws SQLException;
    List<Map<String,Object>> listRequests(Account user) throws SQLException;
    Map<String,Object> findRequest(int id) throws SQLException;
    void status(int id,String status) throws SQLException;
    List<Map<String,Object>> messages(int requestId) throws SQLException;
    Map<String,Object> send(int requestId,int senderId,String body) throws SQLException;
    List<Harvest> saved(int buyerId) throws SQLException;
    void save(int buyerId,int harvestId) throws SQLException;
    void unsave(int buyerId,int harvestId) throws SQLException;
}
