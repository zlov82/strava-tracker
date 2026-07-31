package xyz.zlov.app.strava.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider activityTools(ActivityMcpTools activityMcpTools) {
        return MethodToolCallbackProvider.builder().toolObjects(activityMcpTools).build();
    }
}
