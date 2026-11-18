package com.farmlink;
import java.sql.*;
import java.time.*;
import java.util.*;
public final class DefaultAuthService implements AuthService {
    private final AccountRepository users;
    public DefaultAuthService(AccountRepository users){this.users=users;}
    @Override public Account register(Map<String,Object> v)throws SQLException{
        Inputs.oneOf(v,"role","FARMER","BUYER");
        Inputs.require(v,"firstName",2,90);Inputs.require(v,"lastName",2,90);
        String nic=Inputs.require(v,"nic",5,40);
        if(!nic.matches("[a-zA-Z0-9-]{5,40}"))throw new ApiException(400,"NIC contains invalid characters");
        Inputs.require(v,"phone",7,30);Inputs.require(v,"district",2,80);
        Inputs.require(v,"province",2,80);Inputs.require(v,"address",3,255);
        String email=Inputs.str(v,"email"); if(email.length()>150 || (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))throw new ApiException(400,"Invalid email");
        String pass=Inputs.require(v,"password",8,128);
        if(!pass.equals(Inputs.str(v,"confirmPassword")))throw new ApiException(400,"Passwords do not match");
        if(users.findByNic(nic)!=null)throw new ApiException(409,"An account with that NIC already exists");
        try{return users.create(v,PasswordHasher.hash(pass));}
        catch(SQLException e){if(e.getMessage()!=null && e.getMessage().toLowerCase().contains("unique"))throw new ApiException(409,"That NIC is already registered");throw e;}
    }
    @Override public Account authenticate(String nic,String password)throws SQLException{
        String hash=users.passwordHash(nic);
        if(hash==null || !PasswordHasher.verify(password,hash))throw new ApiException(401,"Invalid NIC or password");
        return users.findByNic(nic);
    }
    @Override public String newSession(Account user)throws SQLException{
        String token=PasswordHasher.newToken();users.session(PasswordHasher.tokenDigest(token),user.id(),Timestamp.from(Instant.now().plus(Duration.ofDays(7))));return token;
    }
    @Override public Account currentUser(String token)throws SQLException{
        return token==null||token.isBlank()?null:users.bySession(PasswordHasher.tokenDigest(token));
    }
    @Override public void signOut(String token)throws SQLException{if(token!=null&&!token.isBlank())users.invalidate(PasswordHasher.tokenDigest(token));}
    @Override public Account updateProfile(Account a,Map<String,Object> d)throws SQLException{
        Inputs.require(d,"firstName",2,90);Inputs.require(d,"lastName",2,90);
        Inputs.require(d,"phone",7,30);Inputs.require(d,"district",2,80);Inputs.require(d,"province",2,80);Inputs.require(d,"address",3,255);
        String email=Inputs.str(d,"email");if(email.length()>150 || (!email.isEmpty()&&!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))throw new ApiException(400,"Invalid email");
        return users.update(a.id(),d);
    }
}
