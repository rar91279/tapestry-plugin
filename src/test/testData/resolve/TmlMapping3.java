class TestModule {
  @Contribute( ComponentClassResolver.class )
  public static void setupLibraryMapping(Configuration<LibraryMapping> configuration)
  {
    configuration.add(new org.apache.tapestry5.services.LibraryMapping("wf", "dk.nesluop.librarymapping.framework"));
  }
}
