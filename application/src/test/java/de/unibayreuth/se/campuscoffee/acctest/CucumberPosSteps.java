package de.unibayreuth.se.campuscoffee.acctest;

import de.unibayreuth.se.campuscoffee.domain.ports.PosService;
import de.unibayreuth.se.campuscoffee.api.dtos.PosDto;
import de.unibayreuth.se.campuscoffee.domain.CampusType;
import de.unibayreuth.se.campuscoffee.domain.PosType;
import de.unibayreuth.se.campuscoffee.domain.exceptions.PosNotFoundException;
import io.cucumber.java.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static de.unibayreuth.se.campuscoffee.TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the POS Cucumber tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
public class CucumberPosSteps {
    static final PostgreSQLContainer<?> postgresContainer = getPostgresContainer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        configurePostgresContainers(registry, postgresContainer);
    }

    @Autowired
    protected PosService posService;

    @LocalServerPort
    private Integer port;

    @BeforeAll
    public static void beforeAll() {
        postgresContainer.start();
    }

    @AfterAll
    public static void afterAll() {
        postgresContainer.stop();
    }

    @Before
    public void beforeEach() {
        posService.clear();
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @After
    public void afterEach() {
        posService.clear();
    }

    private List<PosDto> createdPosList;

    @DataTableType
    public PosDto toPosDto(Map<String,String> row) {
        return PosDto.builder()
                .name(row.get("name"))
                .description(row.get("description"))
                .type(PosType.valueOf(row.get("type")))
                .campus(CampusType.valueOf(row.get("campus")))
                .street(row.get("street"))
                .houseNumber(row.get("houseNumber"))
                .postalCode(Integer.parseInt(row.get("postalCode")))
                .city(row.get("city"))
                .build();
    }

    // Given -----------------------------------------------------------------------

    @Given("an empty POS list")
    public void anEmptyPosList() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList).isEmpty();
    }

    // When -----------------------------------------------------------------------

    @When("I insert POS with the following elements")
    public void iInsertPosWithTheFollowingValues(List<PosDto> posList) {
        createdPosList = createPos(posList);
        assertThat(createdPosList).size().isEqualTo(posList.size());
    }

    // Then -----------------------------------------------------------------------

    @Then("the POS list should contain the same elements in the same order")
    public void thePosListShouldContainTheSameElementsInTheSameOrder() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrderElementsOf(createdPosList);
    }

    // Given -----------------------------------------------------------------------

    @Given("I insert three POS with the following elements")
    public void iInsertThreePosWithTheFollowingElements(List<PosDto> posList) {
        assertThat(posList).hasSize(3);
        createdPosList = createPos(posList);
        assertThat(createdPosList).size().isEqualTo(3);
    }
    

    // When -----------------------------------------------------------------------

    @When("I modify the decription of one of them based on its name")
    public void modifyPosDescriptionByName(List<PosDto> posList) {
        assertThat(posList).hasSize(1);
        PosDto posToModify = posList.get(0);
        List<PosDto> retrievedPosList = retrievePos();
        PosDto existingPos = retrievedPosList.stream()
                .filter(pos -> pos.getName().equals(posToModify.getName()))
                .findFirst()
                .orElseThrow(() -> new PosNotFoundException("POS with name " + posToModify.getName() + " not found"));

        // Neues Objekt mit geänderter Beschreibung bauen
        PosDto updatedPos = PosDto.builder()
                .id(existingPos.getId())
                .name(existingPos.getName())
                .description(posToModify.getDescription())
                .type(existingPos.getType())
                .campus(existingPos.getCampus())
                .street(existingPos.getStreet())
                .houseNumber(existingPos.getHouseNumber())
                .postalCode(existingPos.getPostalCode())
                .city(existingPos.getCity())
                .build();

        // updatePos erwartet eine Liste
        updatePos(List.of(updatedPos));
    }

    // Then -----------------------------------------------------------------------

    @Then("the description should be updated")
    public void theDescriptionShouldBeUpdated(List<PosDto> posList) {
        assertThat(posList).hasSize(1);
        PosDto expected = posList.get(0);

        List<PosDto> retrievedPosList = retrievePos();
        PosDto actual = retrievedPosList.stream()
                .filter(pos -> pos.getName().equals(expected.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("POS with name " + expected.getName() + " not found"));

        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    }
}
