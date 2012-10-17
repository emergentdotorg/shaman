package org.emergent.android.weave.client;

/**
* @author Patrick Woodworth
*/
public enum HashNode {

  INFO_COLLECTIONS(false, "/info/collections"),
  META_GLOBAL(false, "/storage/meta/global"),
  ;

  public final boolean userServer;
  public final String nodePath;

  HashNode(boolean userServer, String path) {
    this.userServer = userServer;
    this.nodePath = path;
  }
}
