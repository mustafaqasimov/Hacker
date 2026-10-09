package az.hacktrain.auth;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
@Mapper(componentModel="spring", unmappedTargetPolicy=ReportingPolicy.ERROR)
interface UserMapper { AuthDtos.Profile profile(UserAccount account); }
