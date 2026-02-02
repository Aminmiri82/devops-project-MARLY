package org.marly.mavigo.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.disruption.DisruptionType;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.DisruptionRepository;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class DisruptionSteps {

    @Autowired
    private DisruptionRepository disruptionRepository;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private UserRepository userRepository;

    private User currentUser;
    private Journey currentJourney;
    private Disruption reportedDisruption;
    private List<Disruption> activeDisruptions;
    private Exception lastException;

    @Before
    public void setUp() {
        currentUser = null;
        currentJourney = null;
        reportedDisruption = null;
        activeDisruptions = null;
        lastException = null;
    }

    @Given("un utilisateur avec un trajet actif sur la ligne {string}")
    public void un_utilisateur_avec_trajet_actif_sur_ligne(String lineCode) {
        currentUser = userRepository.findByEmail("disruption-test@example.com").orElseGet(() -> {
            User user = new User("ext-disruption-" + UUID.randomUUID(), "disruption-test@example.com",
                    "Disruption Test User");
            return userRepository.save(user);
        });

        currentJourney = new Journey(
                currentUser,
                "Gare de Lyon",
                "Châtelet",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));

        JourneySegment segment = new JourneySegment(currentJourney, 1, SegmentType.PUBLIC_TRANSPORT);
        segment.setLineCode(lineCode);
        segment.setLineName("Métro " + lineCode);
        currentJourney.addSegment(segment);

        currentJourney = journeyRepository.save(currentJourney);
    }

    @Given("une disruption existante sur la ligne {string}")
    public void une_disruption_existante_sur_ligne(String lineCode) {
        Disruption disruption = new Disruption();
        disruption.setDisruptionType(DisruptionType.LINE);
        disruption.setAffectedLineCode(lineCode);
        disruption.setEffectedLine(lineCode);
        disruption.setCreatedAt(LocalDateTime.now());
        disruption.setValidUntil(LocalDateTime.now().plusHours(2));
        disruptionRepository.save(disruption);
    }

    @When("je signale une perturbation sur la ligne {string}")
    public void je_signale_une_perturbation_sur_ligne(String lineCode) {
        try {
            reportedDisruption = Disruption.lineDisruption(currentJourney, lineCode, currentUser);
            reportedDisruption = disruptionRepository.save(reportedDisruption);
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("je signale une perturbation à la station {string}")
    public void je_signale_une_perturbation_a_station(String stationId) {
        try {
            reportedDisruption = Disruption.stationDisruption(currentJourney, stationId, currentUser);
            reportedDisruption = disruptionRepository.save(reportedDisruption);
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("je consulte les perturbations actives")
    public void je_consulte_les_perturbations_actives() {
        activeDisruptions = disruptionRepository.findByValidUntilAfter(LocalDateTime.now());
    }

    @Then("la perturbation devrait être enregistrée")
    public void la_perturbation_devrait_etre_enregistree() {
        assertNull(lastException, "Une erreur s'est produite: " +
                (lastException != null ? lastException.getMessage() : ""));
        assertNotNull(reportedDisruption);
        assertNotNull(reportedDisruption.getId());
    }

    @Then("la perturbation devrait être de type {string}")
    public void la_perturbation_devrait_etre_de_type(String type) {
        assertNotNull(reportedDisruption);
        assertEquals(DisruptionType.valueOf(type), reportedDisruption.getDisruptionType());
    }

    @Then("je devrais voir la perturbation sur la ligne {string}")
    public void je_devrais_voir_la_perturbation_sur_ligne(String lineCode) {
        assertNotNull(activeDisruptions);
        assertTrue(activeDisruptions.stream()
                .anyMatch(d -> lineCode.equals(d.getAffectedLineCode())));
    }

    @Then("la perturbation devrait être liée à mon trajet")
    public void la_perturbation_devrait_etre_liee_a_mon_trajet() {
        assertNotNull(reportedDisruption);
        assertEquals(currentJourney.getId(), reportedDisruption.getJourney().getId());
    }

    @Then("je devrais voir au moins {int} perturbation(s) active(s)")
    public void je_devrais_voir_au_moins_n_perturbations_actives(int minDisruptions) {
        assertNotNull(activeDisruptions);
        assertTrue(activeDisruptions.size() >= minDisruptions);
    }
}
