package com.farmlink;

import java.util.*;

/** A deliberately small, dependency-free JSON reader/writer for API payloads. */
public final class Json {
    private Json() {}
    public static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return '"' + escape(s) + '"';
        if (value instanceof Number n) {
            if (n instanceof Double d && !Double.isFinite(d)) return "null";
            return n.toString();
        }
        if (value instanceof Boolean b) return b.toString();
        if (value instanceof Map<?,?> map) {
            StringJoiner j = new StringJoiner(",", "{", "}");
            for (var entry : map.entrySet()) j.add(stringify(String.valueOf(entry.getKey())) + ":" + stringify(entry.getValue()));
            return j.toString();
        }
        if (value instanceof Collection<?> items) {
            StringJoiner j = new StringJoiner(",", "[", "]");
            for (Object item : items) j.add(stringify(item));
            return j.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON data type: " + value.getClass());
    }
    private static String escape(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) switch(c) {
            case '"' -> b.append("\\\""); case '\\' -> b.append("\\\\"); case '\n' -> b.append("\\n");
            case '\r' -> b.append("\\r"); case '\t' -> b.append("\\t");
            default -> { if (c < 32) b.append(String.format("\\u%04x",(int)c)); else b.append(c); }
        }
        return b.toString();
    }
    public static Map<String,Object> object(String text) {
        Object result = new Parser(text).parse();
        if (!(result instanceof Map<?,?>)) throw new ApiException(400,"JSON object required");
        @SuppressWarnings("unchecked") Map<String,Object> map = (Map<String,Object>) result;
        return map;
    }
    private static final class Parser {
        private final String s; private int pos;
        Parser(String s) { this.s=s; }
        Object parse() { Object v = value(); ws(); if (pos != s.length()) fail(); return v; }
        private void ws() { while(pos<s.length() && Character.isWhitespace(s.charAt(pos))) pos++; }
        private void fail() { throw new ApiException(400,"Invalid JSON input"); }
        private char take() { if(pos>=s.length()) { fail(); } return s.charAt(pos++); }
        private Object value() {
            ws(); char c=take();
            if (c=='"') return string();
            if (c=='{') { Map<String,Object> m=new LinkedHashMap<>(); ws(); if(next('}')) return m;
                do { ws(); if(take()!='"') fail(); String key=string(); ws(); if(take()!=':') fail(); m.put(key,value()); ws(); if(next('}')) return m; if(take()!=',') fail(); } while(true); }
            if (c=='[') { List<Object> a=new ArrayList<>(); ws(); if(next(']')) return a;
                do { a.add(value()); ws(); if(next(']')) return a; if(take()!=',') fail(); } while(true); }
            if (c=='t' && word("rue")) return true;
            if (c=='f' && word("alse")) return false;
            if (c=='n' && word("ull")) return null;
            if (c=='-' || Character.isDigit(c)) { int from=pos-1; while(pos<s.length() && "0123456789.eE+-".indexOf(s.charAt(pos))>=0) pos++;
                try { return Double.parseDouble(s.substring(from,pos)); } catch(NumberFormatException e) { fail(); } }
            fail(); return null;
        }
        private boolean word(String w){ if(s.startsWith(w,pos)){pos+=w.length();return true;}fail();return false; }
        private boolean next(char c){ if(pos<s.length() && s.charAt(pos)==c){pos++;return true;}return false; }
        private String string() {
            StringBuilder b=new StringBuilder();
            while(true) { char c=take(); if(c=='"') return b.toString(); if(c<32) fail();
                if(c!='\\') { b.append(c); continue; }
                char e=take(); switch(e) {
                    case '"' -> b.append('"'); case '\\' -> b.append('\\'); case '/' -> b.append('/');
                    case 'b' -> b.append('\b'); case 'f' -> b.append('\f'); case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r'); case 't' -> b.append('\t');
                    case 'u' -> { if(pos+4>s.length()) fail(); try { b.append((char)Integer.parseInt(s.substring(pos,pos+4),16));pos+=4; }
                                  catch(NumberFormatException err) { fail(); } }
                    default -> fail();
                }
            }
        }
    }
}
