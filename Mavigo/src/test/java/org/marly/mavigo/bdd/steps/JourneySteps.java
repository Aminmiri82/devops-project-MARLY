package org.marly.mavigo.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.journey.JourneyPlanningService;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class JourneySteps {

    @Autowired
    private JourneyPlanningService journeyPlanningService;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private UserRepository userRepository;

    private User currentUser;
    private List<Journey> plannedJourneys;
    private Exception lastException;

    @Before
    public void setUp() {
        plannedJourneys = null;
        lastException = null;
    }

    @Given("un utilisateur connecté avec l'email {string}")
    public void un_utilisateur_connecte_avec_email(String email) {
        currentUser = userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User("ext-" + UUID.randomUUID(), email, "Test User");
            return userRepository.save(user);
        });
    }

    @Given("un utilisateur existant dans le système")
    public void un_utilisateur_existant_dans_le_systeme() {
        currentUser = userRepository.findByEmail("bdd-test@example.com").orElseGet(() -> {
            User user = new User("ext-bdd-" + UUID.randomUUID(), "bdd-test@example.com", "BDD Test User");
            return userRepository.save(user);
        });
    }

    @When("je planifie un trajet de {string} à {string}")
    public void je_planifie_un_trajet_de_a(String origin, String destination) {
        try {
            JourneyPlanningParameters params = new JourneyPlanningParameters(
                    currentUser.getId(),
                    origin,
                    destination,
                    LocalDateTime.now().plusHours(1),
                    JourneyPreferences.disabled(),
                    false);
            plannedJourneys = journeyPlanningService.planAndPersist(params);
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("je planifie un trajet de {string} à {string} avec le mode confort activé")
    public void je_planifie_un_trajet_avec_mode_confort(String origin, String destination) {
        try {
            JourneyPreferences preferences = new JourneyPreferences(true, false, null);
            JourneyPlanningParameters params = new JourneyPlanningParameters(
                    currentUser.getId(),
                    origin,
                    destination,
                    LocalDateTime.now().plusHours(1),
                    preferences,
                    false);
            plannedJourneys = journeyPlanningService.planAndPersist(params);
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("je devrais recevoir au moins {int} option(s) de trajet")
    public void je_devrais_recevoir_au_moins_n_options(int minOptions) {
        assertNull(lastException, "Une erreur inattendue s'est produite: " +
                (lastException != null ? lastException.getMessage() : ""));
        assertNotNull(plannedJourneys, "Aucun trajet n'a été planifié");
        assertTrue(plannedJourneys.size() >= minOptions,
                "Attendu au moins " + minOptions + " options, reçu " + plannedJourneys.size());
    }

    @Then("le trajet devrait avoir le statut {string}")
    public void le_trajet_devrait_avoir_le_statut(String statusName) {
        assertNotNull(plannedJourneys);
        assertFalse(plannedJourneys.isEmpty());
        JourneyStatus expectedStatus = JourneyStatus.valueOf(statusName);
        assertEquals(expectedStatus, plannedJourneys.get(0).getStatus());
    }

    @Then("le trajet devrait avoir le mode confort activé")
    public void le_trajet_devrait_avoir_le_mode_confort_active() {
        assertNotNull(plannedJourneys);
        assertFalse(plannedJourneys.isEmpty());
        assertTrue(plannedJourneys.get(0).isComfortModeEnabled());
    }

    @Then("une erreur devrait être levée avec le message contenant {string}")
    public void une_erreur_devrait_etre_levee_avec_message(String messageFragment) {
        assertNotNull(lastException, "Aucune exception n'a été levée");
        assertTrue(lastException.getMessage().contains(messageFragment),
                "Le message d'erreur ne contient pas '" + messageFragment + "': " + lastException.getMessage());
    }

    @Then("le trajet devrait être sauvegardé en base de données")
    public void le_trajet_devrait_etre_sauvegarde() {
        assertNotNull(plannedJourneys);
        assertFalse(plannedJourneys.isEmpty());

        Journey firstJourney = plannedJourneys.get(0);
        assertNotNull(firstJourney.getId());

        assertTrue(journeyRepository.findById(firstJourney.getId()).isPresent());
    }
}
