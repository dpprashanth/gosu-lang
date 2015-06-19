/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.properties;

import gw.fs.IFile;
import gw.lang.reflect.IPropertyInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeInfo;
import gw.lang.reflect.ITypeLoader;
import gw.lang.reflect.TypeBase;
import gw.lang.reflect.gs.IPropertiesType;
import gw.util.GosuClassUtil;
import gw.util.concurrent.LockingLazyVar;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

/**
 * Type based on a {@link PropertyNode}
 */
@SuppressWarnings("serial")
public class PropertiesType extends TypeBase implements IPropertiesType {

  private final PropertiesTypeLoader _typeLoader;
  private final PropertyNode _propertyNode;
  private IFile _file;
  private String _contentCached;
  private final LockingLazyVar<PropertiesTypeInfo> _typeInfo = new LockingLazyVar<PropertiesTypeInfo>() {
    @Override
    protected PropertiesTypeInfo init() {
      return new PropertiesTypeInfo(PropertiesType.this);
    }
  };
  
  public PropertiesType(PropertiesTypeLoader typeLoader, PropertyNode propertyNode, IFile file) {
    _typeLoader = typeLoader;
    _propertyNode = propertyNode;
    _file = file;
  }

  @Override
  public IType[] getInterfaces() {
    return EMPTY_TYPE_ARRAY;
  }

  @Override
  public String getName() {
    return _propertyNode.getTypeName();  
  }

  @Override
  public String getNamespace() {
    return GosuClassUtil.getPackage( getName() );
  }

  @Override
  public String getRelativeName() {
    return GosuClassUtil.getShortClassName( getName() );
  }

  @Override
  public IType getSupertype() {
    return null;
  }

  @Override
  public ITypeInfo getTypeInfo() {
    return _typeInfo.get();
  }

  @Override
  public ITypeLoader getTypeLoader() {
    return _typeLoader;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  PropertyNode getPropertyNode() {
    return _propertyNode;
  }

  @Override
  public String getPropertiesFileKey( IPropertyInfo pi )
  {
    PropertiesPropertyInfo ppi = (PropertiesPropertyInfo)pi;
    return ppi.getPropertyEntryName();
  }

  @Override
  public IFile[] getSourceFiles() {
    if( _file == null ) {
      // _file may be null e.g., for SystemProperties
      return IFile.EMPTY_ARRAY;
    }
    return new IFile[] {_file};
  }

  public int findOffsetOf( PropertyNode node )
  {
    String fqn = node.getPath();
    // this is a crappy way to approximate the offset, we really need to parse the file ourselves and store the offsets
    return getCachedContent().indexOf( fqn );
  }

  public String getCachedContent()
  {
    if( _contentCached == null )
    {
      IFile[] files = getSourceFiles();
      if( files != null && files.length > 0 )
      {
        try
        {
          StringBuilder sb = new StringBuilder();
          List<String> lines = Files.readAllLines( files[0].toJavaFile().toPath(), Charset.defaultCharset() );
          for( String line: lines )
          {
            sb.append( line ).append( "\n" );
          }
          _contentCached = sb.toString();
            //new Scanner( files[0].toJavaFile() ).useDelimiter( "\\Z" ).next();
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }
    }
    return _contentCached;
  }
}
