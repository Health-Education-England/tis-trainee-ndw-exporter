package uk.nhs.hee.tis.trainee.ndw.mapper;

import org.mapstruct.Mapper;
import uk.nhs.hee.tis.trainee.ndw.mapper.util.FormUtil;

@Mapper(componentModel = "spring", uses = FormUtil.class)
public interface FormMapper {
}
