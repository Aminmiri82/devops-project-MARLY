WebMvcTest(PerturbationController.class)
class PerturbationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetPerturbations() throws Exception {
        mockMvc.perform(get("/api/perturbations"))
               .andExpect(status().isOk());
    }
}
