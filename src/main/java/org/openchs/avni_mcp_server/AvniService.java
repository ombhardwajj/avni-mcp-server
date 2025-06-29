package org.openchs.avni_mcp_server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AvniService {

	private static final String BASE_URL = "https://staging.avniproject.org";
	private static final String API_KEY = ""; // Replace with actual API key

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public AvniService() {
		this.restClient = RestClient.builder()
				.baseUrl(BASE_URL)
				.defaultHeader("auth-token", API_KEY)
				.defaultHeader("Accept", "application/json")
				.defaultHeader("Content-Type", "application/json")
				.build();
		this.objectMapper = new ObjectMapper();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OrganisationResponse(Long id, String name) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record UserResponse(Long id, String username) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AddressLevelTypeResponse(Long id, String name, Double level) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LocationResponse(Long id, String name) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CatchmentResponse(Long id, String name) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record UserGroupResponse(Long id, String name) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SubjectTypeResponse(String uuid, String name) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ProgramResponse(String uuid, String name) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EncounterTypeResponse(String uuid, String name) {}

	@Tool(name = "get_location_types", description = "Retrieve a list of location types for an organization to find IDs for creating locations or sub-location types")
	public String getLocationTypes() {
		try {
			List<?> rawResponse = restClient.get()
					.uri("/addressLevelType")
					.retrieve()
					.body(List.class);
			if (rawResponse == null || rawResponse.isEmpty()) {
				return "No location types found.";
			}
			List<AddressLevelTypeResponse> response = rawResponse.stream()
					.map(item -> objectMapper.convertValue(item, AddressLevelTypeResponse.class))
					.toList();
			return response.stream()
					.map(r -> String.format("ID: %d, Name: %s, Level: %.1f", r.id(), r.name(), r.level()))
					.collect(Collectors.joining("\n"));
		} catch (RestClientException e) {
			return "Failed to retrieve location types: " + e.getMessage();
		}
	}

	@Tool(name = "get_catchments", description = "Retrieve a list of catchments for an organization to find IDs for assigning users")
	public String getCatchments() {
		try {
			List<?> rawResponse = restClient.get()
					.uri("/catchment")
					.retrieve()
					.body(List.class);
			if (rawResponse == null || rawResponse.isEmpty()) {
				return "No catchments found.";
			}
			List<CatchmentResponse> response = rawResponse.stream()
					.map(item -> objectMapper.convertValue(item, CatchmentResponse.class))
					.toList();
			return response.stream()
					.map(r -> String.format("ID: %d, Name: %s", r.id(), r.name()))
					.collect(Collectors.joining("\n"));
		} catch (RestClientException e) {
			return "Failed to retrieve catchments: " + e.getMessage();
		}
	}

	@Tool(name = "get_groups", description = "Retrieve a list of user groups for an organization to find IDs for assigning users")
	public String getGroups() {
		try {
			List<?> rawResponse = restClient.get()
					.uri("/web/groups")
					.retrieve()
					.body(List.class);
			if (rawResponse == null || rawResponse.isEmpty()) {
				return "No user groups found.";
			}
			List<UserGroupResponse> response = rawResponse.stream()
					.map(item -> objectMapper.convertValue(item, UserGroupResponse.class))
					.toList();
			return response.stream()
					.map(r -> String.format("ID: %d, Name: %s", r.id(), r.name()))
					.collect(Collectors.joining("\n"));
		} catch (RestClientException e) {
			return "Failed to retrieve user groups: " + e.getMessage();
		}
	}

	@Tool(name = "create_organization", description = "Create a new organization in Avni with default settings, enabling data entry app setup")
	public String createOrganization(
			@ToolParam(description = "Name of the organization") String name) {
		try {
			String orgName = name.toLowerCase().replaceAll("\\s+", "_");
			Map<String, Object> payload = Map.of(
					"name", name,
					"dbUser", orgName,
					"schemaName", orgName,
					"mediaDirectory", orgName,
					"usernameSuffix", orgName,
					"categoryId", 1,
					"statusId", 1
			);

			OrganisationResponse response = restClient.post()
					.uri("/organisation")
					.body(payload)
					.retrieve()
					.body(OrganisationResponse.class);

			return String.format("Organization '%s' created successfully with ID %d", name, response.id());
		} catch (RestClientException e) {
			return "Failed to create organization: " + e.getMessage();
		}
	}

	@Tool(name = "create_a_user", description = "Create a user for an Avni organization to manage app configuration and data entry, requires group ID if you also need to assign a group to the user")
	public String createAdminUser(
			@ToolParam(description = "Organization name") String orgName,
			@ToolParam(description = "First name of the user") String firstName,
			@ToolParam(description = "Last name of the user", required = false) String lastName,
			@ToolParam(description = "Email address") String email,
			@ToolParam(description = "Phone number") String phoneNumber,
			@ToolParam(description = "List of group IDs (use get_groups to find IDs)", required = false) List<Long> groupIds) {
		try {
			String usernamePrefix = firstName.length() >= 4 ? firstName.substring(0, 4).toLowerCase() : firstName.toLowerCase();
			String username = usernamePrefix + "@" + orgName.toLowerCase().replaceAll("\\s+", "_");
			String fullName = lastName != null ? firstName + " " + lastName : firstName;
			Map<String, Object> settings = Map.of(
					"locale", "en",
					"isAllowedToInvokeTokenGenerationAPI", false,
					"datePickerMode", "calendar",
					"timePickerMode", "clock"
			);
			Map<String, Object> payload = Map.of(
					"operatingIndividualScope", "None",
					"username", username,
					"ignored", usernamePrefix,
					"name", fullName,
					"email", email,
					"phoneNumber", phoneNumber,
					"groupIds", groupIds != null ? groupIds : Collections.emptyList(),
					"settings", settings
			);

			UserResponse response = restClient.post()
					.uri("/user")
					.body(payload)
					.retrieve()
					.body(UserResponse.class);

			return String.format("Admin user '%s' created successfully with ID %d", username, response.id());
		} catch (RestClientException e) {
			return "Failed to create admin user: " + e.getMessage();
		}
	}

	@Tool(name = "create_location_type", description = "Create a location type (e.g., State, District) for hierarchical location setup in Avni")
	public String createLocationType(
			@ToolParam(description = "Name of the location type") String name,
			@ToolParam(description = "Level of the location type (e.g., 3 for State, 2 for District)") double level,
			@ToolParam(description = "Parent location type ID, if any (use get_location_types to find IDs)", required = false) Long parentId) {
		try {
			Map<String, Object> payload = parentId != null ?
					Map.of("name", name, "level", level, "parentId", parentId) :
					Map.of("name", name, "level", level);

			AddressLevelTypeResponse response = restClient.post()
					.uri("/addressLevelType")
					.body(payload)
					.retrieve()
					.body(AddressLevelTypeResponse.class);

			return String.format("Location type '%s' created successfully with ID %d", name, response.id());
		} catch (RestClientException e) {
			return "Failed to create location type: " + e.getMessage();
		}
	}

	@Tool(name = "create_location", description = "Create a real location (e.g., Himachal Pradesh, Kullu) in Avni's location hierarchy")
	public String createLocation(
			@ToolParam(description = "Name of the location") String name,
			@ToolParam(description = "Level of the location (e.g., 1 for Village)") int level,
			@ToolParam(description = "Type of the location (use get_location_types to find type names)") String type,
			@ToolParam(description = "Parent location ID (use get_locations to find IDs)", required = false) Long parentId) {
		try {
			Map<String, Object> parentMap = parentId != null ? Map.of("id", (Object) parentId) : Collections.emptyMap();
			List<Map<String, Object>> payload = Collections.singletonList(Map.of(
					"name", name,
					"level", level,
					"type", type,
					"parents", parentId != null ? Collections.singletonList(parentMap) : Collections.emptyList()
			));

			LocationResponse response = restClient.post()
					.uri("/locations")
					.body(payload)
					.retrieve()
					.body(LocationResponse.class);

			return String.format("Location '%s' created successfully with ID %d", name, response.id());
		} catch (RestClientException e) {
			return "Failed to create location: " + e.getMessage();
		}
	}

	@Tool(name = "create_catchment", description = "Create a catchment grouping locations for data collection in Avni")
	public String createCatchment(
			@ToolParam(description = "Name of the catchment") String name,
			@ToolParam(description = "List of location IDs (use get_locations to find IDs)") List<Long> locationIds) {
		try {
			Map<String, Object> payload = Map.of(
					"deleteFastSync", false,
					"name", name,
					"locationIds", locationIds
			);

			CatchmentResponse response = restClient.post()
					.uri("/catchment")
					.body(payload)
					.retrieve()
					.body(CatchmentResponse.class);

			return String.format("Catchment '%s' created successfully with ID %d", name, response.id());
		} catch (RestClientException e) {
			return "Failed to create catchment: " + e.getMessage();
		}
	}

	@Tool(name = "create_user", description = "Create a user in Avni with customizable settings for data entry or app access")
	public String createUser(
			@ToolParam(description = "Organization name") String orgName,
			@ToolParam(description = "Username (without org suffix)") String username,
			@ToolParam(description = "Full name of the user") String name,
			@ToolParam(description = "Email address") String email,
			@ToolParam(description = "Phone number") String phoneNumber,
			@ToolParam(description = "Catchment ID (use get_catchments to find IDs)", required = false) Long catchmentId,
			@ToolParam(description = "List of group IDs (use get_groups to find IDs)", required = false) List<Long> groupIds,
			@ToolParam(description = "Enable location tracking in Field App", required = false) boolean trackLocation,
			@ToolParam(description = "Allow token generation API access", required = false) boolean allowTokenApi,
			@ToolParam(description = "Enable beneficiary mode in Field App", required = false) boolean beneficiaryMode,
			@ToolParam(description = "Disable dashboard auto-refresh", required = false) boolean disableAutoRefresh,
			@ToolParam(description = "Disable auto-sync in Field App", required = false) boolean disableAutoSync,
			@ToolParam(description = "Enable call masking via Exotel", required = false) boolean enableCallMasking,
			@ToolParam(description = "Enable register and enrol flow", required = false) boolean registerEnrol) {
		try {
			Map<String, Object> settings = Map.of(
					"locale", "en",
					"trackLocation", trackLocation,
					"isAllowedToInvokeTokenGenerationAPI", allowTokenApi,
					"showBeneficiaryMode", beneficiaryMode,
					"disableAutoRefresh", disableAutoRefresh,
					"disableAutoSync", disableAutoSync,
					"enableCallMasking", enableCallMasking,
					"registerEnrol", registerEnrol,
					"datePickerMode", "calendar",
					"timePickerMode", "clock"
			);
			Map<String, Object> payload = new HashMap<>();
			payload.put("operatingIndividualScope", catchmentId != null ? "ByCatchment" : "None");
			payload.put("username", username + "@" + orgName.toLowerCase().replaceAll("\\s+", "_"));
			payload.put("ignored", username);
			payload.put("name", name);
			payload.put("email", email);
			payload.put("phoneNumber", phoneNumber);
			payload.put("catchmentId", catchmentId);
			payload.put("groupIds", groupIds != null ? groupIds : Collections.emptyList());
			payload.put("settings", settings);

			UserResponse response = restClient.post()
					.uri("/user")
					.body(payload)
					.retrieve()
					.body(UserResponse.class);

			return String.format("User '%s' created successfully with ID %d", username, response.id());
		} catch (RestClientException e) {
			return "Failed to create user: " + e.getMessage();
		}
	}

	@Tool(name = "create_user_group", description = "Create a user group in Avni for assigning roles to users")
	public String createUserGroup(
			@ToolParam(description = "Name of the user group") String name) {
		try {
			Map<String, Object> payload = Map.of("name", name);

			UserGroupResponse response = restClient.post()
					.uri("/web/groups")
					.body(payload)
					.retrieve()
					.body(UserGroupResponse.class);

			return String.format("User group '%s' created successfully with ID %d", name, response.id());
		} catch (RestClientException e) {
			return "Failed to create user group: " + e.getMessage();
		}
	}

	@Tool(name = "create_subject_type", description = "Create a subject type (e.g., Person, Household) for data collection in Avni")
	public String createSubjectType(
			@ToolParam(description = "Name of the subject type") String name,
			@ToolParam(description = "Type of the subject : Person, Individual, Group, Household or User ") String type,
			@ToolParam(description = "Location type UUID (use get_location_types to find UUIDs)",required = false) String locationTypeUuid) {
		try {
			Map<String, Object> settings = Map.of(
					"displayRegistrationDetails", true,
					"displayPlannedEncounters", true
			);
			Map<String, Object> payload = Map.of(
					"name", name,
					"groupRoles", Collections.emptyList(),
					"subjectSummaryRule", "",
					"programEligibilityCheckRule", "",
					"shouldSyncByLocation", true,
					"lastNameOptional", false,
					"settings", settings,
					"type", type,
					"active", true,
					"locationTypeUUIDs", Collections.singletonList(locationTypeUuid)
			);

			SubjectTypeResponse response = restClient.post()
					.uri("/web/subjectType")
					.body(payload)
					.retrieve()
					.body(SubjectTypeResponse.class);

			return String.format("Subject type '%s' created successfully with UUID %s", name, response.uuid());
		} catch (RestClientException e) {
			return "Failed to create subject type: " + e.getMessage();
		}
	}

	@Tool(name = "create_program", description = "Create a program in Avni for managing data collection activities")
	public String createProgram(
			@ToolParam(description = "Name of the program") String name,
			@ToolParam(description = "Subject type UUID (use create_subject_type to get UUID)") String subjectTypeUuid) {
		try {
			Map<String, Object> payload = new HashMap<>();
			payload.put("name", name);
			payload.put("colour", "#611717");
			payload.put("programSubjectLabel", name.toLowerCase().replaceAll("\\s+", "_"));
			payload.put("enrolmentSummaryRule", "");
			payload.put("subjectTypeUuid", subjectTypeUuid);
			payload.put("enrolmentEligibilityCheckRule", "");
			payload.put("enrolmentEligibilityCheckDeclarativeRule", null);
			payload.put("manualEligibilityCheckRequired", false);
			payload.put("showGrowthChart", false);
			payload.put("allowMultipleEnrolments", true);
			payload.put("manualEnrolmentEligibilityCheckRule", "");

			ProgramResponse response = restClient.post()
					.uri("/web/program")
					.body(payload)
					.retrieve()
					.body(ProgramResponse.class);

			return String.format("Program '%s' created successfully with UUID %s", name, response.uuid());
		} catch (RestClientException e) {
			return "Failed to create program: " + e.getMessage();
		}
	}

	@Tool(name = "create_encounter_type", description = "Create an encounter type for a program and subject type in Avni")
	public String createEncounterType(
			@ToolParam(description = "Name of the encounter type") String name,
			@ToolParam(description = "Subject type UUID (use create_subject_type to get UUID)") String subjectTypeUuid,
			@ToolParam(description = "Program UUID (use create_program to get UUID)") String programUuid) {
		try {
			Map<String, Object> payload = new HashMap<>();
			payload.put("name", name);
			payload.put("encounterEligibilityCheckRule", name);
			payload.put("loaded", true);
			payload.put("subjectTypeUuid", subjectTypeUuid);
			payload.put("programUuid", programUuid);

			EncounterTypeResponse response = restClient.post()
					.uri("/web/encounterType")
					.body(payload)
					.retrieve()
					.body(EncounterTypeResponse.class);

			return String.format("Encounter type '%s' created successfully with UUID %s", name, response.uuid());
		} catch (RestClientException e) {
			return "Failed to create encounter type: " + e.getMessage();
		}
	}
}