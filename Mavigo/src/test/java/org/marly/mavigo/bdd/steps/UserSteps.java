package org.marly.mavigo.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class UserSteps {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private User currentUser;
    private ComfortProfile updatedProfile;
    private Exception lastException;

    @Before
    public void setUp() {
        currentUser = null;
        updatedProfile = null;
        lastException = null;
    }

    @Given("je suis un nouvel utilisateur")
    public void je_suis_un_nouvel_utilisateur() {
        String uniqueEmail = "new-user-" + UUID.randomUUID() + "@test.com";
        currentUser = new User("ext-new-" + UUID.randomUUID(), uniqueEmail, "New User");
    }

    @Given("je suis un utilisateur existant avec l'email {string}")
    public void je_suis_un_utilisateur_existant_avec_email(String email) {
        currentUser = userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User("ext-existing-" + UUID.randomUUID(), email, "Existing User");
            return userRepository.save(user);
        });
    }

    @When("je crée mon compte")
    public void je_cree_mon_compte() {
        try {
            currentUser = userRepository.save(currentUser);
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("je mets à jour mon profil avec un maximum de {int} correspondances")
    public void je_mets_a_jour_mon_profil_avec_max_correspondances(int maxTransfers) {
        try {
            ComfortProfile profile = currentUser.getComfortProfile();
            profile.setMaxNbTransfers(maxTransfers);
            currentUser.setComfortProfile(profile);
            currentUser = userRepository.save(currentUser);
            updatedProfile = currentUser.getComfortProfile();
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("je mets à jour mon profil de confort avec accès fauteuil roulant")
    public void je_mets_a_jour_mon_profil_de_confort_avec_acces_fauteuil_roulant() {
        try {
            ComfortProfile profile = currentUser.getComfortProfile();
            profile.setWheelchairAccessible(true);
            currentUser.setComfortProfile(profile);
            currentUser = userRepository.save(currentUser);
            updatedProfile = currentUser.getComfortProfile();
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("mon compte devrait être créé avec succès")
    public void mon_compte_devrait_etre_cree() {
        assertNull(lastException, "Une erreur s'est produite: " +
                (lastException != null ? lastException.getMessage() : ""));
        assertNotNull(currentUser);
        assertNotNull(currentUser.getId());
    }

    @Then("je devrais avoir un profil de confort par défaut")
    public void je_devrais_avoir_un_profil_de_confort_par_defaut() {
        assertNotNull(currentUser.getComfortProfile());
    }

    @Then("mon profil devrait avoir {int} correspondances maximum")
    public void mon_profil_devrait_avoir_n_correspondances_maximum(int expectedMax) {
        assertNotNull(updatedProfile);
        assertEquals(expectedMax, updatedProfile.getMaxNbTransfers());
    }

    @Then("mon profil devrait indiquer l'accès fauteuil roulant")
    public void mon_profil_devrait_indiquer_l_acces_fauteuil_roulant() {
        assertNotNull(updatedProfile);
        assertTrue(updatedProfile.getWheelchairAccessible());
    }

    @Then("mes préférences devraient être sauvegardées")
    public void mes_preferences_devraient_etre_sauvegardees() {
        assertNotNull(currentUser.getId());
        User savedUser = userRepository.findById(currentUser.getId()).orElse(null);
        assertNotNull(savedUser);
        assertNotNull(savedUser.getComfortProfile());
    }
}
