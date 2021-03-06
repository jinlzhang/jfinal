package com.jfsnpm.jfsnpm.plugin.sqlinxml;


import com.jfinal.log.Log;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.bind.JAXB;

public class SqlKit {

  protected static final Log LOG = Log.getLog(SqlKit.class);

  private static Map<String, String> sqlMap;

  public static String sql(String groupNameAndsqlId) {
    if (sqlMap == null) {
      throw new NullPointerException("SqlInXmlPlugin not start");
    }
    return sqlMap.get(groupNameAndsqlId);
  }

  static void clearSqlMap() {
    sqlMap.clear();
  }

  static void init() {
    sqlMap = new HashMap<String, String>();
    //加载sql文件
    Enumeration<URL> baseURLs = null;
    try {
      baseURLs = SqlKit.class.getClassLoader().getResources("sql");

      if (baseURLs == null) {
        baseURLs = SqlKit.class.getClassLoader().getResources("");
      }
      URL url = null;
      while (baseURLs.hasMoreElements()) {
        url = baseURLs.nextElement();
        loadFilePath(url);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    LOG.debug("sqlMap" + sqlMap);
  }

  static void init(String... paths) {
    sqlMap = new HashMap<String, String>();

    for (String path : paths) {
      if (path.startsWith("/")) {
        path += path.substring(1);
      }
      Enumeration<URL> baseURLs = null;
      try {
        baseURLs = SqlKit.class.getClassLoader().getResources(path);

        if (baseURLs == null) {
          baseURLs = SqlKit.class.getClassLoader().getResources("");
        }
        URL url = null;
        while (baseURLs.hasMoreElements()) {
          url = baseURLs.nextElement();
          loadFilePath(url);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    LOG.debug("sqlMap" + sqlMap);
  }

  private static void loadFilePath(URL baseURL) {
    if (baseURL != null) {
      String protocol = baseURL.getProtocol();
      String basePath = baseURL.getFile();
      if ("jar".equals(protocol)) {
        String[] pathurls = basePath.split("!/");
        // 获取jar
        try {
          loadJarFileList(URLDecoder.decode(pathurls[0].replace("file:", ""), "UTF-8"), pathurls[1]);
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        //加载sql文件
        loadFileList(basePath);
      }
    }
  }

  public static void loadFileList(String strPath) {
    List<File> files = new ArrayList<File>();
    File dir = new File(strPath);
    File[] dirs = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        if (pathname.getName().endsWith("sql") || pathname.getName().endsWith("sql.xml")) {
          return true;
        }
        return false;
      }
    });

    if (dirs == null)
      return;
    for (int i = 0; i < dirs.length; i++) {
      if (dirs[i].isDirectory()) {
        loadFileList(dirs[i].getAbsolutePath());
      } else {
        if (dirs[i].getName().endsWith("sql.xml")) {
          files.add(dirs[i]);
        }
      }
    }
    //加载sql文件
    loadFiles(files);
  }


  /**
   * find jar file
   *
   * @param filePath    文件路径
   * @param packageName 包名
   * @return list
   * @throws java.io.IOException 文件读取异常
   */
  private static void loadJarFileList(String filePath, String packageName) throws IOException {
    Map<String, InputStream> sqlFiles = new HashMap<String, InputStream>();
    JarFile localJarFile = new JarFile(new File(filePath));
    sqlFiles = findInJar(localJarFile, packageName);
    //加载sql文件
    loadStreamFiles(sqlFiles);
    localJarFile.close();
  }

  /**
   * 从jar里加载文件
   *
   * @param localJarFile 文件路径
   * @param packageName  包名
   * @return
   */
  private static Map<String, InputStream> findInJar(JarFile localJarFile, String packageName) {
    Map<String, InputStream> sqlFiles = new HashMap<String, InputStream>();
    Enumeration<JarEntry> entries = localJarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry jarEntry = entries.nextElement();
      String entryName = jarEntry.getName();
      if (!jarEntry.isDirectory() && (packageName == null || entryName.startsWith(packageName)) && entryName.endsWith("sql.xml")) {
        sqlFiles.put(entryName.substring(entryName.lastIndexOf("/") + 1), SqlKit.class.getResourceAsStream(File.separator + entryName));
      }
    }
    return sqlFiles;
  }

  /**
   * 加载xml文件
   *
   * @param files
   */
  private static void loadFiles(List<File> files) {
    for (File xmlfile : files) {
      SqlRoot root = JAXB.unmarshal(xmlfile, SqlRoot.class);
      
      for (SqlGroup sqlGroup : root.sqlGroups) {

        getSql(xmlfile.getName(), sqlGroup);
      }
    }
  }

  private static void loadStreamFiles(Map<String, InputStream> streams) {
    for (String filename : streams.keySet()) {
      SqlRoot root = JAXB.unmarshal(streams.get(filename), SqlRoot.class);
      for (SqlGroup sqlGroup : root.sqlGroups) {

        getSql(filename, sqlGroup);
      }
    }
  }


  private static void getSql(String filename, SqlGroup sqlGroup) {
    String name = sqlGroup.name;
    if (name == null || name.trim().equals("")) {
      name = filename;
    }
    for (SqlItem sqlItem : sqlGroup.sqlItems) {
      sqlMap.put(name + "." + sqlItem.id, sqlItem.value);
    }
  }
}
