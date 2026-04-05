import RouteHandler.Handler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import Wrapper.Wrapper;

public class httpServer
{
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
    private static String readPostBody(BufferedReader buffer) throws Exception
    {
        
        String line;
        
        int contentLength = 0;
        while((line=buffer.readLine()) != null && !line.isEmpty())
        {
           
            if(line.startsWith("Content-Length"))
            {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }
        
        char[] body = new char[contentLength];
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
    private static void handlePost(BufferedReader buffer,Socket client)
    {
        Wrapper.run(()->{
            String body = readPostBody(buffer);
            HashMap<String,String> data = parseData(body);
            body = mapOutput(data);
            
            byte[] bytes = body.getBytes();
            String headers =
                    "HTTP/1.1 200 OK\r\n" +
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
    private static void handleClient(Socket client)
    {
        Wrapper.run(()->{
                BufferedReader Buffer = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String site = Buffer.readLine();
                System.out.println(site);
                String[] parts = site.split(" ");
                String method = parts[0];
                String route = parts[1];
                Handler router = new Handler();
                addRoutes(router);
                if(method.equals("GET"))
                {
                    String filePath = router.resolve(route);
                    showUI(filePath, client);

                }
                else if(method.equals("POST"))
                {
                    handlePost(Buffer, client);
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