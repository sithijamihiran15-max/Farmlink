package com.farmlink;
import java.io.Console;
import java.nio.file.Path;
import java.util.*;
/** Local-only administrator bootstrap. Never exposed as a web/API route. */
public final class AdminSetup {
    private AdminSetup(){}
    public static void main(String[] args) throws Exception {
        System.out.println("FarmLink - Create local administrator account");
        System.out.println("This command adds ADMIN directly to the selected FarmLink database.");
        System.out.println("Use FARMLINK_DB=mysql and FARMLINK_DB_PASSWORD to select MySQL.");
        Console console=System.console();
        Scanner scanner=console==null?new Scanner(System.in):null;
        String first=prompt(console,scanner,"First name",2,90);
        String last=prompt(console,scanner,"Last name",2,90);
        String nic=prompt(console,scanner,"Admin login NIC (unique)",5,40);
        if(!nic.matches("[a-zA-Z0-9-]{5,40}"))throw new IllegalArgumentException("NIC may contain only letters, digits and hyphen.");
        String email=prompt(console,scanner,"Email",5,150);
        if(!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new IllegalArgumentException("Valid email required.");
        String password;
        if(console!=null){
            char[] pw=console.readPassword("Password (min 12 characters): ");
            char[] again=console.readPassword("Confirm password: ");
            if(pw==null || again==null)throw new IllegalStateException("Password input cancelled.");
            if(!Arrays.equals(pw,again))throw new IllegalArgumentException("Passwords do not match.");
            password=new String(pw);Arrays.fill(pw,'\0');Arrays.fill(again,'\0');
        }else{
            System.out.println("WARNING: This terminal cannot hide password input. Use Command Prompt for private entry.");
            System.out.print("Password (min 12 characters): ");password=scanner.nextLine();
            System.out.print("Confirm password: ");if(!password.equals(scanner.nextLine()))throw new IllegalArgumentException("Passwords do not match.");
        }
        if(password.length()<12 || password.length()>128)throw new IllegalArgumentException("Password must be 12 to 128 characters.");
        Database db=new Database(Path.of("data"));
        try{
            AccountRepository users=new JdbcAccountRepository(db);
            if(users.findByNic(nic)!=null)throw new IllegalStateException("NIC is already registered. Choose a different NIC; existing accounts are not changed.");
            Map<String,Object> values=Inputs.map("role","ADMIN","firstName",first,"lastName",last,"nic",nic,"email",email,"phone","0000000000","district","Local","province","Local","address","System administrator");
            Account admin=users.create(values,PasswordHasher.hash(password));
            System.out.println("SUCCESS: Admin account created (ID="+admin.id()+", NIC="+nic+").");
            System.out.println("Log in at the normal FarmLink login page. Your dashboard is /admin-dashboard.html");
        }finally{db.shutdown();}
    }
    private static String prompt(Console console,Scanner scanner,String label,int min,int max){
        String s=console!=null?console.readLine(label+": "):read(scanner,label+": ");
        if(s==null || s.trim().length()<min || s.trim().length()>max)throw new IllegalArgumentException(label+" must be between "+min+" and "+max+" characters.");
        return s.trim();
    }
    private static String read(Scanner scanner,String label){System.out.print(label);return scanner.nextLine();}
}
