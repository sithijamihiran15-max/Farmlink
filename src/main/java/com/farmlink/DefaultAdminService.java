package com.farmlink;
import java.sql.SQLException;
import java.util.*;
public final class DefaultAdminService implements AdminService {
    private final AdminRepository repository;
    public DefaultAdminService(AdminRepository repository){this.repository=repository;}
    private static void requireAdmin(Account actor){Inputs.allowed(actor!=null && "ADMIN".equals(actor.role()),"Administrator access required");}
    @Override public Map<String,Object> overview(Account actor)throws SQLException{requireAdmin(actor);return repository.overview();}
    @Override public List<Map<String,Object>> users(Account actor)throws SQLException{requireAdmin(actor);return repository.users();}
    @Override public List<Map<String,Object>> harvests(Account actor)throws SQLException{requireAdmin(actor);return repository.harvests();}
    @Override public void unlist(Account actor,int harvestId)throws SQLException{
        requireAdmin(actor);
        if(!repository.unlistHarvest(harvestId))throw new ApiException(404,"Active harvest not found");
    }
    private static void checkDeletionPermission(Account actor,int userId) {
        requireAdmin(actor);
        if(actor.id()==userId)throw new ApiException(403,"You cannot delete your own administrator account");
    }
    @Override public Map<String,Object> deleteImpact(Account actor,int userId) throws SQLException {
        checkDeletionPermission(actor,userId);
        return repository.deleteImpact(userId);
    }
    @Override public void deleteUser(Account actor,int userId) throws SQLException {
        checkDeletionPermission(actor,userId);
        repository.deleteUser(userId);
    }
}
