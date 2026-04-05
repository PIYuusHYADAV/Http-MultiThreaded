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
        if(path.endsWith(".css"))
        {
            return path.substring(1);
        }
        else if(path.endsWith(".js"))
        {
            return path.substring(1);
        }
        else 
        {
            return hm.getOrDefault(path, "static/404.html");
        }
    }       
}
