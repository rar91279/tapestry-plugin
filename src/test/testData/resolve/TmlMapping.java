import org.apache.tapestry5.services.LibraryMapping;

class TestModule {
  public static void contributeComponentClassResolver(){
    new org.apache.tapestry5.services.LibraryMapping("foo", "com.testapp.components.other");
  }
}
