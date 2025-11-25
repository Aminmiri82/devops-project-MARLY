class IDFMClientTest {

    @Test
    void testFetch() {
        IDFMClient client = new IDFMClient("FAKE_KEY");
        var result = client.fetchPerturbations();
        assertNotNull(result);
    }
}
