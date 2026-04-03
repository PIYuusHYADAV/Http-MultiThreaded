package RouteHandler;

import java.util.HashMap;

public class Handler 
{
    private final HashMap<String,String> hm = new HashMap<>();
    public void addRoute(String path,String filePath)
    {
        hm.put(path,filePath);
    }
    public String resolve(String path)
    {
        return hm.getOrDefault(path, "static/404.html");
    }       
}
