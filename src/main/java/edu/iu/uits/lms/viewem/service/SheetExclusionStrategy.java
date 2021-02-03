package edu.iu.uits.lms.viewem.service;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Created by chmaurer on 8/4/15.
 */
public class SheetExclusionStrategy implements ExclusionStrategy {
    public SheetExclusionStrategy() {
    }

    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }

    public boolean shouldSkipField(FieldAttributes f) {
        boolean skip = false;
        RestResource annotation = f.getAnnotation(RestResource.class);
        if (annotation != null) {
            boolean exported = annotation.exported();
            skip = !exported;
        }
        return skip;
    }
}
