import RouteHandler.Handler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import Wrapper.Wrapper;

public class httpServer
{
    private static void addRoutes(Handler router)
    {
        Wrapper.run(()->{
            router.addRoute("/", "static/index.html");
            router.addRoute("/about", "static/about.html");
        });
    }
    private static void showUI(String filePath,Socket client)
    {
        Wrapper.run(()->{
            Path path = Path.of(filePath);
            String body = Files.readString(path);
            String response =
                            "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " +body.getBytes().length+"\r\n"+
                            "\r\n" +
                            body; 
            OutputStream out = client.getOutputStream();
            out.write(response.getBytes());
            out.flush();
            client.close();
        });
    }
    
    public static void main(String[] args) 
    {
         
        Wrapper.run(()->{
            ServerSocket server = new ServerSocket(8080);
            while(true)
            {
                Socket client = server.accept();
                BufferedReader Buffer = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String site = Buffer.readLine();
                String[] parts = site.split(" ");
                String method = parts[0];
                String route = parts[1];
                Handler router = new Handler();
                addRoutes(router);
                String filePath = router.resolve(route);
                showUI(filePath, client);
            }
            

        });
        

       
        
    }
}