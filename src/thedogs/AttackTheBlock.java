/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package thedogs;

import corbastuff.KeyboardHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Selvyn
 */
public class AttackTheBlock implements Runnable
{
    static  private final    String  itsPathCommand = "-path:";
    static  private final    String  itsDebugCommand = "-debug:";
    static  private final    String  itsThreadsCommand = "-threads:";
    static  private final    String  itsCountCommand = "-count:";
    static  private final    String  itsHelpCommand = "-help";
    static  private String  itsPath = "http://localhost:8080/";
    static  private int     itsNoOfThreads = 5;
    static  private boolean fDebugOn = false; //toggles debugging
    static  private int     itsCount = 1000;
    static  private int     itsDebugLevel = 0; // 0-none, 1-medium, 2-verbose

    private ArrayList<URLComponents> fileMappings = new ArrayList<URLComponents>();
    protected URL  fDefaultURL = null;//used as default URL
    private boolean keepRunning = true;
    
    private static  AttackTheBlock itsUrlParser = null;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        itsUrlParser = new AttackTheBlock();
        parseParams(args);
        
        ExecutorService executor = Executors.newFixedThreadPool(itsNoOfThreads);
        for (int i = 0; i < itsNoOfThreads; i++) 
        {
            Runnable worker = new AttackTheBlock();
            executor.execute(worker);
        }
        //itsUrlParser.waitForShutdownCommand();
        
        executor.shutdown();

        while (!executor.isTerminated()) 
        {
        }
        System.out.println("Finished all threads");
    }
    
    static  void    parseParams( String args[] )
    {
        for (String arg : args)
        {
            int idx = arg.indexOf(itsPathCommand);
            if( idx > -1 )
            {
                String tempServerId = arg.substring(idx + itsPathCommand.length());
                if( tempServerId.length() > 0 )
                    itsPath = tempServerId;
            }
            idx = arg.indexOf(itsThreadsCommand);
            if(idx > -1)
            {
                String tempInstanceValue = arg.substring(idx + itsThreadsCommand.length());
                if( tempInstanceValue.length() > 0 )
                {
                    itsNoOfThreads = Integer.parseInt(tempInstanceValue);
                }
            }
            idx = arg.indexOf(itsDebugCommand);
            if(idx > -1)
            {
                String tempDebugValue = arg.substring(idx + itsDebugCommand.length());
                if( tempDebugValue.length() > 0 )
                {
                    itsDebugLevel = Integer.parseInt(tempDebugValue);
                }
            }
            idx = arg.indexOf(itsCountCommand);
            if(idx > -1)
            {
                String tempInstanceValue = arg.substring(idx + itsCountCommand.length());
                if( tempInstanceValue.length() > 0 )
                {
                    itsCount = Integer.parseInt(tempInstanceValue);
                }
            }
            idx = arg.indexOf(itsHelpCommand);
            if(idx > -1)
            {
                showHelp();
                System.exit(0);
            }
        }
    }
    
    static  public  void    showHelp()
    {
        System.out.println( "java -jar attacktheblock.jar [options]");
        System.out.println( "options:\n" +
        "-threads:#    - # is number of threads, default is 5\n" + 
        "-count:#      - # number of times each thread hits the server, default is 1000, -1 is forever\n" +
        "-path:url     - url the inet path to the server, default is http://localhost:8080\n" +
        "-debug:#      - default is 0, 0-none, 1-medium, 2-verbose");    
    }

    public  void waitForShutdownCommand()
    {
        Thread tt = new Thread( KeyboardHandler.getInstance() );
        
        tt.start();
        
        System.out.println("Waiting for a quit command");
        synchronized( KeyboardHandler.getInstance().itsMonitor )
        {
            try 
            {
                KeyboardHandler.getInstance().itsMonitor.wait();
            } 
            catch (InterruptedException ex) 
            {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Application is shutting down");
        keepRunning = false;
        System.out.println("Application has quit...");
    }


     public  ArrayList<URLComponents> getFileMappings()
    {
        return fileMappings;
    }
    
    public  String  getFilename( String fullPath  )
    {
        int pos = fullPath.lastIndexOf('/', fullPath.length());
        String newString = fullPath.substring(pos+1);
        
        if( newString.contains("."))
            return newString;
        else
            return "";
    }
    
    public  String  getPath( String fullPath )
    {
        //int pos = fullPath.lastIndexOf('/', fullPath.length());
        //String result = fullPath.substring(0, pos+1);
        int pos = fullPath.lastIndexOf('/', fullPath.length());
        String fileName = fullPath.substring(pos+1);
        String path = fullPath.substring(0, pos+1);
        
        if( ! fileName.contains(".") && fullPath.charAt(fullPath.length()-1) != '/')
            path = fullPath + "/";
        
        return path;
    }
    
    public  URLComponents   getURLComponents( String fullPath )
    {
        URLComponents components = new URLComponents();
        int pos = fullPath.lastIndexOf('/', fullPath.length());
        String fileName = fullPath.substring(pos+1);
        components.path = fullPath.substring(0, pos+1);
        
        if( !fileName.contains(".") && fullPath.charAt(fullPath.length()-1) != '/')
            components.path = fullPath + "/";
        else
        {
            components.fileName = fileName;  
            components.savedFileName = fileName;
        }
        
        //if( (fileName.length() < 1) && (components.path.length() != fullPath.length()))
        //    components.path = fullPath + "/";
        
        return components;
    }
    
    public  String  standardHttpURL( String fullPath )
    {
        if( ! fullPath.contains("http://"))
            return "http://" + fullPath;
        
        return  fullPath;
    }
    
    /**
     * Load a document given a URL string, sends the output a file with same as 
     * what is given at the end of the URL, if no name is given, it will generate
     * a name
     *
     * @param specStr A String containing the URL to load: i.e.
     * "http://www.rawthought.com/"
     */
     public  void    loadURL( String specStr, URL tempURL )
    {
        InputStream tmpInputStream;//stream from which to read data

        try
        {
            //create new URL using fDefaultURL as the base or default URL
            HttpURLConnection conn = (HttpURLConnection)tempURL.openConnection();
            
            if (itsDebugLevel > 0)
            {
                System.out.println("Opening input stream...");
            }
            //open the connection, get InputStream from which to read
            //content data

            // These three lines should force a HTTP GET
            conn.setRequestMethod("GET");

            tmpInputStream = conn.getInputStream();
            tmpInputStream.close();
        } 
        catch (MalformedURLException murlEx)
        {
            if (fDebugOn)
                System.err.println("new URL threw ex: " + murlEx);
        } 
        catch (IOException ex)
        {
            if (fDebugOn)
                Logger.getLogger(AttackTheBlock.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch( ClassCastException ex )
        {
            if (fDebugOn)
                Logger.getLogger(AttackTheBlock.class.getName()).log(Level.SEVERE, null, ex);
        }   
    } // loadURL    

    @Override
    public void run()
    {
        try
        {
            int idx = itsCount;
            URL tempURL = new URL(itsPath);
            //URLConnection urlconn = tempURL.openConnection();
            
            if (itsDebugLevel > 1)
            {
                System.out.println("protocol: " + tempURL.getProtocol());
                System.out.println("host: " + tempURL.getHost());
                System.out.println("port: " + tempURL.getPort());
                System.out.println("path: " + getPath( tempURL.getPath()));
                System.out.println("file path: " + tempURL.getFile());
                System.out.println("filename: " + getFilename( tempURL.getFile()));
            }

            if( itsCount == -1 )
            {
                while( true )
                    loadURL(itsPath, tempURL );
            }
            else
            {
                while( idx-- > 0 )
                    loadURL(itsPath, tempURL );
            }
        } catch (MalformedURLException ex)
        {
            Logger.getLogger(AttackTheBlock.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
