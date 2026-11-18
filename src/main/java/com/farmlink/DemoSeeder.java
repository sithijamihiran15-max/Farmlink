package com.farmlink;
import java.sql.*;
import java.util.*;
/** Optional first-run demo data, disabled with FARMLINK_SEED_DEMO=false. */
public final class DemoSeeder {
    private DemoSeeder(){}
    public static void seed(AccountRepository users,HarvestRepository harvests,TradeRepository trades)throws SQLException{
        if(users.findByNic("DEMOFARMER")!=null || users.findByNic("DEMOBUYER")!=null)return;
        Account farmer=users.create(person("FARMER","Nimal","Perera","DEMOFARMER","Kandy","Central"),PasswordHasher.hash("farmer1234"));
        Account buyer=users.create(person("BUYER","Amaya","Silva","DEMOBUYER","Colombo","Western"),PasswordHasher.hash("buyer1234"));
        Object[][] data={
            {"Fresh Red Tomatoes","Vegetables","Kandy",240,350,"kg","Organic tomatoes picked fresh daily. Collection or transport can be arranged."},
            {"Golden Harvest Carrots","Vegetables","Nuwara Eliya",310,180,"kg","Crisp highland carrots grown with care and available in bulk."},
            {"Premium Green Cabbage","Vegetables","Nuwara Eliya",180,500,"kg","Fresh green cabbages, harvested by our family farm."},
            {"Sweet Ripe Bananas","Fruits","Kurunegala",220,240,"kg","Sweet yellow bananas from our local farm. Ideal for shops and restaurants."},
            {"Fresh Sweet Corn","Grains","Anuradhapura",150,400,"kg","Freshly gathered sweet corn with excellent natural flavor."},
            {"Farm Fresh Potatoes","Vegetables","Badulla",260,325,"kg","Fresh potatoes, graded and ready for collection or transport."},
            {"Fresh Green Broccoli","Vegetables","Nuwara Eliya",420,95,"kg","Small-batch fresh broccoli from cool-climate farms."},
            {"Sweet Papaya","Fruits","Matale",190,160,"kg","Naturally ripened papayas, handled carefully for freshness."},
            {"Fresh Leafy Spinach","Leafy Greens","Gampaha",140,120,"bundles","Bright green leafy spinach, cleaned and bundled after harvest."}
        };
        List<Harvest> created=new ArrayList<>();
        for(Object[] row:data){created.add(harvests.create(farmer.id(),Inputs.map("title",row[0],"category",row[1],"district",row[2],"price",row[3],"quantity",row[4],"unit",row[5],"status","active","description",row[6],"image","")));}
        trades.save(buyer.id(),created.get(0).id());trades.save(buyer.id(),created.get(1).id());
        trades.createRequest(buyer.id(),created.get(0),25,"Hi! I'm interested in 25 kg. Is pickup possible this week?");
        System.out.println("Demo accounts seeded: DEMOFARMER / farmer1234 and DEMOBUYER / buyer1234");
    }
    private static Map<String,Object> person(String role,String first,String last,String nic,String district,String province){
        return Inputs.map("role",role,"firstName",first,"lastName",last,"nic",nic,"phone","0771234567","email","","address","Main Street, "+district,"district",district,"province",province);
    }
}
