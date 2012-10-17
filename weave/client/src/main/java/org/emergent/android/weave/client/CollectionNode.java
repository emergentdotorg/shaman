package org.emergent.android.weave.client;

/**
* @author Patrick Woodworth
*/
public enum CollectionNode {

  STORAGE_CLIENTS("clients"),
  STORAGE_BOOKMARKS("bookmarks"),
  STORAGE_PASSWORDS("passwords"),
  ;

  public final String engineName;
  public final String nodePath;

  CollectionNode(String engineName) {
    this.engineName = engineName;
    this.nodePath = "/storage/" + this.engineName;
  }
}
