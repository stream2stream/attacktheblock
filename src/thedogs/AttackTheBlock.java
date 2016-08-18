/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package thedogs;

import corbastuff.KeyboardHandler;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
                if( tempDebugValue.equalsIgnoreCase("on") )
                {
                    fDebugOn = true;
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
        "-count:#      - # number of times each thread hits the server, default is 1000\n" +
        "-path:url     - url the inet path to the server, default is http://localhost:8080\n" +
        "-debug:on|off - default is off, on is verbose output, processing is slower");    
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
     public  void    loadURL( String specStr )
    {
        loadURL( specStr, null );
    }
     
    public void loadURL(String specStr, String outputFileName )
    {
        URL tempURL;//the URL to be loaded
        InputStream tempInputStream;//stream from which to read data
        int curByte; //used for copying from input stream to a file

        try
        {
            //create new URL using fDefaultURL as the base or default URL
            tempURL = new URL(specStr);

            if (fDebugOn)
            {
                System.out.println("protocol: " + tempURL.getProtocol());
                System.out.println("host: " + tempURL.getHost());
                System.out.println("port: " + tempURL.getPort());
                System.out.println("path: " + getPath( tempURL.getPath()));
                System.out.println("file path: " + tempURL.getFile());
                System.out.println("filename: " + getFilename( tempURL.getFile()));
            }

            try
            {
                if (fDebugOn)
                {
                    System.out.println("Opening input stream...");
                }
                //open the connection, get InputStream from which to read
                //content data
                tempInputStream = tempURL.openStream();

                //if we get to this point without an exception being thrown,
                //then we've connected to a valid Web server,
                // requested a valid URL, and there's content
                //data waiting for us on the InputStream
                try
                {
                    //use URL.hashCode() to generate a unique filename if no file name specified
                    URLComponents uc = getURLComponents( specStr );

                    if( uc.fileName.length() < 1 )
                    {
                        uc.savedFileName = String.valueOf(tempURL.hashCode()) + ".html";
                    }
                    fileMappings.add( uc );

                    if( outputFileName == null )
                        outputFileName = uc.savedFileName;
                    else
                        uc.savedFileName = outputFileName;
                    
                    if (fDebugOn)
                    {
                        System.out.println(
                                "Opening output file: " + outputFileName);
                    }
                    //open output file
                    OutputStream outstream = new NullOutputStream();

                    if (fDebugOn)
                    {
                        System.out.println("Copying Data...");
                    }
                    try
                    {
                        while ((curByte = tempInputStream.read()) != -1)
                        {
                            //simple byte-for-byte copy...could be improved!
                            outstream.write(curByte);
                        }
                        if (fDebugOn)
                        {
                            System.out.println("Done Downloading Content!");
                        }
                        //we're done writing to the local file, so close it
                        outstream.close();
                    } catch (IOException copyEx)
                    {
                        System.err.println("copyEx: " + copyEx);
                    }
                } catch (Exception fileOpenEx)
                {
                    System.err.println("fileOpenEx: " + fileOpenEx);
                }
            } catch (IOException retrieveEx)
            {
                System.err.println("retrievEx: " + retrieveEx);
            }
        } catch (MalformedURLException murlEx)
        {
            System.err.println("new URL threw ex: " + murlEx);
        }
    } // loadURL    

    @Override
    public void run()
    {
        int idx = itsCount;
        
        if( itsCount == -1 )
        {
            while( true )
                loadURL(itsPath);
        }
        else
        {
            while( idx-- > 0 )
                loadURL(itsPath);
        }
    }


}
