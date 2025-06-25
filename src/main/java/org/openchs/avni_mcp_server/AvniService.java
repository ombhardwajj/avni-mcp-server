package org.openchs.avni_mcp_server;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AvniService {

	private static final String BASE_URL = "https://app.avniproject.org";
	private static final String API_KEY = ""; // Replace with actual API key

	private final RestClient restClient;

	public AvniService() {
		this.restClient = RestClient.builder()
				.baseUrl(BASE_URL)
				.defaultHeader("auth-token", API_KEY)
				.defaultHeader("Accept", "application/json")
				.defaultHeader("Content-Type", "application/json")
				.build();
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

	@Tool(description = "Create a new organization with default settings")
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

	@Tool(description = "Create an admin user for an organization")
	public String createAdminUser(
			@ToolParam(description = "Organization name") String orgName,
			@ToolParam(description = "First name of the user") String firstName,
			@ToolParam(description = "Last name of the user") String lastName,
			@ToolParam(description = "Email address") String email,
			@ToolParam(description = "Phone number") String phoneNumber) {
		try {
			String usernamePrefix = firstName.length() >= 4 ? firstName.substring(0, 4).toLowerCase() : firstName.toLowerCase();
			String username = usernamePrefix + "@" + orgName.toLowerCase().replaceAll("\\s+", "_");
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
					"name", firstName + " " + lastName,
					"email", email,
					"phoneNumber", phoneNumber,
					"groupIds", Collections.emptyList(), // Assuming default admin group ID
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

	@Tool(description = "Create a location type")
	public String createLocationType(
			@ToolParam(description = "Name of the location type") String name,
			@ToolParam(description = "Level of the location type") double level,
			@ToolParam(description = "Parent location type ID, if any") Long parentId) {
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

	@Tool(description = "Create a location")
	public String createLocation(
			@ToolParam(description = "Name of the location") String name,
			@ToolParam(description = "Level of the location") int level,
			@ToolParam(description = "Type of the location") String type,
			@ToolParam(description = "Parent location ID") long parentId) {
		try {
			List<Map<String, Object>> payload = Collections.singletonList(Map.of(
					"name", name,
					"level", level,
					"type", type,
					"parents", Collections.singletonList(Map.of("id", parentId))
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

	@Tool(description = "Create a catchment")
	public String createCatchment(
			@ToolParam(description = "Name of the catchment") String name,
			@ToolParam(description = "List of location IDs") List<Long> locationIds) {
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

	@Tool(description = "Create a user")
	public String createUser(
			@ToolParam(description = "Organization name") String orgName,
			@ToolParam(description = "Username") String username,
			@ToolParam(description = "Full name") String name,
			@ToolParam(description = "Email address") String email,
			@ToolParam(description = "Phone number") String phoneNumber,
			@ToolParam(description = "Catchment ID") long catchmentId,
			@ToolParam(description = "List of group IDs") List<Long> groupIds,
			@ToolParam(description = "Track location") boolean trackLocation,
			@ToolParam(description = "Allow token generation API") boolean allowTokenApi,
			@ToolParam(description = "Enable beneficiary mode") boolean beneficiaryMode,
			@ToolParam(description = "Disable auto refresh") boolean disableAutoRefresh,
			@ToolParam(description = "Disable auto sync") boolean disableAutoSync,
			@ToolParam(description = "Enable call masking") boolean enableCallMasking,
			@ToolParam(description = "Enable register and enrol") boolean registerEnrol) {
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
			Map<String, Object> payload = Map.of(
					"operatingIndividualScope", "ByCatchment",
					"username", username + "@" + orgName.toLowerCase().replaceAll("\\s+", "_"),
					"ignored", username,
					"name", name,
					"email", email,
					"phoneNumber", phoneNumber,
					"catchmentId", catchmentId,
					"groupIds", groupIds,
					"settings", settings
			);

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

	@Tool(description = "Create a user group")
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
}
