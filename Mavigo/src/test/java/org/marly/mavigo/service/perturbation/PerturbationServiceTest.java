@SpringBootTest
class PerturbationServiceTest {

    @Autowired
    PerturbationService service;

    @Test
    void testFetchPerturbations() {
        var result = service.fetchPerturbations();
        assertNotNull(result);
    }
}
