package io.stardog.stardao.core.field;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Field {
    public abstract String getName();
    public abstract String getStorageName();
    public abstract boolean isOptional();
    public abstract boolean isCreatable();
    public abstract boolean isUpdatable();

    public abstract Field.Builder toBuilder();
    public static Field.Builder builder() { return new AutoValue_Field.Builder(); }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);
        public abstract Builder storageName(String storageName);
        public abstract Builder optional(boolean optional);
        public abstract Builder creatable(boolean creatable);
        public abstract Builder updatable(boolean updatable);
        public abstract Field build();
    }
}
