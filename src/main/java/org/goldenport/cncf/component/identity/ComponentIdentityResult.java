package org.goldenport.cncf.component.identity;

import java.util.Objects;
import java.util.Optional;

/*
 * @since   Aug.  7, 2026
 * @version Aug.  7, 2026
 * @author  ASAMI, Tomoharu
 */
public final class ComponentIdentityResult<T> {
    private final T _value;
    private final Error _error;

    private ComponentIdentityResult(T value, Error error) {
        if ((value == null) == (error == null)) {
            throw new IllegalArgumentException("result must contain exactly one value or error");
        }
        _value = value;
        _error = error;
    }

    public static <T> ComponentIdentityResult<T> success(T value) {
        return new ComponentIdentityResult<>(Objects.requireNonNull(value, "value is required"), null);
    }

    public static <T> ComponentIdentityResult<T> failure(Error error) {
        return new ComponentIdentityResult<>(null, Objects.requireNonNull(error, "error is required"));
    }

    public Optional<T> value() {
        return Optional.ofNullable(_value);
    }

    public Optional<Error> error() {
        return Optional.ofNullable(_error);
    }

    public boolean isSuccess() {
        return _value != null;
    }

    public boolean isFailure() {
        return _error != null;
    }

    public T requireValue() {
        if (_value == null) {
            throw new IllegalStateException("component identity result has no value: "
                    + _error.code() + ": " + _error.message());
        }
        return _value;
    }

    public static final class Error {
        private final String _code;
        private final String _message;

        public Error(String code, String message) {
            _code = _require_non_empty(code, "error code");
            _message = _require_non_empty(message, "error message");
        }

        public String code() {
            return _code;
        }

        public String message() {
            return _message;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Error that
                    && _code.equals(that._code)
                    && _message.equals(that._message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_code, _message);
        }

        @Override
        public String toString() {
            return _code + ": " + _message;
        }

        private static String _require_non_empty(String value, String name) {
            Objects.requireNonNull(value, name + " is required");
            if (value.isEmpty()) {
                throw new IllegalArgumentException(name + " must not be empty");
            }
            return value;
        }
    }
}
