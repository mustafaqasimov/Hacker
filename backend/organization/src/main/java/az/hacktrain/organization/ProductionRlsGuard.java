package az.hacktrain.organization;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
@Profile("prod")
class ProductionRlsGuard implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    ProductionRlsGuard(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void run(ApplicationArguments args) {
        Boolean unsafe=jdbc.queryForObject("select exists(select 1 from pg_roles where rolname=current_user and (rolsuper or rolbypassrls)) or exists(select 1 from pg_class c join pg_roles r on r.oid=c.relowner where r.rolname=current_user and c.relname in ('organization','org_membership','study_group','group_student','teacher_assignment','org_invitation','organization_event','organization_creator_quota','course','course_topic','course_task','learning_objective','course_group','course_event'))",Boolean.class);
        if(Boolean.TRUE.equals(unsafe)) throw new IllegalStateException("Production runtime database role must not own tenant tables or bypass RLS");
    }
}
