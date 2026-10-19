package org.jetbrains.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** Minimal stand-in for the JetBrains annotation so the generator compiles without external dependencies. */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
public @interface Nullable { }
