import RouteHandler.Handler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import Wrapper.Wrapper;

public class httpServer
{
    static class Request
    {
        String method;
        String rawroute;
        String path;
        HashMap<String,String> headers;
        HashMap<String,String> query;
        HashMap<String,String> params;
        public Request(String m,String r,HashMap<String,String> hm)
        {
            this.method = m;
            this.rawroute = r;
            this.headers = hm;
            this.params = new HashMap<>();
            this.query = new HashMap<>();
        }

    }
    private static ArrayList<String> data = new ArrayList<>();
    private static HashMap<String,String> extractHeaders(BufferedReader Buffer) throws Exception
    {
        HashMap<String,String> hm = new HashMap<>();
        String line;
        while((line = Buffer.readLine()) != null && !line.isEmpty())
        {
            String[] parts = line.split(":", 2);
            if(parts.length == 2)
            {
                hm.put(parts[0].trim(),parts[1].trim());
            }

        }
        return hm;
    }
    private static String stylingPath(String filePath)
    {
        if (filePath.endsWith(".css"))
        {
            return "text/css";
        }
        else if (filePath.endsWith(".js"))
        {
            return "application/javascript";
        }
        else
        {
            return "text/html";
        }
    }
    
    private static void addRoutes(Handler router)
    {
        Wrapper.run(()->{
            router.addRoute("/", "static/index.html");
            router.addRoute("/about", "static/about.html");
            router.addRoute("/form", "static/form.html");
        });
    }
    private static String readPostBody(BufferedReader buffer,Request req) throws Exception
    {
        
        int contentLength = Integer.parseInt(req.headers.get("Content-Length"));
        System.out.println(contentLength);
        char[] body = new char[contentLength];
        System.out.println(body);
        buffer.read(body,0,contentLength);

        return new String(body);
        
    }
    private static String mapOutput(HashMap<String,String> data)
    {
            StringBuilder json = new StringBuilder("{");
            for(Map.Entry<String,String> entry : data.entrySet())
            {
                json.append("\"")
                    .append(entry.getKey())
                    .append("\":\"")
                    .append(entry.getValue())
                    .append("\",");
            }
            json.deleteCharAt(json.length() - 1); 
            json.append("}");
            return json.toString();
    }
    private static void handlePost(BufferedReader buffer,Socket client,Request req)
    {
        Wrapper.run(()->{
            String body = readPostBody(buffer,req);
            System.out.println(body);
            HashMap<String,String> data = parseData(body);
            body = mapOutput(data);
            System.out.println(body);
            byte[] bytes = body.getBytes();
            String headers =
                    "HTTP/1.1 200 OK\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
            "Access-Control-Allow-Headers: Content-Type,Content-Length, Authorization\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + bytes.length + "\r\n" +
                    "Connection: close\r\n\r\n";
            OutputStream out = client.getOutputStream();
            out.write(headers.getBytes());
            out.write(bytes);
            out.flush();
            client.close();
        });
    }
    private static void showUI(String filePath,Socket client)
    {
        Wrapper.run(()->{
            byte[] bodyBytes = Files.readAllBytes(Path.of(filePath));

            String headers =
                    "HTTP/1.1 200 OK\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
            "Access-Control-Allow-Headers: Content-Type,Content-Length, Authorization\r\n" +
                    "Content-Type: " + stylingPath(filePath) + "\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            OutputStream out = client.getOutputStream();
            out.write(headers.getBytes());
            out.write(bodyBytes);
            out.flush();
            client.close(); 
        });
    }
    private static HashMap<String,String> parseData(String body)
    {
        HashMap<String,String> hm = new HashMap<>();
        String[] content = body.split("&");
        for(String cont : content)
        {
            String[] Pair = cont.split("=");
            String key = Pair[0];
            String value = Pair[1];
            hm.put(key,value);
        }
        return hm;
    }
    private static void handleByIdPut(BufferedReader Buffer,String id,Request req) throws Exception
    {
        String body = readPostBody(Buffer,req);
        HashMap<String,String> values = parseData(body);
        
        for(String dataId : data)
        {
            if(dataId.equals(id))
            {
                // update logic here.
            }
        }
        return;

    }
    private static void handleByIdDelete(String id) throws Exception
    {
       data.remove(id);
       return;
    }
    private static boolean authorizationMiddleware(Request req,Socket client) throws Exception
    {
        String val = req.headers.get("Authorization");
        
        if(val == null)
        {
            sendUnauthorized(client);
            return false;
        }
        return true;
    }
    private static void sendUnauthorized(Socket client) throws Exception
    {
        String body = "{\"error\":\"Unauthorized\"}";
        byte[] bytes = body.getBytes();
        String headers =
            "HTTP/1.1 401 Unauthorized\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
            "Access-Control-Allow-Headers: Content-Type,Content-Length, Authorization\r\n" +
            "Content-Type: application/json\r\n" +
            "Content-Length: " + bytes.length + "\r\n" +
            "Connection: close\r\n\r\n";
        OutputStream out = client.getOutputStream();
        out.write(headers.getBytes());
        out.write(bytes);
        out.flush();
        client.close();
    }
    private static HashMap<String,String> parseQuery(String query) throws Exception
    {
        HashMap<String,String> hm = new HashMap<>();
        if(query.isEmpty()) return hm;
        String Query[] = query.split("&");
        for(String str : Query)
        {
            String[] queryparams = str.split("=",2);
            hm.put(queryparams[0],queryparams[1]);   
        }
        
        
        return hm;
    }
    private static void extractParams(Request req) throws Exception
    {
        if(req.path.length() <=1) return;
        String routes[] = req.path.split("/");
        System.out.println(routes[routes.length-1]);
        req.params.put("id",routes[routes.length-1]);
      
        return;
    }
    private static void parseQueryAndParameter(Request req) throws Exception
    {
        
        if(req.rawroute.contains("?"))
        {
            String parts[] = req.rawroute.split("\\?",2);
            req.path = parts[0];
            req.query = parseQuery(parts[1]);
        }
        else
        {
            req.path = req.rawroute;
        }
    }
    private static void handleOptions(Socket client) throws Exception
    {
        String headers = "HTTP/1.1 204 No Content\r\n" +
        "Access-Control-Allow-Origin: *\r\n" +
        "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
        "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
        "Connection: close\r\n\r\n";
        OutputStream out = client.getOutputStream();
        out.write(headers.getBytes());
   
        out.flush();
        client.close();
    }

    private static void handleClient(Socket client)
    {
        Wrapper.run(()->{
                BufferedReader Buffer = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String site = Buffer.readLine();
                String[] parts = site.split(" ");
                String method = parts[0];
                String route = parts[1];
                
                Handler router = new Handler();
                addRoutes(router);
                Request req = new Request(method, route, extractHeaders(Buffer));
                if(method.equals("OPTIONS"))
                {
                    handleOptions(client);
                    return;
                }
    
                // if(!authorizationMiddleware(req,client))
                // {
                //     return;
                // }
                parseQueryAndParameter(req);
                extractParams(req);
                
                if(method.equals("GET"))
                {
                    String filePath = router.resolve(req.path);
                   
                    showUI(filePath, client);

                }
                else if(method.equals("POST"))
                {
                    handlePost(Buffer, client,req);
                }
                else if(method.equals("PUT"))
                {
                    
                    
                    handleByIdPut(Buffer,req.params.get("id"),req);
                }
        
                
                else
                {
                  
                    handleByIdDelete(req.params.get("id"));
                }
            });
    }
    
    public static void main(String[] args) 
    {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        Wrapper.run(()->{
            ServerSocket server = new ServerSocket(8080);
            while(true)
            {
                Socket client = server.accept();
                pool.submit(()->{
                    handleClient(client);
                });
                
            }
            

        });
        

       
        
    }
}