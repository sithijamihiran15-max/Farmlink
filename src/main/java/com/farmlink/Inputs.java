package com.farmlink;
import java.util.*;
public final class Inputs {
    private Inputs() {}
    public static String str(Map<String,Object> data,String name) { Object v=data.get(name);return v==null?"":v.toString().trim(); }
    public static String require(Map<String,Object> data,String name,int min,int max) {
        String s=str(data,name); if(s.length()<min || s.length()>max) throw new ApiException(400,name+" must have "+min+" to "+max+" characters");return s;
    }
    public static double number(Map<String,Object> d,String name,double min,double max){
        try {double n=Double.parseDouble(str(d,name));if(!Double.isFinite(n)||n<min||n>max) throw new NumberFormatException();return n;}
        catch(NumberFormatException e){throw new ApiException(400,name+" must be between "+min+" and "+max);}
    }
    public static int id(String raw){try {int id=Integer.parseInt(raw);if(id<0)throw new NumberFormatException();return id;}catch(NumberFormatException e){throw new ApiException(400,"Invalid numeric ID");}}
    /** JSON numbers arrive as Double; numeric IDs must still be accepted as integers. */
    public static int idValue(Object value){
        if(value instanceof Number n){double d=n.doubleValue();if(Double.isFinite(d)&&d>=0&&d<=Integer.MAX_VALUE&&d==Math.floor(d))return (int)d;}
        if(value instanceof String s)return id(s);
        throw new ApiException(400,"Invalid numeric ID");
    }
    public static String oneOf(Map<String,Object> d,String field,String...values){String v=str(d,field);if(!Arrays.asList(values).contains(v))throw new ApiException(400,"Invalid "+field);return v;}
    public static void allowed(boolean okay,String message){if(!okay)throw new ApiException(403,message);}
    public static Map<String,Object> map(Object...pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
}
