package org.watson.demos.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.watson.demos.models.HealthStatus;

import java.beans.PropertyEditorSupport;

@ConditionalOnProperty("server.accept.case.insensitive.enums")
@Configuration(proxyBeanMethods = false)
public class EnumBinderConfiguration {

    @InitBinder
    public void initBinder(final DataBinder binder) {
        StringToUpperEnumPropertyEditor.register(binder, LivenessState.class);
        StringToUpperEnumPropertyEditor.register(binder, ReadinessState.class);
        StringToUpperEnumPropertyEditor.register(binder, HealthStatus.class);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class StringToUpperEnumPropertyEditor<T extends Enum<T>> extends PropertyEditorSupport {
        static <T extends Enum<T>> void register(final DataBinder binder, final Class<T> clazz) {
            binder.registerCustomEditor(clazz, new StringToUpperEnumPropertyEditor<>(clazz));
        }

        @NonNull
        private final Class<T> clazz;

        @Override
        public void setAsText(@Nullable final String text) throws IllegalArgumentException {
            setValue(text != null ? Enum.valueOf(clazz, text.toUpperCase()) : null);
        }
    }
}
