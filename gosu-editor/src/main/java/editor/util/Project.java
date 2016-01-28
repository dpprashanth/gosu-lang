package editor.util;

import editor.FileWatcher;
import editor.GosuPanel;
import editor.tabpane.TabPane;
import gw.lang.reflect.TypeSystem;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 */
public class Project
{
  private String _name;
  private List<String> _sourcePath;
  private File _projectDir;
  private List<String> _openFiles;
  private String _activeFile;
  private GosuPanel _gosuPanel;

  public Project( String name, File dir, GosuPanel gosuPanel )
  {
    _name = name;
    _gosuPanel = gosuPanel;
    _sourcePath = Arrays.asList( new File( dir, getRelativeGosuSourcePath() ).getAbsolutePath() );
    _projectDir = dir;
    //noinspection ResultOfMethodCallIgnored
    _projectDir.mkdirs();
    _openFiles = Collections.emptyList();
  }

  public Project( File dir, GosuPanel gosuPanel )
  {
    _name = dir.getName();
    _projectDir = dir;
    _gosuPanel = gosuPanel;
    load();
    FileWatcher.instance( this );
  }

  public String getName()
  {
    return _name;
  }
  public void setName( String name )
  {
    _name = name;
  }

  public List<String> getSourcePath()
  {
    return _sourcePath;
  }
  public void setSourcePath( List<String> classpath )
  {
    _sourcePath = classpath;
  }

  public File getProjectDir()
  {
    return _projectDir;
  }
  public void setProjectDir( File projectDir )
  {
    _projectDir = projectDir;
  }

  public List<String> getOpenFiles()
  {
    return _openFiles;
  }

  public String getActiveFile()
  {
    return _activeFile;
  }

  public GosuPanel getGosuPanel()
  {
    return _gosuPanel;
  }

  public File getOrMakeProjectFile()
  {
    File projectDir = getProjectDir();
    //noinspection ResultOfMethodCallIgnored
    projectDir.mkdirs();
    File project = new File( projectDir, projectDir.getName() + ".prj" );
    if( project.isFile() )
    {
      return project;
    }
    //noinspection ResultOfMethodCallIgnored
    try( FileWriter writer = new FileWriter( project ) )
    {
      Properties props = new Properties();
      props.put( "Name", getName() );
      if( getSourcePath() != null )
      {
        for( int i = 0; i < getSourcePath().size(); i++ )
        {
          String sourcePath = getSourcePath().get( i );
          props.put( "Classpath.Entry" + i, sourcePath );
          File sourceDir = new File( sourcePath );
          sourceDir.mkdirs();
        }
      }
      props.store( writer, "Gosu Project: " + getName() );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
    return project;
  }

  public void save( TabPane tabPane )
  {
    File userFile = getOrMakeProjectFile();

    Properties props = new Properties();
    props.put( "Name", getName() );
    props.put( "Tab.Active", makeProjectRelativePath( (File)tabPane.getSelectedTab().getContentPane().getClientProperty( "_file" ) ) );
    for( int i = 0; i < tabPane.getTabCount(); i++ )
    {
      File file = (File)tabPane.getTabAt( i ).getContentPane().getClientProperty( "_file" );
      props.put( "Tab.Open." + ((char)(i + 'A')), makeProjectRelativePath( file ) );
    }

    for( int i = 0; i < getSourcePath().size(); i++ )
    {
      String path = getSourcePath().get( i );
      String relativePath = makeProjectRelativePath( new File( path ) );
      props.put( "Classpath.Entry" + i, relativePath == null ? path : relativePath );
    }

    try
    {
      FileWriter fw = new FileWriter( userFile );
      props.store( fw, "Gosu Project" );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private String makeProjectRelativePath( File file )
  {
    String absProjectDir = getProjectDir().getAbsolutePath();
    String absFile = file.getAbsolutePath();
    if( !absFile.startsWith( absProjectDir ) )
    {
      return null;
    }
    return absFile.substring( absProjectDir.length() + 1 );
  }

  private void load()
  {
    Properties props = new Properties();
    try
    {
      System.setProperty( "user.dir", getProjectDir().getAbsolutePath() );
      props.load( new FileReader( getOrMakeProjectFile() ) );

      setName( props.getProperty( "Name", getProjectDir().getName() ) );

      Set<String> keys = props.stringPropertyNames();
      //noinspection SuspiciousToArrayCall
      String[] sortedKeys = keys.toArray( new String[keys.size()] );
      Arrays.sort( sortedKeys );
      ArrayList<String> sourcePath = new ArrayList<>();
      for( String cpEntry : sortedKeys )
      {
        if( cpEntry.startsWith( "Classpath.Entry" ) )
        {
          File file = new File( props.getProperty( cpEntry ) ).getAbsoluteFile();
          if( file.exists() )
          {
            sourcePath.add( file.getAbsolutePath() );
          }
        }
      }
      _sourcePath = sourcePath;
      if( _sourcePath.isEmpty() )
      {
        File srcPath = new File( getProjectDir(), getRelativeGosuSourcePath() );
        _sourcePath.add( srcPath.getAbsolutePath() );
      }

      TypeSystem.refresh( TypeSystem.getGlobalModule() );

      List<String> openFiles = new ArrayList<>();
      for( String strTab : sortedKeys )
      {
        if( strTab.startsWith( "Tab.Open" ) )
        {
          File file = new File( props.getProperty( strTab ) ).getAbsoluteFile();
          if( file.isFile() )
          {
            openFiles.add( file.getAbsolutePath() );
          }
        }
      }
      _openFiles = openFiles;
      _activeFile = props.getProperty( "Tab.Active" );
      if( _activeFile != null && !_activeFile.isEmpty() )
      {
        _activeFile = new File( _activeFile ).getAbsolutePath();
      }
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public File getOrMakeUntitledProgram()
  {
    File srcDir = new File( getSourcePath().get( 0 ) );
    //noinspection ResultOfMethodCallIgnored
    srcDir.mkdirs();
    File file = new File( srcDir, "Untitled.gsp" );
    try
    {
      //noinspection ResultOfMethodCallIgnored
      file.createNewFile();
      return file;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static String getRelativeGosuSourcePath()
  {
    return "src" + File.separator + "main" + File.separator + "gosu";
  }
}
