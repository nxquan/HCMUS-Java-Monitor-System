
package hcmus.server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Action {
    private String ip;
    private int port;
    private String action;
    private String desc;
    private Date date;

    public String getDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        return formatter.format(date);
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Action(String ip, int port, String action, String desc, Date date) {
        this.ip = ip;
        this.port = port;
        this.action = action;
        this.desc = desc;
        this.date = date;
    }

    

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
    
   
    
    
    
}
