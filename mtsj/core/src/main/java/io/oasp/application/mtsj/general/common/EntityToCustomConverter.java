package io.oasp.application.mtsj.general.common;

import net.sf.mmm.util.transferobject.api.EntityTo;
import org.dozer.CustomConverter;
import org.dozer.MappingException;

public class EntityToCustomConverter implements CustomConverter {
    @Override
    public Object convert(Object destination, Object source, Class<?> destClass, Class<?> sourceClass) {
        if (source == null) {
            return null;
        }

        if (source instanceof EntityTo) {
            return ((Long) ((EntityTo) source).getId());
        } else {
            throw new MappingException("Converter EntityToCustomConverter "
                + "used incorrectly. Arguments passed in were:"
                + destination + " and " + source);
        }
    }
}
