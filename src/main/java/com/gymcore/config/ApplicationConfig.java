package com.gymcore.config;

import java.time.Clock;
import java.time.ZoneId;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock businessClock(@Value("${gymcore.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("GymCore — API local")
                .version("v1").description("API de desenvolvimento sem autenticação. Academia no caminho é contexto de consulta, não autorização. Pagamentos e acessos são registros manuais; nenhum dinheiro ou dispositivo é processado."));
    }

    @Bean
    org.springdoc.core.customizers.OpenApiCustomizer errorResponses() {
        return api -> api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
            for (var code : java.util.List.of("400", "404", "409", "422", "500", "503")) {
                operation.getResponses().putIfAbsent(code, new io.swagger.v3.oas.models.responses.ApiResponse()
                    .description(switch (code) {
                        case "400" -> "Entrada inválida; code e fieldErrors quando aplicável";
                        case "404" -> "Recurso não encontrado no contexto da academia";
                        case "409" -> "Duplicidade, concorrência ou conflito de estado";
                        case "422" -> "Regra comercial não atendida";
                        case "503" -> "Persistência temporariamente indisponível";
                        default -> "Falha interna sem exposição de detalhes";
                    }).content(new io.swagger.v3.oas.models.media.Content().addMediaType("application/problem+json",
                        new io.swagger.v3.oas.models.media.MediaType().schema(new io.swagger.v3.oas.models.media.ObjectSchema()
                            .addProperty("status", new io.swagger.v3.oas.models.media.IntegerSchema())
                            .addProperty("code", new io.swagger.v3.oas.models.media.StringSchema())
                            .addProperty("detail", new io.swagger.v3.oas.models.media.StringSchema())
                            .addProperty("instance", new io.swagger.v3.oas.models.media.StringSchema())
                            .addProperty("fieldErrors", new io.swagger.v3.oas.models.media.ObjectSchema())))));
            }
        }));
    }
}
