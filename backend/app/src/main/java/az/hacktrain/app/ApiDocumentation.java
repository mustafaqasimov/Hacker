package az.hacktrain.app;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import java.util.Set;
@Configuration
class ApiDocumentation {
    @Bean OpenAPI api() {
        return new OpenAPI().info(new Info().title("HackTrain API").version("v1"))
            .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
    @Bean OpenApiCustomizer operationSecurity() {
        var publicPaths=Set.of("register","login","refresh","logout","verify-email","resend-verification","forgot-password","reset-password");
        return api->api.getPaths().forEach((path,item)->{
            String suffix=path.substring(path.lastIndexOf('/')+1);
            if(!path.startsWith("/api/v1/auth/") || !publicPaths.contains(suffix)) item.readOperations().forEach(op->op.addSecurityItem(new SecurityRequirement().addList("bearerAuth")));
        });
    }
}
