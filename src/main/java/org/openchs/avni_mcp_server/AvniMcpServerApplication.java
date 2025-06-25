package org.openchs.avni_mcp_server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AvniMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AvniMcpServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider avniTools(AvniService avniService) {
		return MethodToolCallbackProvider.builder().toolObjects(avniService).build();
	}

}
