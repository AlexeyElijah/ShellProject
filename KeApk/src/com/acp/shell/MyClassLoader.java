package com.acp.shell;


import java.io.InputStream;

import dalvik.system.DexClassLoader;

public class MyClassLoader extends DexClassLoader {

	private ClassLoader mClassLoader = null;

	  public MyClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent)
	  {
	    super(dexPath, optimizedDirectory, librarySearchPath, parent.getParent());
	    this.mClassLoader = parent;
	  }

	  public InputStream getResourceAsStream(String paramString)
	  {
	    return this.mClassLoader.getResourceAsStream(paramString);
	  }
	@Override
	protected Class<?> loadClass(String className, boolean resolve)
			throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return super.loadClass(className, resolve);
	}

	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		return super.loadClass(className);
	}
   
}
