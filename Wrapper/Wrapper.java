package Wrapper;
public class Wrapper {
    @FunctionalInterface
    public interface ThrowingTask 
    {
        void excecute() throws Exception;
        
    }
    public static void run(ThrowingTask task)
    {
        try
        {
            task.excecute();
        }
        catch(Exception e)
        {
            e.getLocalizedMessage();
        }
    }
}
