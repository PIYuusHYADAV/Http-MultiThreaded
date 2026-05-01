package RouteHandler;

import java.util.ArrayList;
import java.util.HashMap;

public class Handler 
{
    String path;
    String file;
    ArrayList<Handler> routes;
    public Handler()
    {
        this.routes = new ArrayList<>();
    }
    public Handler(String p,String f)
    {
        this.path = p;
        this.file = f;
        this.routes = new ArrayList<>();
    }

    public void addRoute(String path,String filePath)
    {
        routes.add(new Handler(path, filePath));
    }
    private HashMap<String,String> match(String routepath,String actualpath)
    {
        String[] routeparts = routepath.split("/");
        String[] actualparts = actualpath.split("/");
        if(routeparts.length != actualparts.length) return null;
        HashMap<String, String> values = new HashMap<>();

        for(int i=0;i<routeparts.length;i++)
        {
            String r = routeparts[i];
            String a = actualparts[i];
            if(r.startsWith(":"))
            {
                String key = r.substring(1);
                values.put(key, a);
            }else if(!r.equals(a))
            {
                return null;
            }
        }
        return values;
    }
    public String resolve(String path,HashMap<String,String>params)
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
            for(Handler route:routes)
            {
                HashMap<String,String> paramsvalues = match(route.path,path);
                if(paramsvalues!= null)
                {
                    params.putAll(paramsvalues);
                    return route.file;
                }
            }
            return "static/404.html";  
        }
    }       
}
