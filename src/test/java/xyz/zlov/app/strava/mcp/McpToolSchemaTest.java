package xyz.zlov.app.strava.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the public MCP surface that the trainer's client depends on. */
class McpToolSchemaTest {

    private static final ToolCallback[] TOOLS = MethodToolCallbackProvider.builder()
            .toolObjects(new ActivityMcpTools(null, null, null)).build().getToolCallbacks();

    @Test
    void toolNamesAreNeutralAndOldRideNamesAreGone() {
        Set<String> names = Arrays.stream(TOOLS).map(t -> t.getToolDefinition().name()).collect(Collectors.toSet());

        assertEquals(Set.of("listRecentActivities", "getActivity", "getActivities", "getActivityStreams", "getTimeInZones"), names);
    }

    @Test
    void listRecentActivitiesExposesFilters() throws Exception {
        ToolCallback list = Arrays.stream(TOOLS)
                .filter(t -> t.getToolDefinition().name().equals("listRecentActivities")).findFirst().orElseThrow();
        JsonNode props = new ObjectMapper().readTree(list.getToolDefinition().inputSchema()).get("properties");

        assertTrue(props.has("limit") && props.has("from") && props.has("to") && props.has("type"), props.toString());
    }
}
